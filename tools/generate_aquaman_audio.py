#!/usr/bin/env python3
"""
Audio of Aquaman (DC Universe expansion): the suit, the swim dash and the rush of water, the
Trident of Atlantis (summon, swing, throw, hit, return), the great white shark (surfacing, bite) and
the call of the sea (a conch horn answered by whale song).

Everything is synthesized from scratch: no samples, no third-party audio.

Usage:  pip install numpy scipy soundfile
        python tools/generate_aquaman_audio.py
"""
import numpy as np

from generate_flight_audio import SR, brass_note, fade, filt, midi_hz, save, sub_boom

rng = np.random.default_rng(1941)


def noise(n):
    return rng.uniform(-1, 1, n)


def t_of(n):
    return np.arange(n) / SR


def place(x, y, at, gain=1.0):
    """Mixes y into x starting at `at` seconds."""
    i = int(at * SR)
    if i >= len(x):
        return x
    m = min(len(y), len(x) - i)
    x[i:i + m] += y[:m] * gain
    return x


def bubble(f0=600, dur=0.08, rise=1.6):
    """One bubble: a sine that pops and glides up (the classic Minnaert resonance)."""
    n = int(dur * SR)
    t = t_of(n)
    f = f0 * (1 + (rise - 1) * t / dur)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 6.0 / dur) * (1 - np.exp(-t * 3000))


def bubbles(dur, rate=40, lo=300, hi=1400):
    n = int(dur * SR)
    x = np.zeros(n)
    for _ in range(int(dur * rate)):
        b = bubble(rng.uniform(lo, hi), rng.uniform(0.03, 0.12), rng.uniform(1.2, 2.2))
        place(x, b, rng.uniform(0, dur), rng.uniform(0.2, 0.7))
    return x


def water(n, lo=150, hi=2500):
    """Moving water: band-limited noise with a slow, uneven swell."""
    t = t_of(n)
    swell = 0.6 + 0.4 * np.sin(2 * np.pi * 0.7 * t + 1.3 * np.sin(2 * np.pi * 0.23 * t))
    return filt(noise(n), "bandpass", [lo, hi]) * swell


def splash(dur=0.6, bright=5000):
    n = int(dur * SR)
    t = t_of(n)
    body = filt(noise(n), "bandpass", [300, bright]) * np.exp(-t * 7) * (1 - np.exp(-t * 400))
    spray = filt(noise(n), "highpass", 3000) * np.exp(-t * 12) * 0.5
    return body + spray + bubbles(dur, 60) * 0.4


def metal(f0, dur, decay=3.0, partials=(1.0, 2.76, 5.40, 8.93), weights=(1.0, 0.6, 0.35, 0.2)):
    """Inharmonic metallic ring (a struck bar / blade)."""
    n = int(dur * SR)
    t = t_of(n)
    x = np.zeros(n)
    for p, w in zip(partials, weights):
        x += w * np.sin(2 * np.pi * f0 * p * t + rng.uniform(0, 6.28)) * np.exp(-t * decay * (0.6 + p * 0.25))
    return x


def shimmer(dur, root=72, chord=(0, 7, 12, 16, 19)):
    """Glassy underwater chime: a soft bell arpeggio."""
    n = int(dur * SR)
    x = np.zeros(n)
    for i, s in enumerate(chord):
        bell = metal(midi_hz(root + s), dur - i * 0.06, decay=2.2, partials=(1.0, 2.0, 3.01), weights=(1.0, 0.25, 0.1))
        place(x, bell, i * 0.06, 0.5)
    return x


def whoosh(dur, lo=200, hi=3000, rise=True):
    n = int(dur * SR)
    t = t_of(n)
    sweep = (t / dur) if rise else (1 - t / dur)
    a = filt(noise(n), "bandpass", [lo, lo * 3])
    b = filt(noise(n), "bandpass", [hi / 3, hi])
    return (a * (1 - sweep) + b * sweep * 1.3) * np.sin(np.pi * t / dur) ** 1.2


def whale(f0, f1, dur):
    """Whale-like moan: a slowly gliding, slightly wobbling tone with a few harmonics."""
    n = int(dur * SR)
    t = t_of(n)
    glide = f0 + (f1 - f0) * (0.5 - 0.5 * np.cos(np.pi * t / dur))
    f = glide * (1 + 0.01 * np.sin(2 * np.pi * 5 * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = np.sin(ph) + 0.4 * np.sin(2 * ph) + 0.15 * np.sin(3 * ph)
    return filt(x, "lowpass", 1800) * np.sin(np.pi * t / dur) ** 1.5


def make_loop(x, xfade=0.3):
    k = int(xfade * SR)
    body = x[:-k].copy()
    tail = x[-k:]
    ramp = np.linspace(0, 1, k)
    body[:k] = body[:k] * ramp + tail * (1 - ramp)
    return body


# ------------------------------------------------------------------------------------------ sfx

def aquaman_suit_up():
    dur = 1.6
    n = int(dur * SR)
    x = whoosh(dur, 150, 4000, True) * 0.5 + water(n) * 0.3 * np.linspace(0.2, 1, n)
    x += bubbles(dur, 70, 400, 1800) * 0.5
    place(x, splash(0.8), 0.85, 0.9)
    place(x, sub_boom(0.8, 0.7), 0.85, 0.7)
    place(x, shimmer(0.9, 74), 0.9, 0.4)
    save("aquaman_suit_up", fade(x, 0.002, 0.2), 0.9)


def aquaman_suit_down():
    dur = 1.0
    n = int(dur * SR)
    t = t_of(n)
    x = whoosh(dur, 150, 3000, False) * 0.6 + bubbles(dur, 50, 500, 1500) * 0.6 * np.exp(-t * 2)
    save("aquaman_suit_down", fade(x, 0.002, 0.15), 0.8)


def swim_dash():
    dur = 0.8
    n = int(dur * SR)
    t = t_of(n)
    rush = filt(noise(n), "bandpass", [200, 2200]) * np.exp(-t * 4) * (1 - np.exp(-t * 80))
    x = rush + bubbles(dur, 90, 400, 1600) * 0.6 * np.exp(-t * 3)
    place(x, sub_boom(0.5, 0.5), 0.0, 0.5)
    save("swim_dash", fade(x, 0.002, 0.1), 0.85)


def swim_loop():
    dur = 3.3
    n = int(dur * SR)
    x = water(n, 120, 1800) * 0.8 + filt(noise(n), "lowpass", 300) * 0.5 + bubbles(dur, 25, 300, 1100) * 0.35
    save("swim_loop", make_loop(x), 0.7)


def trident_summon():
    dur = 1.3
    n = int(dur * SR)
    x = water(n, 200, 3000) * np.linspace(1, 0, n) ** 2 * 0.5
    place(x, bubbles(0.6, 80, 600, 2000), 0.0, 0.5)
    place(x, metal(523, 1.0, 2.5), 0.35, 0.7)
    place(x, shimmer(0.9, 79, (0, 4, 7, 12)), 0.35, 0.5)
    save("trident_summon", fade(x, 0.002, 0.2), 0.85)


def trident_swing():
    dur = 0.35
    n = int(dur * SR)
    t = t_of(n)
    x = filt(noise(n), "bandpass", [400, 4000]) * np.sin(np.pi * t / dur) ** 2
    x += metal(880, dur, 9.0) * 0.15
    save("trident_swing", fade(x, 0.002, 0.05), 0.8)


def trident_throw():
    dur = 0.7
    n = int(dur * SR)
    t = t_of(n)
    x = whoosh(dur, 300, 5000, True) * 0.9
    x += metal(660, dur, 5.0) * 0.3 * (1 - np.exp(-t * 50))
    save("trident_throw", fade(x, 0.002, 0.1), 0.85)


def trident_hit():
    dur = 0.9
    n = int(dur * SR)
    t = t_of(n)
    thud = filt(noise(n), "lowpass", 700) * np.exp(-t * 18)
    x = thud + metal(392, dur, 3.5) * 0.6
    place(x, splash(0.6), 0.0, 0.5)
    place(x, sub_boom(0.5, 0.6), 0.0, 0.6)
    save("trident_hit", fade(x, 0.001, 0.15), 0.9)


def trident_return():
    dur = 0.8
    n = int(dur * SR)
    x = whoosh(dur, 300, 4000, False) * 0.6
    place(x, metal(784, 0.5, 6.0), 0.55, 0.4)
    save("trident_return", fade(x, 0.002, 0.1), 0.8)


def shark_summon():
    dur = 2.0
    n = int(dur * SR)
    t = t_of(n)
    x = filt(noise(n), "lowpass", 400) * np.sin(np.pi * np.minimum(t / 1.2, 1.0)) * 0.6
    place(x, bubbles(1.2, 70, 200, 900), 0.0, 0.6)
    place(x, splash(1.0, 6000), 1.0, 1.0)
    place(x, sub_boom(1.0, 0.9), 0.95, 0.9)
    # A low, ominous two-note pulse underneath (original motif).
    for i, m in enumerate((33, 34, 33, 34)):
        note = np.sin(2 * np.pi * midi_hz(m) * t_of(int(0.22 * SR))) * np.exp(-t_of(int(0.22 * SR)) * 6)
        place(x, note, 0.05 + i * 0.25, 0.5)
    save("shark_summon", fade(x, 0.002, 0.25), 0.9)


def shark_bite():
    dur = 0.5
    n = int(dur * SR)
    t = t_of(n)
    snap = filt(noise(n), "bandpass", [800, 6000]) * np.exp(-t * 40)
    crunch = filt((rng.uniform(0, 1, n) < 0.03) * rng.uniform(-1, 1, n), "bandpass", [600, 4000]) * np.exp(-t * 10) * 4
    thud = np.sin(2 * np.pi * 70 * t) * np.exp(-t * 14)
    x = snap * 0.8 + crunch * 0.6 + thud * 0.9
    place(x, bubbles(0.4, 60, 300, 1200), 0.05, 0.3)
    save("shark_bite", fade(x, 0.001, 0.08), 0.95)


def sea_call():
    dur = 3.2
    n = int(dur * SR)
    x = np.zeros(n)
    # A conch horn (original), then whales and the sea answering.
    horn = brass_note(50, 1.3, 0.9, 0.6)
    horn = filt(horn, "lowpass", 2200)
    place(x, horn, 0.0, 0.8)
    place(x, brass_note(57, 0.9, 0.7, 0.5), 0.9, 0.5)
    place(x, whale(220, 330, 1.6), 1.3, 0.5)
    place(x, whale(300, 190, 1.4), 1.7, 0.4)
    x += water(n, 100, 1500) * 0.15
    place(x, bubbles(1.8, 50, 300, 1500), 1.2, 0.4)
    place(x, shimmer(1.5, 69, (0, 5, 7, 12, 17)), 1.4, 0.3)
    save("sea_call", fade(x, 0.002, 0.4), 0.9)


def main():
    aquaman_suit_up()
    aquaman_suit_down()
    swim_dash()
    swim_loop()
    trident_summon()
    trident_swing()
    trident_throw()
    trident_hit()
    trident_return()
    shark_summon()
    shark_bite()
    sea_call()


if __name__ == "__main__":
    main()
