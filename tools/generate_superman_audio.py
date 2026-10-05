#!/usr/bin/env python3
"""
Audio of Superman (DC Universe expansion): the suit, heat vision, the super punch, super breath,
X-ray vision, the solar charge, his sound barrier boom and his ORIGINAL flight theme.

Everything is synthesized from scratch (no samples, no third-party music). The theme is written for
this mod (a broad, sunny anthem in B-flat major, built on its own melody) and is rendered as two
aligned stereo stems that loop seamlessly, like the other themes:

* superman_theme_base.ogg - horns and strings ostinato, low brass, timpani and a steady march
* superman_theme_peak.ogg - the trumpet melody, choir, cymbals and snare rolls

The game plays the base stem while Superman flies fast and fades the peak stem in past the sound
barrier.

Usage:  pip install numpy scipy soundfile
        python tools/generate_superman_audio.py
"""
import os

import numpy as np
import soundfile as sf

from generate_flight_audio import (ROOT, SR, Track, brass_note, choir_note, crash, fade, filt, midi_hz, reverb, save,
                                   snare, strings_note, sub_boom, swell_cymbal, timpani, tom)

rng = np.random.default_rng(1938)


def noise(n):
    return rng.uniform(-1, 1, n)


def t_of(n):
    return np.arange(n) / SR


def chirp(f0, f1, dur, curve=1.0):
    n = int(dur * SR)
    t = t_of(n)
    f = f0 + (f1 - f0) * (t / dur) ** curve
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def whoosh(dur, lo=300, hi=4000, rise=True):
    n = int(dur * SR)
    t = t_of(n)
    sweep = (t / dur) if rise else (1 - t / dur)
    a = filt(noise(n), "bandpass", [lo, lo * 3])
    b = filt(noise(n), "bandpass", [hi / 3, min(hi, 20000)])
    return (a * (1 - sweep) + b * sweep * 1.3) * np.sin(np.pi * t / dur) ** 1.2


def shimmer(n, base=880):
    """A bright, glassy chord: the crystal's 'Kryptonian' sound."""
    t = t_of(n)
    x = np.zeros(n)
    for k, ratio in enumerate((1.0, 1.5, 2.0, 2.52, 3.0, 4.0)):
        x += np.sin(2 * np.pi * base * ratio * t + k) / (1 + k * 0.6) * (0.8 + 0.2 * np.sin(2 * np.pi * (3 + k) * t))
    return x


# ------------------------------------------------------------------------------------------ sfx

def superman_suit_up():
    dur = 1.8
    n = int(dur * SR)
    t = t_of(n)
    x = shimmer(n, 660) * np.exp(-((t - 0.45) / 0.35) ** 2) * 0.35
    x += whoosh(dur, 200, 7000, True) * 0.6
    rise = chirp(110, 880, 0.9, 1.6) * np.sin(np.pi * t_of(int(0.9 * SR)) / 0.9) * 0.2
    x[: len(rise)] += rise
    # Cape snap and a deep heroic thump at the end.
    snap = filt(noise(int(0.12 * SR)), "bandpass", [600, 5000]) * np.exp(-t_of(int(0.12 * SR)) * 40)
    i = int(1.0 * SR)
    x[i:i + len(snap)] += snap * 1.2
    hit = sub_boom(0.8, 1.0)
    x[i:i + len(hit)] += hit[: n - i] * 0.9
    save("superman_suit_up", fade(x, 0.002, 0.2), 0.9)


def superman_suit_down():
    dur = 1.1
    n = int(dur * SR)
    t = t_of(n)
    x = whoosh(dur, 200, 5000, False) * 0.8 + shimmer(n, 520) * np.exp(-t * 4) * 0.2
    save("superman_suit_down", fade(x, 0.002, 0.15), 0.8)


def heat_vision():
    """A searing electric hum with a hiss of burning: about 1.6 s, replayed while it lasts."""
    dur = 1.6
    n = int(dur * SR)
    t = t_of(n)
    f = 140 * (1 + 0.04 * np.sin(2 * np.pi * 6 * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    hum = sum(np.sin(k * ph) / k ** 0.8 for k in range(1, 16)) * 0.25
    hum = filt(hum, "lowpass", 3500)
    sizzle = filt(noise(n), "bandpass", [2500, 9000]) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 11 * t))) * 0.5
    whine = np.sin(2 * np.pi * np.cumsum(2200 + 120 * np.sin(2 * np.pi * 0.7 * t)) / SR) * 0.06
    env = np.minimum(1, t / 0.04) * np.minimum(1, (dur - t) / 0.08)
    save("heat_vision", fade((hum + sizzle + whine) * env, 0.002, 0.05), 0.85)


def heat_vision_end():
    dur = 0.8
    n = int(dur * SR)
    t = t_of(n)
    x = chirp(900, 90, dur, 0.5) * np.exp(-t * 4) * 0.4 + filt(noise(n), "bandpass", [2000, 7000]) * np.exp(-t * 6) * 0.5
    save("heat_vision_end", fade(x, 0.001, 0.1), 0.75)


def super_punch():
    """Thunderclap: a sharp crack, a huge sub hit and a long rolling rumble."""
    dur = 2.6
    n = int(dur * SR)
    t = t_of(n)
    crack = filt(noise(n), "highpass", 1800) * np.exp(-t * 30)
    body = np.sin(2 * np.pi * np.cumsum(55 * (1 + 4 * np.exp(-t * 25))) / SR) * np.exp(-t * 3)
    rumble = filt(noise(n), "lowpass", 200) * np.exp(-t * 1.5) * 2.5 * (1 + 0.4 * np.sin(2 * np.pi * 5 * t))
    debris = filt((rng.uniform(0, 1, n) < 0.01) * noise(n), "bandpass", [800, 5000]) * np.exp(-t * 3) * 3
    x = crack * 1.4 + body * 1.6 + rumble + debris + sub_boom(dur, 1.0) * 0.8
    save("super_punch", fade(x, 0.0005, 0.4), 0.98)


def super_breath():
    """A huge, howling gust of icy wind (about 2.1 s, replayed while it lasts)."""
    dur = 2.1
    n = int(dur * SR)
    t = t_of(n)
    base = noise(n)
    gust = np.zeros(n)
    for f0, gain in ((250, 1.0), (700, 0.7), (1800, 0.45), (4200, 0.3)):
        mod = 0.65 + 0.35 * np.sin(2 * np.pi * (0.8 + f0 / 3000) * t + rng.uniform(0, 6))
        gust += filt(base, "bandpass", [f0 * 0.6, f0 * 1.5]) * gain * mod
    howl = np.sin(2 * np.pi * np.cumsum(620 + 90 * np.sin(2 * np.pi * 0.9 * t)) / SR) * 0.05
    frost = filt((rng.uniform(0, 1, n) < 0.02) * noise(n), "bandpass", [5000, 12000]) * 1.5
    env = np.minimum(1, t / 0.12) * np.minimum(1, (dur - t) / 0.25)
    save("super_breath", fade((gust + howl + frost * np.exp(-t * 0.8)) * env, 0.005, 0.1), 0.85)


def xray_on():
    dur = 0.9
    n = int(dur * SR)
    t = t_of(n)
    sweep = chirp(300, 2400, dur, 0.7) * np.sin(np.pi * t / dur) * 0.25
    x = sweep + shimmer(n, 990) * np.exp(-((t - 0.5) / 0.25) ** 2) * 0.2
    x += np.sin(2 * np.pi * 60 * t) * np.sin(np.pi * t / dur) * 0.15
    save("xray_on", fade(x, 0.003, 0.1), 0.7)


def xray_off():
    dur = 0.6
    n = int(dur * SR)
    t = t_of(n)
    x = chirp(2000, 250, dur, 0.6) * (1 - t / dur) * 0.25 + shimmer(n, 700) * np.exp(-t * 8) * 0.12
    save("xray_off", fade(x, 0.002, 0.08), 0.65)


def solar_charged():
    """Warm rising major chord: the crystal is full of sunlight."""
    dur = 1.8
    n = int(dur * SR)
    t = t_of(n)
    x = np.zeros(n)
    for i, m in enumerate((70, 74, 77, 82)):
        start = int(i * 0.12 * SR)
        k = n - start
        tt = t_of(k)
        x[start:] += np.sin(2 * np.pi * midi_hz(m) * tt) * np.exp(-tt * 2.2) * np.minimum(1, tt / 0.01) * 0.3
        x[start:] += np.sin(2 * np.pi * midi_hz(m) * 2 * tt) * np.exp(-tt * 3.5) * 0.08
    x += shimmer(n, 1400) * np.exp(-t * 2.5) * 0.06
    save("solar_charged", fade(x, 0.002, 0.3), 0.7)


def super_boom():
    """Superman's sound barrier: a cannon-like double boom with a wind tail."""
    dur = 3.2
    n = int(dur * SR)
    t = t_of(n)
    crack = filt(noise(n), "highpass", 1200) * np.exp(-t * 16)
    boom = sub_boom(dur, 1.0)
    second = np.zeros(n)
    i = int(0.11 * SR)
    second[i:] = boom[: n - i] * 0.7
    wind = whoosh(dur, 150, 3000, False) * 0.7
    x = crack * 1.2 + boom * 1.5 + second + wind + filt(noise(n), "lowpass", 180) * np.exp(-t * 1.4) * 2
    save("super_boom", fade(x, 0.0005, 0.4), 0.97)


# ------------------------------------------------------------------------------------- the theme
# An anthem of our own: B-flat major, 112 BPM, a hopeful melody that climbs by steps and leaps up a
# sixth at its peak, over a progression that keeps lifting (I - IV - vi - V, then bVII - IV - I).

BPM = 112
BEAT = 60.0 / BPM
BAR = 4 * BEAT
BARS = 16
LOOP = BARS * BAR

CHORDS = {
    "Bb": (34, [58, 62, 65]), "Eb": (39, [55, 58, 63]), "Gm": (31, [55, 58, 62]), "F": (29, [53, 57, 60]),
    "Ab": (32, [56, 60, 63]), "Cm": (36, [55, 60, 63]), "Dm": (38, [53, 57, 62]),
}
PROGRESSION = ["Bb", "Eb", "Gm", "F", "Bb", "Eb", "Cm", "F", "Gm", "Dm", "Eb", "Bb", "Ab", "Eb", "F", "F"]

# Trumpet melody (note, beats), one entry per bar.
MELODY = [
    [(70, 1.5), (72, 0.5), (74, 1), (77, 1)],
    [(75, 2), (74, 1), (72, 1)],
    [(70, 1), (74, 1), (79, 1.5), (77, 0.5)],
    [(77, 3), (72, 1)],
    [(70, 1.5), (72, 0.5), (74, 1), (77, 1)],
    [(79, 2), (82, 1), (79, 1)],
    [(79, 1), (77, 1), (75, 1), (72, 1)],
    [(77, 4)],
    [(74, 1.5), (75, 0.5), (77, 1), (79, 1)],
    [(77, 2), (74, 2)],
    [(75, 1), (77, 1), (79, 1), (82, 1)],
    [(82, 2.5), (77, 1.5)],
    [(80, 1.5), (79, 0.5), (77, 1), (75, 1)],
    [(79, 2), (75, 2)],
    [(77, 1), (79, 1), (81, 1), (84, 1)],
    [(82, 3), (77, 1)],
]


def render_base(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Horns and strings ostinato in 8ths: chord tones rocking up and down.
        pattern = [0, 1, 2, 1, 0, 1, 2, 1]
        for i, k in enumerate(pattern):
            vel = 1.0 if i % 2 == 0 else 0.7
            track.add(strings_note(triad[k], BEAT * 0.5, vel, spiccato=True), t0 + i * BEAT * 0.5, 0.26,
                      pan=-0.35 if i % 2 else 0.35)
        # Low brass on beats 1 and 3, sustained strings pad.
        for hit in (0, 2):
            track.add(brass_note(root + 12, BEAT * 1.8, 0.9, bright=0.8), t0 + hit * BEAT, 0.18)
        for j, m in enumerate(triad):
            track.add(strings_note(m - 12, BAR * 1.05), t0, 0.1, pan=(j - 1) * 0.6)
        # March: timpani on 1, toms on 3, a light snare on 2 and 4.
        track.add(timpani(root + 12 if root < 36 else root, 0.9, 0.9), t0, 0.32)
        track.add(tom(43, 0.7), t0 + 2 * BEAT, 0.2)
        track.add(tom(43, 0.5), t0 + 2.5 * BEAT, 0.14)
        track.add(snare(0.5), t0 + BEAT, 0.14)
        track.add(snare(0.5), t0 + 3 * BEAT, 0.14)
        if b % 4 == 3:
            for s in range(4):
                track.add(snare(0.3 + 0.15 * s), t0 + (3 + s * 0.25) * BEAT, 0.12)


def render_peak(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        for j, m in enumerate(triad):
            track.add(choir_note(m + 12, BAR * 1.05), t0, 0.08, pan=(1 - j) * 0.5)
            track.add(brass_note(m, BEAT * 3.6, 0.8, bright=0.9), t0, 0.06, pan=(j - 1) * 0.5)
        track.add(sub_boom(0.9, 0.7), t0, 0.3)
        track.add(snare(0.9), t0 + BEAT, 0.25)
        track.add(snare(0.9), t0 + 3 * BEAT, 0.25)
        if b % 4 == 0:
            track.add(crash(2.4), t0, 0.28)
        if b % 8 == 7:
            track.add(swell_cymbal(BAR), t0, 0.2)
        # Melody, doubled an octave below by horns.
        t = t0
        for m, beats in MELODY[b]:
            track.add(brass_note(m - 12, beats * BEAT * 0.96, 1.0, bright=1.3), t, 0.22, pan=0.1)
            track.add(brass_note(m - 24, beats * BEAT * 0.96, 0.8, bright=0.9), t, 0.1, pan=-0.15)
            t += beats * BEAT


def render_stem(fn):
    track = Track(LOOP * 2)
    fn(track, 0.0)
    fn(track, LOOP)
    l, r = reverb(track.l, track.r, seconds=2.2, wet=0.28)
    a, b = int(LOOP * SR), int(2 * LOOP * SR)
    return np.stack([l[a:b], r[a:b]], axis=1)


def master(x):
    x = filt(x.T, "highpass", 30).T
    return np.tanh(x * 1.3) / np.tanh(1.3)


def superman_theme():
    base = master(render_stem(render_base))
    peak = master(render_stem(render_peak))
    scale = 0.85 / np.max(np.abs(base + peak))
    for name, stem in (("superman_theme_base", base), ("superman_theme_peak", peak)):
        path = os.path.join(ROOT, "music", name + ".ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sf.write(path, (stem * scale).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        print("wrote", os.path.relpath(path), f"{len(stem) / SR:.2f}s")


def main():
    superman_suit_up()
    superman_suit_down()
    heat_vision()
    heat_vision_end()
    super_punch()
    super_breath()
    xray_on()
    xray_off()
    solar_charged()
    super_boom()
    superman_theme()


if __name__ == "__main__":
    main()
