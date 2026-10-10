#!/usr/bin/env python3
"""
Audio of Batman (DC Universe expansion): the suit, the cape (spreading, and the wind while gliding),
the batarang, the grappling hook, the swarm of bats and the Batmobile (ignition, engine loop, boost
and missiles).

Everything is synthesized from scratch: no samples, no third-party audio.

Usage:  pip install numpy scipy soundfile
        python tools/generate_batman_audio.py
"""
import numpy as np

from generate_aquaman_audio import make_loop, metal, noise, place, t_of, whoosh
from generate_flight_audio import SR, fade, filt, save, sub_boom


def flap(dur=0.09, f0=900):
    """One leathery wing beat: a short, low-passed burst of noise."""
    n = int(dur * SR)
    t = t_of(n)
    return filt(noise(n), "bandpass", [f0 * 0.3, f0]) * np.sin(np.pi * t / dur) ** 2


def cloth(dur, lo=120, hi=1600, rate=11.0):
    """Heavy fabric snapping in the air."""
    n = int(dur * SR)
    t = t_of(n)
    flutter = 0.55 + 0.45 * np.abs(np.sin(2 * np.pi * rate * t + 2 * np.sin(2 * np.pi * 1.7 * t)))
    return filt(noise(n), "bandpass", [lo, hi]) * flutter


def click(dur=0.03, f=2600):
    n = int(dur * SR)
    t = t_of(n)
    return np.sin(2 * np.pi * f * t) * np.exp(-t * 180) + filt(noise(n), "highpass", 3000) * np.exp(-t * 300) * 0.6


def engine(n, base=48.0, rough=0.25):
    """A V8 rumble: firing pulses at `base` Hz with a raspy harmonic stack."""
    t = t_of(n)
    f = base * (1 + 0.02 * np.sin(2 * np.pi * 0.9 * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    pulses = np.maximum(0, np.sin(ph)) ** 3
    x = pulses + 0.5 * np.sin(ph * 0.5) + 0.3 * np.sin(2 * ph)
    x += filt(noise(n), "lowpass", 900) * rough * (0.5 + pulses)
    return filt(x, "lowpass", 1400)


def batman_suit_up():
    dur = 1.4
    n = int(dur * SR)
    x = cloth(dur, 100, 1400, 9) * np.linspace(0.2, 1, n) ** 2 * 0.6
    place(x, whoosh(0.6, 150, 2500, True), 0.3, 0.7)
    for i, at in enumerate((0.7, 0.82, 0.95)):
        place(x, click(0.04, 1800 + i * 300), at, 0.6)
    place(x, metal(220, 0.6, 5.0), 0.95, 0.5)
    place(x, sub_boom(0.7, 0.8), 0.9, 0.8)
    save("batman_suit_up", fade(x, 0.002, 0.15), 0.9)


def batman_suit_down():
    dur = 0.9
    n = int(dur * SR)
    t = t_of(n)
    x = cloth(dur, 120, 1500, 12) * np.exp(-t * 3) * 0.7 + whoosh(dur, 150, 2000, False) * 0.5
    place(x, click(0.04, 1500), 0.05, 0.5)
    save("batman_suit_down", fade(x, 0.002, 0.15), 0.8)


def cape_glide():
    dur = 0.7
    n = int(dur * SR)
    t = t_of(n)
    # The cape snapping open like a sail.
    snap = filt(noise(n), "bandpass", [150, 2500]) * np.exp(-t * 14) * (1 - np.exp(-t * 900))
    x = snap + cloth(dur, 100, 1200, 14) * np.exp(-t * 4) * 0.5 + whoosh(dur, 200, 2000, True) * 0.3
    place(x, sub_boom(0.4, 0.4), 0.0, 0.4)
    save("cape_glide", fade(x, 0.002, 0.1), 0.85)


def cape_wind():
    dur = 3.4
    n = int(dur * SR)
    x = filt(noise(n), "bandpass", [180, 1600]) * 0.7 + cloth(dur, 80, 700, 7) * 0.45 + filt(noise(n), "lowpass", 220) * 0.4
    save("cape_wind", make_loop(x), 0.7)


def batarang_throw():
    dur = 0.45
    n = int(dur * SR)
    t = t_of(n)
    # A spinning blade: whoosh chopped at the spin rate.
    spin = 0.4 + 0.6 * np.abs(np.sin(2 * np.pi * 18 * t))
    x = whoosh(dur, 400, 5000, True) * spin
    place(x, click(0.03, 3200), 0.0, 0.4)
    save("batarang_throw", fade(x, 0.001, 0.08), 0.8)


def batarang_hit():
    dur = 0.5
    n = int(dur * SR)
    t = t_of(n)
    x = metal(1100, dur, 6.0) * 0.6 + filt(noise(n), "bandpass", [800, 6000]) * np.exp(-t * 40)
    save("batarang_hit", fade(x, 0.001, 0.1), 0.85)


def grapple_fire():
    dur = 0.6
    n = int(dur * SR)
    t = t_of(n)
    # Gas-powered launcher: a pop, then the cable hissing out.
    pop = filt(noise(n), "lowpass", 1500) * np.exp(-t * 60) + sub_boom(dur, 0.6) * np.exp(-t * 8)
    hiss = filt(noise(n), "bandpass", [2000, 7000]) * np.exp(-t * 4) * (1 - np.exp(-t * 60)) * 0.5
    save("grapple_fire", fade(pop + hiss, 0.001, 0.08), 0.85)


def grapple_hit():
    dur = 0.4
    n = int(dur * SR)
    t = t_of(n)
    x = filt(noise(n), "bandpass", [300, 3000]) * np.exp(-t * 35) + metal(650, dur, 9.0) * 0.5
    place(x, click(0.03, 2200), 0.0, 0.6)
    save("grapple_hit", fade(x, 0.001, 0.08), 0.85)


def grapple_reel():
    dur = 1.2
    n = int(dur * SR)
    t = t_of(n)
    # A motorized winch whining up, with the ratchet ticking.
    f = 300 + 500 * (t / dur) ** 0.6
    whine = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.4 + np.sin(2 * np.pi * np.cumsum(f * 2.01) / SR) * 0.2
    x = whine * np.sin(np.pi * t / dur) ** 0.5
    ticks = np.zeros(n)
    tpos = 0.0
    while tpos < dur:
        place(ticks, click(0.015, 3000), tpos, 0.3)
        tpos += 1.0 / (25 + 35 * tpos / dur)
    save("grapple_reel", fade(x + ticks, 0.005, 0.15), 0.75)


def bat_swarm():
    dur = 2.4
    n = int(dur * SR)
    t = t_of(n)
    rng = np.random.default_rng(1939)
    x = np.zeros(n)
    for _ in range(260):
        at = rng.uniform(0, dur - 0.1)
        place(x, flap(rng.uniform(0.05, 0.11), rng.uniform(600, 1500)), at, rng.uniform(0.2, 0.6) * min(1, at * 3 + 0.2))
    for _ in range(40):
        # High squeaks.
        d = rng.uniform(0.02, 0.05)
        m = int(d * SR)
        tt = t_of(m)
        f = rng.uniform(5000, 8000) * (1 + 0.3 * tt / d)
        place(x, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * tt / d), rng.uniform(0, dur), 0.12)
    x += whoosh(dur, 200, 2500, True) * 0.25
    save("bat_swarm", fade(x * np.minimum(1, (dur - t) * 2), 0.002, 0.2), 0.8)


def batmobile_start():
    dur = 2.0
    n = int(dur * SR)
    t = t_of(n)
    crank = np.zeros(n)
    k = int(0.5 * SR)
    crank[:k] = filt(noise(k), "bandpass", [200, 1200]) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 12 * t[:k]))) * 0.5
    rev = np.zeros(n)
    m = n - int(0.45 * SR)
    tt = t_of(m)
    base = 40 + 70 * np.exp(-((tt - 0.45) / 0.3) ** 2)
    # Pitch the rev by reading a fixed-rate engine along a variable phase.
    eng = engine(m * 3, 48.0)
    ph = np.clip(np.cumsum(base / 48.0), 0, len(eng) - 1)
    rev[-m:] = np.interp(ph, np.arange(len(eng)), eng) * np.minimum(1, tt * 8)
    x = crank + rev * 0.9
    place(x, sub_boom(0.8, 0.8), 0.45, 0.6)
    save("batmobile_start", fade(x, 0.002, 0.3), 0.9)


def batmobile_engine():
    dur = 3.0
    n = int(dur * SR)
    x = engine(n, 48.0) + filt(noise(n), "bandpass", [80, 400]) * 0.15
    save("batmobile_engine", make_loop(x, 0.4), 0.75)


def batmobile_boost():
    dur = 1.6
    n = int(dur * SR)
    t = t_of(n)
    # Afterburner: a deep roar with a crackle.
    roar = filt(noise(n), "lowpass", 700) * (1 - np.exp(-t * 12)) * np.exp(-t * 0.9)
    crackle = filt(noise(n), "highpass", 2500) * (np.random.default_rng(7).random(n) > 0.995) * 3.0 * np.exp(-t * 1.5)
    x = roar + crackle * 0.4 + whoosh(dur, 150, 3000, True) * 0.4
    place(x, sub_boom(1.0, 0.9), 0.0, 0.7)
    save("batmobile_boost", fade(x, 0.002, 0.3), 0.9)


def batmobile_missile():
    dur = 1.2
    n = int(dur * SR)
    t = t_of(n)
    launch = filt(noise(n), "bandpass", [300, 5000]) * np.exp(-t * 3) * (1 - np.exp(-t * 300))
    x = launch + whoosh(dur, 300, 4000, False) * 0.5
    place(x, click(0.03, 1200), 0.0, 0.8)
    place(x, sub_boom(0.6, 0.6), 0.0, 0.6)
    save("batmobile_missile", fade(x, 0.001, 0.2), 0.9)


def main():
    batman_suit_up()
    batman_suit_down()
    cape_glide()
    cape_wind()
    batarang_throw()
    batarang_hit()
    grapple_fire()
    grapple_hit()
    grapple_reel()
    bat_swarm()
    batmobile_start()
    batmobile_engine()
    batmobile_boost()
    batmobile_missile()


if __name__ == "__main__":
    main()
