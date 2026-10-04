#!/usr/bin/env python3
"""
Audio of Aquaman's Kraken (v1.10.0): rising from the deep, the roar, the heavy wet steps, the
tentacle slam, the water jet, the swim strokes, the hurt growl and the dying groan.

Everything is synthesized from scratch: no samples, no third-party audio.

Usage:  pip install numpy scipy soundfile
        python tools/generate_kraken_audio.py
"""
import numpy as np

from generate_aquaman_audio import bubbles, noise, place, splash, t_of, water, whoosh
from generate_flight_audio import SR, fade, filt, save, sub_boom

rng = np.random.default_rng(1010)


def growl(f0, f1, dur, rough=0.6, formants=(320, 720, 1400)):
    """A monster's voice: a buzzy low tone gliding f0 -> f1, roughened, shaped by throat formants."""
    n = int(dur * SR)
    t = t_of(n)
    glide = f0 + (f1 - f0) * (t / dur) ** 0.8
    wobble = 1 + 0.03 * np.sin(2 * np.pi * 4.5 * t) + 0.015 * rng.standard_normal(n).cumsum() / np.sqrt(n)
    ph = 2 * np.pi * np.cumsum(glide * wobble) / SR
    # Buzzy source: a band-limited sawtooth.
    src = sum(np.sin(k * ph) / k for k in range(1, 24))
    # Rough, fluttering throat (amplitude modulation around 28 Hz plus noise).
    flutter = 1 + rough * (0.5 * np.sin(2 * np.pi * 28 * t) + 0.5 * filt(rng.uniform(-1, 1, n), "lowpass", 60) * 4)
    src = src * flutter
    out = np.zeros(n)
    for i, f in enumerate(formants):
        out += filt(src, "bandpass", [f * 0.75, f * 1.3]) * (1.0 / (i + 1))
    breath = filt(noise(n), "bandpass", [200, 2500]) * 0.35
    env = np.minimum(t / 0.15, 1.0) * np.minimum((dur - t) / 0.5, 1.0)
    return (out + breath) * np.clip(env, 0, 1)


def kraken_summon():
    dur = 4.0
    n = int(dur * SR)
    t = t_of(n)
    # The deep stirs: a rumble rising, then the sea bursts open and the Kraken roars.
    x = filt(noise(n), "lowpass", 160) * np.minimum(t / 1.6, 1.0) * np.exp(-np.maximum(t - 2.0, 0) * 1.2) * 1.4
    place(x, bubbles(1.6, 90, 150, 700), 0.2, 0.7)
    place(x, splash(1.4, 5000), 1.5, 1.2)
    place(x, sub_boom(1.5, 1.2), 1.45, 1.0)
    place(x, growl(70, 48, 2.3, 0.7), 1.6, 1.1)
    place(x, growl(140, 96, 2.0, 0.5, (500, 1100, 2200)), 1.7, 0.35)
    save("kraken_summon", fade(x, 0.01, 0.5), 0.92)


def kraken_roar():
    x = growl(64, 44, 2.6, 0.75)
    place(x, growl(128, 90, 2.3, 0.5, (450, 1000, 2100)), 0.08, 0.3)
    x += filt(noise(len(x)), "lowpass", 120) * 0.25
    save("kraken_roar", fade(x, 0.01, 0.4), 0.9)


def kraken_step():
    dur = 1.0
    n = int(dur * SR)
    t = t_of(n)
    thud = np.sin(2 * np.pi * np.cumsum(38 * (1 + 1.2 * np.exp(-t * 18))) / SR) * np.exp(-t * 5)
    squelch = filt(noise(n), "bandpass", [180, 1400]) * np.exp(-t * 14) * (1 - np.exp(-t * 300))
    x = thud * 1.2 + squelch * 0.5
    place(x, bubbles(0.5, 40, 200, 800), 0.05, 0.25)
    save("kraken_step", fade(x, 0.002, 0.2), 0.95)


def kraken_slam():
    dur = 1.4
    n = int(dur * SR)
    x = np.zeros(n)
    # The tentacle swings (a rising whoosh), then the crash.
    place(x, whoosh(0.4, 120, 1600, rise=True), 0.0, 0.9)
    crash_n = int(1.0 * SR)
    ct = t_of(crash_n)
    crash = filt(noise(crash_n), "lowpass", 2500) * np.exp(-ct * 7) * (1 - np.exp(-ct * 800))
    place(x, crash, 0.38, 1.0)
    place(x, sub_boom(1.0, 1.3), 0.37, 1.0)
    place(x, splash(0.8, 4000), 0.4, 0.5)
    save("kraken_slam", fade(x, 0.002, 0.2), 0.95)


def kraken_jet():
    dur = 1.6
    n = int(dur * SR)
    t = t_of(n)
    # High pressure water: a hiss over a roaring body, with a hard start.
    roar = filt(noise(n), "bandpass", [150, 1200]) * 0.9
    hiss = filt(noise(n), "bandpass", [1500, 7000]) * 0.6
    gurgle = water(n, 100, 900) * 0.4
    env = np.minimum(t / 0.04, 1.0) * (0.85 + 0.15 * np.sin(2 * np.pi * 9 * t))
    x = (roar + hiss + gurgle) * env
    place(x, sub_boom(0.6, 0.6), 0.0, 0.6)
    save("kraken_jet", fade(x, 0.002, 0.25), 0.88)


def kraken_swim():
    x = whoosh(0.7, 80, 1200, rise=False) * 1.2
    x += water(len(x), 100, 900) * 0.3 * np.sin(np.pi * t_of(len(x)) / 0.7)
    place(x, bubbles(0.6, 70, 200, 900), 0.05, 0.4)
    save("kraken_swim", fade(x, 0.01, 0.15), 0.8)


def kraken_hurt():
    x = growl(95, 60, 0.8, 0.9, (380, 860, 1700))
    save("kraken_hurt", fade(x, 0.005, 0.2), 0.9)


def kraken_death():
    dur = 3.8
    x = growl(60, 26, dur, 0.8)
    n = len(x)
    t = t_of(n)
    x *= np.exp(-t * 0.35)
    place(x, growl(120, 50, 2.2, 0.6, (500, 1000, 2000)), 0.0, 0.3)
    place(x, bubbles(2.5, 80, 120, 600), 1.0, 0.6)
    x += filt(noise(n), "lowpass", 140) * 0.3 * np.exp(-t * 0.6)
    save("kraken_death", fade(x, 0.01, 0.8), 0.9)


def main():
    kraken_summon()
    kraken_roar()
    kraken_step()
    kraken_slam()
    kraken_jet()
    kraken_swim()
    kraken_hurt()
    kraken_death()


if __name__ == "__main__":
    main()
