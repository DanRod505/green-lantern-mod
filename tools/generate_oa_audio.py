#!/usr/bin/env python3
"""
Original sounds of the Oa update, synthesized from scratch (no samples):
the portal effects and "Oa", the ambient music of the planet.

Usage:  pip install numpy soundfile
        python tools/generate_oa_audio.py
"""
import os
import subprocess

import numpy as np
import soundfile as sf

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "greenlantern", "sounds")
rng = np.random.default_rng(2814)


def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def fft_filter(x, lo=None, hi=None):
    """Brick-wall-ish band filter with soft edges, done in the frequency domain (fast for long signals)."""
    n = len(x)
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(n, 1 / SR)
    gain = np.ones_like(f)
    if hi is not None:
        gain *= 1 / (1 + (f / hi) ** 4)
    if lo is not None:
        gain *= 1 / (1 + (lo / np.maximum(f, 1e-3)) ** 4)
    return np.fft.irfft(spec * gain, n)


def env(n, a, r):
    e = np.ones(n)
    na, nr = int(a * SR), int(r * SR)
    na, nr = min(na, n), min(nr, n)
    e[:na] = np.linspace(0, 1, na)
    if nr:
        e[-nr:] *= np.linspace(1, 0, nr)
    return e


def normalize(x, peak=0.9):
    m = np.max(np.abs(x))
    return x if m == 0 else x / m * peak


def save(name, x, peak=0.9):
    x = normalize(x, peak)
    path = os.path.join(OUT, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if x.ndim == 1:
        sf.write(path, x.astype(np.float32), SR, format="OGG", subtype="VORBIS")
    else:
        # libsndfile can crash on long Vorbis files: encode the music with ffmpeg instead.
        wav = path[:-4] + ".wav"
        sf.write(wav, x.astype(np.float32), SR, subtype="PCM_16")
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", path], check=True)
        os.remove(wav)
    print("wrote", os.path.relpath(path))


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


# ------------------------------------------------------------------ portal effects

def portal_open():
    dur = 1.8
    t = t_axis(dur)
    n = len(t)
    f = 60 * (12 ** (t / dur))
    phase = 2 * np.pi * np.cumsum(f) / SR
    swirl = np.sin(phase) * np.sin(2 * np.pi * 7 * t) ** 2
    rush = fft_filter(rng.uniform(-1, 1, n), lo=300, hi=2500) * np.linspace(0.2, 1, n) ** 2
    chord = sum(np.sin(2 * np.pi * midi(m) * t + rng.uniform(0, 6)) for m in (62, 69, 74, 78, 81)) / 5
    chord *= np.clip((t - 0.6) / 0.6, 0, 1)
    return (0.6 * swirl + 0.5 * rush + 0.8 * chord) * env(n, 0.05, 0.5)


def portal_hum():
    dur = 2.0
    t = t_axis(dur)
    n = len(t)
    hum = np.sin(2 * np.pi * 55 * t) + 0.5 * np.sin(2 * np.pi * 110.5 * t) + 0.25 * np.sin(2 * np.pi * 165 * t)
    hum *= 0.7 + 0.3 * np.sin(2 * np.pi * 1.5 * t)
    air = fft_filter(rng.uniform(-1, 1, n), lo=200, hi=900) * 0.4
    return (hum + air) * env(n, 0.3, 0.5)


def portal_travel():
    dur = 1.4
    t = t_axis(dur)
    n = len(t)
    whoosh = fft_filter(rng.uniform(-1, 1, n), lo=150, hi=3000) * np.exp(-((t - 0.35) / 0.18) ** 2)
    f = 900 * (0.25 ** (t / dur))
    drop = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 3)
    chime = sum(np.sin(2 * np.pi * midi(m) * t) * np.exp(-t * 2.5) for m in (81, 86, 88)) / 3
    return (1.2 * whoosh + 0.5 * drop + 0.7 * chime) * env(n, 0.01, 0.3)


# ------------------------------------------------------------------ music: "Oa"

BPM = 64
BEAT = 60 / BPM
BAR = 4 * BEAT
# Chords as MIDI notes (D minor with a hopeful lift at the end of each phrase).
PROGRESSION = [
    [50, 57, 62, 65, 69, 76],   # Dm9
    [46, 53, 58, 62, 65, 69],   # Bbmaj7
    [43, 50, 55, 58, 62, 69],   # Gm9
    [45, 52, 57, 61, 64, 71],   # A (sus to major)
    [50, 57, 62, 65, 69, 72],   # Dm(add9)
    [41, 48, 53, 57, 60, 67],   # Fmaj9
    [48, 55, 60, 64, 67, 74],   # Cadd9
    [45, 52, 57, 61, 64, 69],   # A
]
BARS_PER_CHORD = 2


def pad_voice(t, freq, detune=0.003):
    out = np.zeros_like(t)
    for d in (-detune, 0, detune):
        ph = rng.uniform(0, 2 * np.pi)
        f = freq * (1 + d)
        # A few harmonics with falling weight: a soft, string-like tone.
        for k, w in ((1, 1.0), (2, 0.35), (3, 0.18), (4, 0.08)):
            out += w * np.sin(2 * np.pi * f * k * t + ph * k)
    return out / 3


def music():
    chord_dur = BARS_PER_CHORD * BAR
    total = chord_dur * len(PROGRESSION) * 2  # two passes, ~3 minutes
    n = int(total * SR)
    left = np.zeros(n)
    right = np.zeros(n)
    t_all = np.arange(n) / SR

    # Pads: overlapping chords with slow swells.
    for rep in range(2):
        for i, chord in enumerate(PROGRESSION):
            start = (rep * len(PROGRESSION) + i) * chord_dur
            dur = chord_dur + 2.5
            s = int(start * SR)
            m = min(int(dur * SR), n - s)
            t = np.arange(m) / SR
            e = env(m, 2.2, 2.6)
            sig_l = np.zeros(m)
            sig_r = np.zeros(m)
            for j, note in enumerate(chord):
                v = pad_voice(t, midi(note)) * (0.5 if j == 0 else 0.32)
                pan = 0.5 + 0.35 * np.sin(j * 1.7 + rep)
                sig_l += v * (1 - pan)
                sig_r += v * pan
            left[s:s + m] += sig_l * e
            right[s:s + m] += sig_r * e
    left = fft_filter(left, hi=1800)
    right = fft_filter(right, hi=1800)
    # Breathing filter sweep.
    lfo = 0.75 + 0.25 * np.sin(2 * np.pi * t_all / (4 * BAR))
    left *= lfo
    right *= lfo

    # Sub drone on the root.
    for rep in range(2):
        for i, chord in enumerate(PROGRESSION):
            start = (rep * len(PROGRESSION) + i) * chord_dur
            s = int(start * SR)
            m = min(int((chord_dur + 1.5) * SR), n - s)
            t = np.arange(m) / SR
            sub = np.sin(2 * np.pi * midi(chord[0] - 12) * t) * env(m, 1.5, 2.0) * 0.45
            left[s:s + m] += sub
            right[s:s + m] += sub

    # Crystal bells: an arpeggio that enters in the second pass, echoing across the stereo field.
    bells_l = np.zeros(n)
    bells_r = np.zeros(n)
    step = BEAT / 2
    first = len(PROGRESSION) * chord_dur * 0.5
    pattern = [2, 3, 4, 5, 4, 3, 5, 2]
    k = 0
    tt = first
    while tt < total - 4:
        chord = PROGRESSION[int(tt // chord_dur) % len(PROGRESSION)]
        note = chord[pattern[k % len(pattern)]] + 12
        if rng.random() < 0.82:
            s = int(tt * SR)
            m = min(int(2.4 * SR), n - s)
            t = np.arange(m) / SR
            f = midi(note)
            bell = (np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 6)
                    + 0.2 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 9)) * np.exp(-t * 2.2)
            bell *= 0.16 * (0.7 + 0.3 * rng.random())
            pan = rng.uniform(0.2, 0.8)
            bells_l[s:s + m] += bell * (1 - pan)
            bells_r[s:s + m] += bell * pan
        k += 1
        tt += step
    # Ping-pong echo.
    d = int(BEAT * 0.75 * SR)
    for tap in range(1, 5):
        g = 0.45 ** tap
        if tap % 2:
            left[d * tap:] += bells_r[:-d * tap] * g
            right[d * tap:] += bells_l[:-d * tap] * g
        else:
            left[d * tap:] += bells_l[:-d * tap] * g
            right[d * tap:] += bells_r[:-d * tap] * g
    left += bells_l
    right += bells_r

    # Distant choir-like "ahh": vibrato sines on the top chord tones during the second pass.
    for i, chord in enumerate(PROGRESSION):
        start = (len(PROGRESSION) + i) * chord_dur
        s = int(start * SR)
        m = min(int((chord_dur + 2) * SR), n - s)
        t = np.arange(m) / SR
        voice = np.zeros(m)
        for note in chord[-2:]:
            vib = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * t + rng.uniform(0, 6))
            ph = 2 * np.pi * np.cumsum(midi(note) * vib) / SR
            voice += np.sin(ph) + 0.3 * np.sin(2 * ph) + 0.12 * np.sin(3 * ph)
        voice = fft_filter(voice, lo=250, hi=2200) * env(m, 2.5, 2.5) * 0.12
        left[s:s + m] += voice
        right[s:s + m] += voice

    master = env(n, 4.0, 6.0)
    stereo = np.stack([left * master, right * master], axis=1)
    return stereo


def main():
    save("portal_open", portal_open())
    save("portal_hum", portal_hum(), 0.7)
    save("portal_travel", portal_travel())
    save("music/oa", music(), 0.8)


if __name__ == "__main__":
    main()
