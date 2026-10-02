#!/usr/bin/env python3
"""
Flight audio for the Green Lantern mod: wind loop, sonic boom, take-off, barrel roll,
hero landing and the ORIGINAL heroic flight theme.

The theme is written and synthesized from scratch here (no samples, no third-party music).
It is rendered as two perfectly aligned stereo stems that loop seamlessly:

* flight_theme_base.ogg - strings ostinato, string pads, timpani and horns
* flight_theme_peak.ogg - full brass, trumpets, choir, big drums and cymbals

The game plays the base stem while flying fast and fades the peak stem in when the player
breaks the sound barrier, so the music "explodes" at the most epic moment.

Usage:  pip install numpy scipy soundfile
        python tools/generate_flight_audio.py
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, fftconvolve, sosfilt

SR = 44100
ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "greenlantern", "sounds")
rng = np.random.default_rng(2814)


# ----------------------------------------------------------------------------------------- utils

def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def filt(x, kind, freq, order=2):
    sos = butter(order, freq, btype=kind, fs=SR, output="sos")
    return sosfilt(sos, x)


def env_adsr(n, a, d, s, r):
    a_n, d_n, r_n = max(1, int(a * SR)), max(1, int(d * SR)), max(1, int(r * SR))
    s_n = max(n - a_n - d_n - r_n, 0)
    e = np.concatenate([
        np.linspace(0, 1, a_n, endpoint=False) ** 1.5,
        np.linspace(1, s, d_n, endpoint=False),
        np.full(s_n, s),
        np.linspace(s, 0, r_n) ** 1.3,
    ])
    return np.pad(e, (0, max(0, n - len(e))))[:n]


def midi_hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def fade(x, a=0.002, r=0.02):
    x = x.copy()
    an, rn = int(a * SR), int(r * SR)
    x[:an] *= np.linspace(0, 1, an)
    x[-rn:] *= np.linspace(1, 0, rn)
    return x


def normalize(x, peak=0.9):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x


def save(name, x, peak=0.9):
    path = os.path.join(ROOT, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sf.write(path, normalize(x, peak).astype(np.float32), SR, format="OGG", subtype="VORBIS")
    print("wrote", os.path.relpath(path))


# ------------------------------------------------------------------------------------ instruments

def additive(freq, n, weights_fn, vibrato=0.0, vib_rate=5.2, vib_delay=0.25, detune=0.0):
    """Band-limited additive oscillator; weights_fn(k, t) gives the amplitude of harmonic k."""
    t = np.arange(n) / SR
    vib = 1 + vibrato * np.sin(2 * np.pi * vib_rate * t) * np.clip((t - vib_delay) / 0.3, 0, 1)
    phase = 2 * np.pi * np.cumsum(freq * (1 + detune) * vib) / SR
    out = np.zeros(n)
    k = 1
    while freq * k < SR / 2 - 1000 and k <= 40:
        w = weights_fn(k, t)
        if np.max(np.abs(w)) > 1e-4:
            out += w * np.sin(k * phase + rng.uniform(0, 6.28))
        k += 1
    return out


def saw_weights(rolloff):
    return lambda k, t: (1.0 / k) * np.exp(-k / rolloff)


def strings_note(m, dur, vel=1.0, spiccato=False):
    n = int(dur * SR)
    f = midi_hz(m)
    if spiccato:
        env = env_adsr(n, 0.004, 0.09, 0.25, 0.06)
        voices = [additive(f, n, saw_weights(14), detune=d) for d in (-0.003, 0.0, 0.003)]
    else:
        env = env_adsr(n, 0.35, 0.3, 0.85, min(0.5, dur * 0.4))
        voices = [additive(f, n, saw_weights(9), vibrato=0.004, detune=d) for d in (-0.005, 0.0, 0.004)]
    return vel * env * sum(voices) / 3


def brass_note(m, dur, vel=1.0, bright=1.0):
    """Horn / trumpet: the harmonics open up during the attack (classic brass swell)."""
    n = int(dur * SR)
    f = midi_hz(m)
    env = env_adsr(n, 0.06, 0.25, 0.8, min(0.25, dur * 0.3))
    swell = np.clip(env * 1.2, 0, 1)

    def w(k, t):
        open_amount = 2.5 + 9.0 * bright * swell[: len(t)]
        return (1.0 / k ** 0.85) * np.exp(-k / open_amount)

    tone = additive(f, n, w, vibrato=0.006, vib_rate=5.5, vib_delay=0.18)
    return vel * env * tone


def choir_note(m, dur, vel=1.0):
    """'Aah' choir: saw harmonics shaped by vowel formants, several detuned voices."""
    n = int(dur * SR)
    f = midi_hz(m)
    formants = [(730, 90, 1.0), (1090, 110, 0.5), (2440, 160, 0.25)]

    def w(k, t):
        h = k * f
        a = sum(g * np.exp(-((h - fc) / bw) ** 2) for fc, bw, g in formants)
        return (0.15 / k + a) * np.ones_like(t)

    env = env_adsr(n, 0.5, 0.3, 0.9, min(0.6, dur * 0.4))
    voices = [additive(f, n, w, vibrato=0.007, vib_rate=4.8 + i * 0.3, detune=(i - 1.5) * 0.004) for i in range(4)]
    return vel * env * sum(voices) / 4


def timpani(m, dur=1.6, vel=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = midi_hz(m) * (1 + 0.08 * np.exp(-t * 18))
    phase = 2 * np.pi * np.cumsum(f) / SR
    body = np.sin(phase) + 0.4 * np.sin(1.5 * phase) + 0.2 * np.sin(2.0 * phase)
    hit = filt(rng.uniform(-1, 1, n), "lowpass", 900) * np.exp(-t * 35)
    return vel * (body * np.exp(-t * 2.6) + hit * 1.2)


def tom(m, vel=1.0):
    n = int(0.45 * SR)
    t = np.arange(n) / SR
    f = midi_hz(m) * (1 + 0.5 * np.exp(-t * 30))
    phase = 2 * np.pi * np.cumsum(f) / SR
    return vel * (np.sin(phase) * np.exp(-t * 9) + filt(rng.uniform(-1, 1, n), "bandpass", [150, 1500]) * np.exp(-t * 40) * 0.6)


def snare(vel=1.0):
    n = int(0.35 * SR)
    t = np.arange(n) / SR
    tone = np.sin(2 * np.pi * 190 * t) * np.exp(-t * 25)
    rattle = filt(rng.uniform(-1, 1, n), "bandpass", [1500, 8000]) * np.exp(-t * 14)
    return vel * (tone * 0.6 + rattle)


def crash(dur=3.0, vel=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = filt(rng.uniform(-1, 1, n), "highpass", 3500) * np.exp(-t * 1.5)
    x += filt(rng.uniform(-1, 1, n), "bandpass", [5000, 12000]) * np.exp(-t * 4) * 0.5
    return vel * x


def sub_boom(dur=1.5, vel=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = 55 * (1 + 1.5 * np.exp(-t * 12))
    return vel * np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 3)


def swell_cymbal(dur, vel=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = filt(rng.uniform(-1, 1, n), "highpass", 4000)
    return vel * x * (t / dur) ** 2.5


# ------------------------------------------------------------------------------------- the theme

BPM = 112
BEAT = 60.0 / BPM
BAR = 4 * BEAT
BARS = 16
LOOP = BARS * BAR

# Chord per bar (root for the bass, triad for pads/brass/choir).
CHORDS = {
    "Dm": (38, [50, 53, 57]), "Bb": (34, [46, 50, 53]), "F": (41, [53, 57, 60]), "C": (36, [48, 52, 55]),
    "Gm": (43, [55, 58, 62]), "A": (45, [45, 49, 52]),
}
PROGRESSION = ["Dm", "Bb", "F", "C", "Dm", "Bb", "Gm", "A"] * 2

# Heroic motif (note, beats) for 8 bars.
MOTIF = [
    [(62, 2), (69, 1), (74, 1)],
    [(72, 1.5), (70, 0.5), (69, 1), (65, 1)],
    [(69, 2), (72, 1), (77, 1)],
    [(76, 3), (74, 0.5), (72, 0.5)],
    [(74, 2), (69, 1), (74, 1)],
    [(77, 2), (76, 1), (74, 1)],
    [(74, 1.5), (70, 0.5), (67, 1), (74, 1)],
    [(73, 2), (76, 1), (81, 1)],
]


class Track:
    def __init__(self, length):
        self.l = np.zeros(int(length * SR) + SR * 4)
        self.r = np.zeros_like(self.l)

    def add(self, x, start, gain=1.0, pan=0.0):
        i = int(start * SR)
        n = min(len(x), len(self.l) - i)
        lg = gain * np.sqrt(0.5 * (1 - pan))
        rg = gain * np.sqrt(0.5 * (1 + pan))
        self.l[i:i + n] += x[:n] * lg
        self.r[i:i + n] += x[:n] * rg


def render_base(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Strings ostinato: "da-da-DA-da" on root / octave, 8th notes.
        pattern = [0, 0, 12, 0, 0, 12, 0, 7]
        for i, iv in enumerate(pattern):
            vel = 1.0 if i in (0, 3, 6) else 0.65
            note = strings_note(root + 12 + iv, BEAT * 0.5, vel, spiccato=True)
            track.add(note, t0 + i * BEAT * 0.5, 0.33, pan=-0.35)
            track.add(strings_note(root + iv, BEAT * 0.5, vel * 0.8, spiccato=True), t0 + i * BEAT * 0.5, 0.25, pan=0.3)
        # Sustained string pad.
        for j, m in enumerate(triad):
            track.add(strings_note(m + 12, BAR * 1.05), t0, 0.16, pan=(j - 1) * 0.6)
        # Timpani on the downbeat, pickup in the last beat of every second bar.
        track.add(timpani(root + 12 if root < 40 else root), t0, 0.55)
        if b % 2 == 1:
            track.add(timpani(root + 12 if root < 40 else root, 0.8, 0.6), t0 + 3.5 * BEAT, 0.35)
            track.add(timpani(root + 12 if root < 40 else root, 0.8, 0.7), t0 + 3.75 * BEAT, 0.4)
    # Horns state the motif softly in the second half.
    for b in range(8, 16):
        t = offset + b * BAR
        for m, beats in MOTIF[b - 8]:
            track.add(brass_note(m - 12, beats * BEAT * 0.98, 0.9, bright=0.5), t, 0.22, pan=-0.2)
            t += beats * BEAT


def render_peak(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Brass chord stabs: downbeat and the syncopated "and of 2".
        for hit, length in ((0.0, 1.4), (1.5, 0.9), (3.0, 0.9)):
            for j, m in enumerate(triad):
                track.add(brass_note(m, length * BEAT, 1.0, bright=1.0), t0 + hit * BEAT, 0.12, pan=(j - 1) * 0.5)
            track.add(brass_note(root + 12, length * BEAT, 1.0, bright=0.8), t0 + hit * BEAT, 0.14)
        # Choir.
        for j, m in enumerate(triad):
            track.add(choir_note(m + 12, BAR * 1.05), t0, 0.12, pan=(1 - j) * 0.5)
        # Drums: 16th-note toms, snare on 2 and 4, sub boom on the downbeat.
        for s in range(16):
            vel = 1.0 if s % 4 == 0 else (0.75 if s % 2 == 0 else 0.5)
            track.add(tom(45 if s % 4 else 40, vel), t0 + s * BEAT / 4, 0.22, pan=0.25 if s % 2 else -0.25)
        track.add(snare(), t0 + BEAT, 0.3)
        track.add(snare(), t0 + 3 * BEAT, 0.3)
        track.add(sub_boom(), t0, 0.5)
        if b % 8 == 0:
            track.add(crash(), t0, 0.35)
        if b % 8 == 7:
            track.add(swell_cymbal(BAR), t0, 0.25)
    # Trumpets: the motif in full, high octave, over all 16 bars.
    for b in range(16):
        t = offset + b * BAR
        for m, beats in MOTIF[b % 8]:
            up = 12 if b >= 8 else 0
            track.add(brass_note(m + up, beats * BEAT * 0.98, 1.0, bright=1.2), t, 0.2, pan=0.15)
            t += beats * BEAT


def reverb(l, r, seconds=2.4, wet=0.32):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    decay = np.exp(-t * 6.9 / seconds)
    irl = filt(rng.uniform(-1, 1, n), "lowpass", 6000) * decay
    irr = filt(rng.uniform(-1, 1, n), "lowpass", 6000) * decay
    irl[: int(0.012 * SR)] = 0
    irr[: int(0.019 * SR)] = 0
    irl /= np.sqrt(np.sum(irl ** 2))
    irr /= np.sqrt(np.sum(irr ** 2))
    wl = fftconvolve(l, irl)[: len(l)]
    wr = fftconvolve(r, irr)[: len(r)]
    return l + wet * wl * 3, r + wet * wr * 3


def render_stem(fn):
    """Renders two loop cycles and keeps the second one, so reverb tails wrap around the loop."""
    track = Track(LOOP * 2)
    fn(track, 0.0)
    fn(track, LOOP)
    l, r = reverb(track.l, track.r)
    a, b = int(LOOP * SR), int(2 * LOOP * SR)
    # The second cycle already contains the tails of the first one, exactly like a looping playback.
    out_l, out_r = l[a:b], r[a:b]
    return np.stack([out_l, out_r], axis=1)


def master(x):
    x = filt(x.T, "highpass", 28).T
    return np.tanh(x * 1.2) / np.tanh(1.2)


def flight_theme():
    base = master(render_stem(render_base))
    peak = master(render_stem(render_peak))
    # Scale both stems by the same factor so base + peak never clips.
    scale = 0.85 / np.max(np.abs(base + peak))  # headroom for the Vorbis encoder
    for name, stem in (("flight_theme_base", base), ("flight_theme_peak", peak)):
        path = os.path.join(ROOT, "music", name + ".ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sf.write(path, (stem * scale).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        print("wrote", os.path.relpath(path), f"{len(stem) / SR:.2f}s")


# ------------------------------------------------------------------------------------ flight sfx

def flight_wind():
    dur = 3.0
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = rng.uniform(-1, 1, n)
    low = filt(x, "bandpass", [120, 900])
    high = filt(x, "bandpass", [1200, 5000]) * 0.35
    mod = 0.75 + 0.25 * np.sin(2 * np.pi * t / dur * 2) + 0.1 * np.sin(2 * np.pi * t / dur * 5)
    y = (low + high) * mod
    # Seamless loop: crossfade the end into the start.
    xf = int(0.4 * SR)
    ramp = np.linspace(0, 1, xf)
    y[:xf] = y[:xf] * ramp + y[-xf:] * (1 - ramp)
    save("flight_wind", y[:-xf], 0.8)


def sonic_boom():
    dur = 4.0
    n = int(dur * SR)
    t = np.arange(n) / SR
    y = np.zeros(n)
    # N-wave double crack.
    for start, sign in ((0.0, 1), (0.11, -1)):
        i = int(start * SR)
        k = int(0.012 * SR)
        y[i:i + k] += sign * np.linspace(1, -1, k)
    crack = filt(rng.uniform(-1, 1, n), "highpass", 800) * np.exp(-t * 30)
    rumble = filt(rng.uniform(-1, 1, n), "lowpass", 180) * np.exp(-t * 1.3) * 3.0
    thump = np.sin(2 * np.pi * np.cumsum(45 * (1 + 2 * np.exp(-t * 15))) / SR) * np.exp(-t * 2.5)
    y = y * 1.5 + crack * 0.7 + rumble + thump * 1.2
    save("sonic_boom", fade(y, 0.0005, 0.3), 0.95)


def flight_takeoff():
    dur = 1.6
    n = int(dur * SR)
    t = np.arange(n) / SR
    whoosh = filt(rng.uniform(-1, 1, n), "bandpass", [300, 3000]) * np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 0.7
    f = 80 * (1 + 3 * (t / dur))
    hum = (np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.5 * np.sin(4 * np.pi * np.cumsum(f) / SR)) * np.exp(-t * 1.5)
    blast = filt(rng.uniform(-1, 1, n), "lowpass", 400) * np.exp(-t * 6) * 2
    save("flight_takeoff", fade(whoosh + hum * 0.5 + blast, 0.002, 0.2), 0.9)


def flight_roll():
    dur = 0.7
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = rng.uniform(-1, 1, n)
    # Pitch sweep up then down: crossfade a low and a high band of the same noise.
    sweep = np.sin(np.pi * t / dur)
    y = filt(x, "bandpass", [300, 1100]) * (1 - sweep) + filt(x, "bandpass", [1600, 5200]) * sweep * 1.3
    y *= np.sin(np.pi * t / dur) ** 1.5
    save("flight_roll", fade(filt(y, "lowpass", 6000)), 0.85)


def hero_landing():
    dur = 2.5
    n = int(dur * SR)
    t = np.arange(n) / SR
    thump = np.sin(2 * np.pi * np.cumsum(50 * (1 + 3 * np.exp(-t * 20))) / SR) * np.exp(-t * 3)
    crunch = filt(rng.uniform(-1, 1, n), "bandpass", [200, 2500]) * np.exp(-t * 9)
    debris = filt(rng.uniform(-1, 1, n), "highpass", 2000) * np.exp(-t * 4) * (rng.uniform(0, 1, n) > 0.97)
    energy = np.sin(2 * np.pi * 330 * t) * np.exp(-t * 5) * 0.3
    save("hero_landing", fade(thump * 1.6 + crunch + debris * 0.8 + energy, 0.0005, 0.2), 0.95)


def main():
    flight_wind()
    sonic_boom()
    flight_takeoff()
    flight_roll()
    hero_landing()
    flight_theme()


if __name__ == "__main__":
    main()
