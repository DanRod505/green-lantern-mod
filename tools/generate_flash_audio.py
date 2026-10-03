#!/usr/bin/env python3
"""
Audio of the Flash (DC Universe expansion): Speed Force crackles, the run start, sound barrier,
skid, super jump, landing, tornado, molecular vibration, Speed Force lightning, the suit and the
ORIGINAL running theme.

Everything is synthesized from scratch (no samples, no third-party music). The theme is rendered as
two aligned stereo stems that loop seamlessly, like the flight theme:

* speed_theme_base.ogg - pulsing 16th-note strings, synth bass, kick and hi-hats
* speed_theme_peak.ogg - brass hook, choir, big toms, snare and cymbals

The game plays the base stem while running fast and fades the peak stem in past the sound barrier.

Usage:  pip install numpy scipy soundfile
        python tools/generate_flash_audio.py
"""
import os

import numpy as np
import soundfile as sf

from generate_flight_audio import (ROOT, SR, Track, brass_note, choir_note, crash, fade, filt, midi_hz, normalize,
                                   reverb, save, snare, strings_note, sub_boom, swell_cymbal, timpani, tom)

rng = np.random.default_rng(1956)


def noise(n):
    return rng.uniform(-1, 1, n)


def t_of(n):
    return np.arange(n) / SR


def chirp(f0, f1, dur, curve=1.0):
    n = int(dur * SR)
    t = t_of(n)
    f = f0 + (f1 - f0) * (t / dur) ** curve
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def crackle(n, density=0.02, bright=3000):
    """Electric crackle: sparse random impulses, each a tiny resonant zap."""
    imp = (rng.uniform(0, 1, n) < density) * rng.uniform(-1, 1, n)
    x = filt(imp, "bandpass", [bright * 0.5, min(bright * 3.5, 20000)], 2)
    buzz = np.sign(np.sin(2 * np.pi * 120 * t_of(n))) * 0.15
    gate = filt((rng.uniform(0, 1, n) < 0.002).astype(float), "lowpass", 30) * 60
    return x * 4 + buzz * np.clip(gate, 0, 1)


def whoosh(dur, lo=300, hi=4000, rise=True):
    n = int(dur * SR)
    t = t_of(n)
    sweep = (t / dur) if rise else (1 - t / dur)
    a = filt(noise(n), "bandpass", [lo, lo * 3])
    b = filt(noise(n), "bandpass", [hi / 3, hi])
    return (a * (1 - sweep) + b * sweep * 1.3) * np.sin(np.pi * t / dur) ** 1.2


def make_loop(x, xfade=0.25):
    """Seamless loop: crossfades the extra tail into the head."""
    k = int(xfade * SR)
    body = x[:-k].copy()
    tail = x[-k:]
    ramp = np.linspace(0, 1, k)
    body[:k] = body[:k] * ramp + tail * (1 - ramp)
    return body


# ------------------------------------------------------------------------------------------ sfx

def flash_suit_up():
    dur = 1.4
    n = int(dur * SR)
    t = t_of(n)
    w = whoosh(dur, 200, 6000, True)
    c = crackle(n, 0.03) * np.exp(-((t - 0.5) / 0.4) ** 2)
    zap = chirp(200, 2400, 0.5, 2) * np.exp(-np.abs(t_of(int(0.5 * SR)) - 0.45) * 8)
    x = w * 0.7 + c * 0.6
    x[: len(zap)] += zap * 0.25
    hit = sub_boom(0.8, 0.8)
    i = int(0.9 * SR)
    x[i:i + len(hit)] += hit[: n - i] * 0.8
    save("flash_suit_up", fade(x, 0.002, 0.2), 0.9)


def flash_suit_down():
    dur = 1.0
    n = int(dur * SR)
    t = t_of(n)
    x = whoosh(dur, 200, 5000, False) * 0.8 + crackle(n, 0.02) * np.exp(-t * 4) * 0.5
    save("flash_suit_down", fade(x, 0.002, 0.15), 0.8)


def speed_start():
    dur = 0.9
    n = int(dur * SR)
    t = t_of(n)
    burst = filt(noise(n), "bandpass", [500, 7000]) * np.exp(-t * 6) * (1 - np.exp(-t * 200))
    zap = crackle(n, 0.04, 3500) * np.exp(-t * 7)
    thump = sub_boom(0.6, 0.7)
    x = burst + zap * 0.7
    x[: len(thump)] += thump * 0.6
    save("speed_start", fade(x, 0.001, 0.1), 0.9)


def speed_crackle():
    dur = 0.6
    n = int(dur * SR)
    t = t_of(n)
    x = crackle(n, 0.05, 4000) * np.sin(np.pi * t / dur) ** 0.5
    save("speed_crackle", fade(x, 0.001, 0.05), 0.75)


def speed_boom():
    """Sound barrier with a thunder-like Speed Force crack."""
    dur = 3.0
    n = int(dur * SR)
    t = t_of(n)
    crack = filt(noise(n), "highpass", 1500) * np.exp(-t * 18)
    boom = sub_boom(dur, 1.0)
    rumble = filt(noise(n), "lowpass", 220) * np.exp(-t * 1.6) * 2.5
    zaps = crackle(n, 0.03, 3000) * np.exp(-t * 3)
    x = crack * 1.2 + boom * 1.4 + rumble + zaps * 0.5
    save("speed_boom", fade(x, 0.0005, 0.4), 0.97)


def speed_skid():
    dur = 1.1
    n = int(dur * SR)
    t = t_of(n)
    scrape = filt(noise(n), "bandpass", [700, 3500]) * (0.6 + 0.4 * np.sin(2 * np.pi * 37 * t))
    grit = filt((rng.uniform(0, 1, n) < 0.03) * noise(n), "bandpass", [2000, 9000]) * 3
    env = np.exp(-t * 2.2) * (1 - np.exp(-t * 80))
    x = (scrape + grit) * env + filt(noise(n), "lowpass", 300) * np.exp(-t * 4) * 0.8
    save("speed_skid", fade(x, 0.002, 0.2), 0.85)


def super_jump():
    dur = 1.2
    n = int(dur * SR)
    t = t_of(n)
    thump = sub_boom(0.7, 1.0)
    x = whoosh(dur, 400, 6000, True) * 0.9 + crackle(n, 0.02) * np.exp(-t * 5) * 0.5
    x[: len(thump)] += thump * 0.9
    save("super_jump", fade(x, 0.001, 0.2), 0.9)


def speed_landing():
    dur = 1.8
    n = int(dur * SR)
    t = t_of(n)
    thump = np.sin(2 * np.pi * np.cumsum(48 * (1 + 3 * np.exp(-t * 20))) / SR) * np.exp(-t * 3.5)
    dust = filt(noise(n), "bandpass", [200, 2500]) * np.exp(-t * 8)
    zaps = crackle(n, 0.04, 3000) * np.exp(-t * 6)
    save("speed_landing", fade(thump * 1.5 + dust + zaps * 0.6, 0.0005, 0.2), 0.95)


def tornado_loop():
    dur = 4.0
    n = int((dur + 0.3) * SR)
    t = t_of(n)
    # Two layers of wind whose band wobbles around: a deep roar and a whistling top.
    roar = np.zeros(n)
    base = noise(n)
    for f0, speed, gain in ((180, 0.5, 1.0), (420, 0.75, 0.6), (900, 1.0, 0.35)):
        mod = 0.5 + 0.5 * np.sin(2 * np.pi * speed * t + rng.uniform(0, 6))
        band = filt(base, "bandpass", [f0 * 0.6, f0 * 1.6])
        roar += band * gain * (0.5 + 0.5 * mod)
    whistle = sum(np.sin(2 * np.pi * np.cumsum(f * (1 + 0.05 * np.sin(2 * np.pi * 0.5 * t))) / SR) * 0.03
                  for f in (1250, 1870))
    debris = filt((rng.uniform(0, 1, n) < 0.004) * noise(n), "bandpass", [1500, 6000]) * 2
    x = roar + whistle + debris + crackle(n, 0.004, 3000) * 0.3
    save("tornado_loop", make_loop(x, 0.3), 0.8)


def tornado_start():
    dur = 1.8
    n = int(dur * SR)
    t = t_of(n)
    x = whoosh(dur, 150, 3000, True) * (t / dur) ** 0.6 * 1.2
    x += filt(noise(n), "lowpass", 250) * (t / dur) ** 2 * 1.5
    save("tornado_start", fade(x, 0.01, 0.25), 0.9)


def vibration(n, f0=95):
    t = t_of(n)
    phase = 2 * np.pi * f0 * t
    tone = sum(np.sin(k * phase) / k ** 0.7 for k in range(1, 12))
    trem = 0.6 + 0.4 * np.sin(2 * np.pi * 32 * t)
    shimmer = filt(noise(n), "bandpass", [3000, 9000]) * (0.5 + 0.5 * np.sin(2 * np.pi * 16 * t)) * 0.3
    return tone * trem * 0.25 + shimmer


def phase_start():
    dur = 0.9
    n = int(dur * SR)
    t = t_of(n)
    x = vibration(n)
    x = x * (t / dur) ** 0.7 + chirp(150, 900, dur, 1.5) * 0.15 * np.sin(np.pi * t / dur)
    save("phase_start", fade(x, 0.005, 0.1), 0.8)


def phase_loop():
    # 95 Hz * 2 s and the tremolo rates are whole numbers of cycles: loops seamlessly.
    dur = 2.0
    n = int((dur + 0.2) * SR)
    save("phase_loop", make_loop(vibration(n), 0.2), 0.7)


def phase_end():
    dur = 0.7
    n = int(dur * SR)
    t = t_of(n)
    x = vibration(n) * (1 - t / dur) ** 1.5 + chirp(900, 120, dur, 0.6) * 0.15 * (1 - t / dur)
    pop = filt(noise(int(0.08 * SR)), "lowpass", 1200) * np.exp(-t_of(int(0.08 * SR)) * 60)
    i = int(0.55 * SR)
    x[i:i + len(pop)] += pop[: n - i]
    save("phase_end", fade(x, 0.002, 0.08), 0.8)


def lightning_throw():
    dur = 0.8
    n = int(dur * SR)
    t = t_of(n)
    zap = chirp(3200, 180, dur, 0.4) * np.exp(-t * 5)
    crack = filt(noise(n), "highpass", 2000) * np.exp(-t * 25)
    x = zap * 0.5 + crack + crackle(n, 0.05, 3500) * np.exp(-t * 6) * 0.8
    save("lightning_throw", fade(x, 0.0005, 0.1), 0.9)


def lightning_hit():
    dur = 2.0
    n = int(dur * SR)
    t = t_of(n)
    crack = filt(noise(n), "highpass", 1200) * np.exp(-t * 14)
    thunder = filt(noise(n), "lowpass", 300) * np.exp(-t * 2.2) * 2.5 * (1 + 0.5 * np.sin(2 * np.pi * 7 * t))
    boom = sub_boom(1.2, 0.8)
    x = crack * 1.3 + thunder + crackle(n, 0.03) * np.exp(-t * 4) * 0.6
    x[: len(boom)] += boom
    save("lightning_hit", fade(x, 0.0005, 0.3), 0.95)


def power_select():
    dur = 0.18
    n = int(dur * SR)
    t = t_of(n)
    x = (np.sin(2 * np.pi * 880 * t) * (t < 0.07) + np.sin(2 * np.pi * 1320 * t) * (t >= 0.07)) * np.exp(-t * 12)
    x += filt(noise(n), "bandpass", [3000, 8000]) * np.exp(-t * 40) * 0.3
    save("power_select", fade(x, 0.001, 0.03), 0.6)


# ------------------------------------------------------------------------------------- the theme

BPM = 150
BEAT = 60.0 / BPM
BAR = 4 * BEAT
BARS = 16
LOOP = BARS * BAR

CHORDS = {
    "Em": (40, [52, 55, 59]), "C": (36, [48, 52, 55]), "G": (43, [55, 59, 62]), "D": (38, [50, 54, 57]),
    "Am": (45, [57, 60, 64]), "B": (47, [47, 51, 54]),
}
PROGRESSION = ["Em", "Em", "C", "D", "Em", "Em", "Am", "B"] * 2

# Hook for 8 bars (note, beats): short, rising, urgent.
HOOK = [
    [(64, 0.5), (67, 0.5), (71, 1), (69, 0.5), (71, 0.5), (76, 1)],
    [(74, 1.5), (71, 0.5), (69, 1), (67, 1)],
    [(64, 0.5), (67, 0.5), (72, 1), (71, 0.5), (72, 0.5), (76, 1)],
    [(78, 2), (74, 1), (69, 1)],
    [(64, 0.5), (67, 0.5), (71, 1), (69, 0.5), (71, 0.5), (79, 1)],
    [(78, 1.5), (76, 0.5), (74, 1), (71, 1)],
    [(72, 1), (76, 1), (81, 1), (79, 1)],
    [(78, 2), (75, 1), (71, 1)],
]


def kick(vel=1.0):
    n = int(0.35 * SR)
    t = t_of(n)
    f = 50 * (1 + 2.5 * np.exp(-t * 35))
    return vel * (np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 8) + filt(noise(n), "highpass", 3000) * np.exp(-t * 200) * 0.4)


def hat(vel=1.0, open_=False):
    n = int((0.25 if open_ else 0.06) * SR)
    t = t_of(n)
    return vel * filt(noise(n), "highpass", 7000) * np.exp(-t * (12 if open_ else 70))


def synth_bass(m, dur, vel=1.0):
    n = int(dur * SR)
    t = t_of(n)
    f = midi_hz(m)
    ph = 2 * np.pi * f * t
    saw = sum(np.sin(k * ph) / k for k in range(1, 14))
    env = np.minimum(1, t / 0.005) * np.exp(-t * 6)
    return vel * filt(saw, "lowpass", 900) * env


def render_base(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Strings pulsing in 16ths: root, fifth, octave pattern.
        pattern = [12, 12, 19, 12, 12, 19, 12, 24, 12, 12, 19, 12, 24, 19, 12, 19]
        for i, iv in enumerate(pattern):
            vel = 1.0 if i % 4 == 0 else 0.6
            track.add(strings_note(root + 12 + iv - 12, BEAT * 0.25, vel, spiccato=True), t0 + i * BEAT * 0.25, 0.3, pan=-0.3 if i % 2 else 0.3)
        # Synth bass on the 8ths, octave jumps.
        for i in range(8):
            track.add(synth_bass(root + (12 if i % 2 else 0), BEAT * 0.5, 1.0 if i % 2 == 0 else 0.7), t0 + i * BEAT * 0.5, 0.35)
        # Pad.
        for j, m in enumerate(triad):
            track.add(strings_note(m + 12, BAR * 1.05), t0, 0.1, pan=(j - 1) * 0.6)
        # Kick on every beat, hats on the off-beats.
        for k in range(4):
            track.add(kick(), t0 + k * BEAT, 0.5)
            track.add(hat(0.8), t0 + (k + 0.5) * BEAT, 0.18, pan=0.4)
            track.add(hat(0.5), t0 + (k + 0.25) * BEAT, 0.1, pan=0.4)
            track.add(hat(0.5), t0 + (k + 0.75) * BEAT, 0.1, pan=0.4)
        if b % 4 == 3:
            track.add(timpani(root + 12 if root < 40 else root, 0.8, 0.8), t0 + 3.5 * BEAT, 0.3)


def render_peak(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Brass stabs: 1, the and-of-2, 4.
        for hit, length in ((0.0, 0.9), (1.5, 0.6), (3.0, 0.8)):
            for j, m in enumerate(triad):
                track.add(brass_note(m, length * BEAT, 1.0, bright=1.1), t0 + hit * BEAT, 0.1, pan=(j - 1) * 0.5)
        for j, m in enumerate(triad):
            track.add(choir_note(m + 12, BAR * 1.05), t0, 0.09, pan=(1 - j) * 0.5)
        # Driving toms in 16ths, snare on 2 and 4.
        for s in range(16):
            vel = 1.0 if s % 4 == 0 else (0.7 if s % 2 == 0 else 0.45)
            track.add(tom(47 if s % 4 else 42, vel), t0 + s * BEAT / 4, 0.18, pan=0.25 if s % 2 else -0.25)
        track.add(snare(), t0 + BEAT, 0.32)
        track.add(snare(), t0 + 3 * BEAT, 0.32)
        track.add(sub_boom(0.8, 0.8), t0, 0.35)
        if b % 4 == 0:
            track.add(crash(2.0), t0, 0.3)
        if b % 8 == 7:
            track.add(swell_cymbal(BAR), t0, 0.22)
    # Trumpet hook over all 16 bars (an octave up in the second half).
    for b in range(16):
        t = offset + b * BAR
        for m, beats in HOOK[b % 8]:
            up = 12 if b >= 8 else 0
            track.add(brass_note(m + up - 12, beats * BEAT * 0.95, 1.0, bright=1.3), t, 0.22, pan=0.1)
            t += beats * BEAT


def render_stem(fn):
    track = Track(LOOP * 2)
    fn(track, 0.0)
    fn(track, LOOP)
    l, r = reverb(track.l, track.r, seconds=1.6, wet=0.22)
    a, b = int(LOOP * SR), int(2 * LOOP * SR)
    return np.stack([l[a:b], r[a:b]], axis=1)


def master(x):
    x = filt(x.T, "highpass", 30).T
    return np.tanh(x * 1.3) / np.tanh(1.3)


def speed_theme():
    base = master(render_stem(render_base))
    peak = master(render_stem(render_peak))
    scale = 0.85 / np.max(np.abs(base + peak))
    for name, stem in (("speed_theme_base", base), ("speed_theme_peak", peak)):
        path = os.path.join(ROOT, "music", name + ".ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sf.write(path, (stem * scale).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        print("wrote", os.path.relpath(path), f"{len(stem) / SR:.2f}s")


def main():
    flash_suit_up()
    flash_suit_down()
    speed_start()
    speed_crackle()
    speed_boom()
    speed_skid()
    super_jump()
    speed_landing()
    tornado_loop()
    tornado_start()
    phase_start()
    phase_loop()
    phase_end()
    lightning_throw()
    lightning_hit()
    power_select()
    speed_theme()


if __name__ == "__main__":
    main()
