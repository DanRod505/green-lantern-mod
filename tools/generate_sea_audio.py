#!/usr/bin/env python3
"""
Audio of the seas (DC Universe expansion, v1.12): the ORIGINAL swimming theme of Aquaman, the
shark's lunge and the voices of the creatures of Atlantis (giant manta ray, giant seahorse and the
glowing Atlantean dolphins).

Everything is written and synthesized from scratch here: no samples, no third-party music. The
theme is rendered as two aligned stereo stems that loop seamlessly, like the flight theme:

* swim_theme_base.ogg - harp arpeggios, low strings, a slow tide of drums and shakers
* swim_theme_peak.ogg - the heroic horn call of the King of Atlantis, choir, toms and cymbals

The game fades the base stem in while Aquaman swims and brings the peak stem in gradually as he
speeds up, so the music builds together with the swim.

Usage:  pip install numpy scipy soundfile
        python tools/generate_sea_audio.py
"""
import os

import numpy as np
import soundfile as sf

from generate_aquaman_audio import bubble, bubbles, make_loop, place, shimmer, splash, water, whale, whoosh
from generate_flight_audio import (ROOT, SR, Track, brass_note, choir_note, crash, fade, filt, midi_hz, reverb, save,
                                   snare, strings_note, sub_boom, swell_cymbal, timpani, tom)

rng = np.random.default_rng(1112)


def noise(n):
    return rng.uniform(-1, 1, n)


def t_of(n):
    return np.arange(n) / SR


# ------------------------------------------------------------------------------------ instruments

def harp(m, dur=1.6, vel=1.0):
    """Plucked harp string (Karplus-Strong): bright attack, warm decay."""
    n = int(dur * SR)
    f = midi_hz(m)
    period = max(2, int(round(SR / f)))
    buf = filt(rng.uniform(-1, 1, period), "lowpass", min(9000, f * 9))
    decay = 0.996 if f < 400 else 0.993
    cycles = []
    for _ in range(n // period + 1):
        cycles.append(buf)
        buf = decay * 0.5 * (buf + np.roll(buf, -1))
    out = np.concatenate(cycles)[:n]
    t = t_of(n)
    return vel * out * (1 - np.exp(-t * 900)) * np.minimum(1, (dur - t) / 0.05)


def low_drum(vel=1.0):
    """Deep, soft drum: the slow beat of the tide."""
    n = int(0.9 * SR)
    t = t_of(n)
    f = 46 * (1 + 0.8 * np.exp(-t * 20))
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 4.5)
    skin = filt(noise(n), "lowpass", 400) * np.exp(-t * 30) * 0.5
    return vel * (body + skin)


def shaker(vel=1.0):
    n = int(0.12 * SR)
    t = t_of(n)
    return vel * filt(noise(n), "bandpass", [4000, 11000]) * np.sin(np.pi * t / 0.12) ** 2


def pad(m, dur, vel=1.0):
    """Warm underwater pad: detuned soft saws through a slowly breathing filter."""
    n = int(dur * SR)
    t = t_of(n)
    f = midi_hz(m)
    x = np.zeros(n)
    for d in (-0.006, 0.0, 0.005):
        ph = 2 * np.pi * f * (1 + d) * t + rng.uniform(0, 6.28)
        x += sum(np.sin(k * ph) / k ** 1.4 for k in range(1, 9))
    x = filt(x, "lowpass", 1400)
    env = np.minimum(1, t / 0.8) * np.minimum(1, (dur - t) / 0.8)
    return vel * x * np.clip(env, 0, 1) / 3


# ------------------------------------------------------------------------------------- the theme

BPM = 100
BEAT = 60.0 / BPM
BAR = 4 * BEAT
BARS = 16
LOOP = BARS * BAR

CHORDS = {
    "Dm": (38, [50, 53, 57]), "Bb": (34, [46, 50, 53]), "C": (36, [48, 52, 55]), "Am": (45, [45, 48, 52]),
    "Gm": (43, [55, 58, 62]), "A": (45, [45, 49, 52]), "F": (41, [53, 57, 60]),
}
PROGRESSION = ["Dm", "Bb", "C", "Am", "Dm", "Gm", "Bb", "A", "Dm", "Bb", "F", "C", "Gm", "Bb", "Gm", "A"]

# The King's horn call (note, beats) for 8 bars: a rising fifth like a conch, then the wave falls.
CALL = [
    [(62, 1.5), (69, 0.5), (74, 2)],
    [(72, 1), (70, 1), (69, 2)],
    [(67, 1.5), (69, 0.5), (72, 1), (76, 1)],
    [(76, 3), (73, 1)],
    [(74, 1.5), (81, 0.5), (79, 1), (77, 1)],
    [(79, 1.5), (77, 0.5), (74, 2)],
    [(74, 1), (77, 1), (81, 1.5), (79, 0.5)],
    [(76, 2), (73, 1), (69, 1)],
]


def render_base(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Harp: rising and falling arpeggio in 8ths over two octaves (like a wave).
        notes = [triad[0] + 12, triad[1] + 12, triad[2] + 12, triad[0] + 24, triad[2] + 12, triad[1] + 12, triad[0] + 12, triad[2]]
        for i, m in enumerate(notes):
            track.add(harp(m, 1.6, 1.0 if i % 4 == 0 else 0.7), t0 + i * BEAT * 0.5, 0.22, pan=-0.45 + 0.9 * (i / 7))
        # Low strings and pad.
        track.add(strings_note(root + 12, BAR * 1.02, 0.9), t0, 0.2, pan=-0.1)
        for j, m in enumerate(triad):
            track.add(pad(m + 12, BAR * 1.05), t0, 0.12, pan=(j - 1) * 0.6)
        # The tide: deep drum on 1 and the "and of 2", shakers in 8ths.
        track.add(low_drum(1.0), t0, 0.45)
        track.add(low_drum(0.6), t0 + 1.5 * BEAT, 0.35)
        track.add(low_drum(0.8), t0 + 2.5 * BEAT, 0.3)
        for k in range(8):
            track.add(shaker(1.0 if k % 2 else 0.6), t0 + k * BEAT * 0.5, 0.12, pan=0.5)
        if b % 4 == 3:
            track.add(timpani(root + 12 if root < 40 else root, 1.0, 0.7), t0 + 3 * BEAT, 0.3)
        # Bubbles rising through the mix now and then.
        if b % 2 == 1:
            track.add(bubbles(1.2, 25, 500, 1800), t0 + 2 * BEAT, 0.08, pan=rng.uniform(-0.6, 0.6))
    # Whale song under the second half.
    track.add(whale(160, 240, 3.5), offset + 8 * BAR + BEAT, 0.12, pan=-0.5)
    track.add(whale(260, 170, 3.0), offset + 12 * BAR + BEAT, 0.1, pan=0.5)
    # Horns hint at the call, softly, in the second half.
    for b in range(8, 16):
        t = offset + b * BAR
        if b % 2:
            continue
        for m, beats in CALL[b - 8]:
            track.add(brass_note(m - 12, beats * BEAT * 0.96, 0.8, bright=0.4), t, 0.16, pan=-0.2)
            t += beats * BEAT


def render_peak(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Strings in 16ths: the current pulling forward.
        pattern = [0, 7, 12, 7, 0, 7, 12, 15, 0, 7, 12, 7, 0, 12, 7, 12]
        for i, iv in enumerate(pattern):
            track.add(strings_note(root + 12 + iv, BEAT * 0.25, 1.0 if i % 4 == 0 else 0.6, spiccato=True),
                      t0 + i * BEAT * 0.25, 0.22, pan=0.35 if i % 2 else -0.35)
        # Brass swells and choir.
        for j, m in enumerate(triad):
            track.add(brass_note(m, BEAT * 1.8, 0.9, bright=0.9), t0, 0.08, pan=(j - 1) * 0.5)
            track.add(brass_note(m, BEAT * 1.6, 0.8, bright=0.8), t0 + 2.5 * BEAT, 0.07, pan=(j - 1) * 0.5)
            track.add(choir_note(m + 12, BAR * 1.05), t0, 0.1, pan=(1 - j) * 0.5)
        # Drums: toms in 8ths with accents, snare on 2 and 4, a deep boom on the downbeat.
        for s in range(8):
            vel = 1.0 if s % 2 == 0 else 0.6
            track.add(tom(43 if s % 4 else 38, vel), t0 + s * BEAT / 2, 0.2, pan=0.2 if s % 2 else -0.2)
        track.add(snare(), t0 + BEAT, 0.26)
        track.add(snare(), t0 + 3 * BEAT, 0.26)
        track.add(sub_boom(1.2, 0.8), t0, 0.4)
        if b % 8 == 0:
            track.add(crash(), t0, 0.3)
        if b % 4 == 3:
            track.add(swell_cymbal(BAR), t0, 0.2)
    # The horn call in full, over all 16 bars (an octave up the second time).
    for b in range(16):
        t = offset + b * BAR
        for m, beats in CALL[b % 8]:
            up = 0 if b < 8 else 12
            track.add(brass_note(m + up - 12, beats * BEAT * 0.97, 1.0, bright=1.15), t, 0.22, pan=0.1)
            if b >= 8:
                track.add(brass_note(m - 12, beats * BEAT * 0.97, 0.8, bright=0.7), t, 0.12, pan=-0.15)
            t += beats * BEAT


def render_stem(fn):
    """Two loop cycles, keeping the second, so reverb tails wrap around the loop point."""
    track = Track(LOOP * 2)
    fn(track, 0.0)
    fn(track, LOOP)
    l, r = reverb(track.l, track.r, seconds=3.0, wet=0.34)
    a, b = int(LOOP * SR), int(2 * LOOP * SR)
    return np.stack([l[a:b], r[a:b]], axis=1)


def master(x):
    x = filt(x.T, "highpass", 30).T
    return np.tanh(x * 1.2) / np.tanh(1.2)


def swim_theme():
    base = master(render_stem(render_base))
    peak = master(render_stem(render_peak))
    scale = 0.85 / np.max(np.abs(base + peak))
    for name, stem in (("swim_theme_base", base), ("swim_theme_peak", peak)):
        path = os.path.join(ROOT, "music", name + ".ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sf.write(path, (stem * scale).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        print("wrote", os.path.relpath(path), f"{len(stem) / SR:.2f}s")


# ------------------------------------------------------------------------------------------ sfx

def shark_lunge():
    """The shark gathers itself and shoots forward: a deep surge of water."""
    dur = 0.7
    n = int(dur * SR)
    t = t_of(n)
    surge = filt(noise(n), "bandpass", [120, 1600]) * np.sin(np.pi * np.minimum(t / 0.45, 1.0)) ** 1.5 * np.exp(-np.maximum(t - 0.3, 0) * 6)
    x = surge + whoosh(dur, 150, 2200, True) * 0.5
    place(x, sub_boom(0.6, 0.8), 0.05, 0.6)
    place(x, bubbles(0.5, 80, 300, 1300), 0.1, 0.4)
    save("shark_lunge", fade(x, 0.003, 0.1), 0.9)


def manta_ambient():
    """Deep, soft, gliding hum with the sigh of the wings."""
    dur = 2.2
    n = int(dur * SR)
    t = t_of(n)
    x = whale(95, 120, dur) * 0.8 + whale(190, 150, dur) * 0.3
    x += filt(noise(n), "bandpass", [200, 900]) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.9 * t)) * 0.25
    save("manta_ambient", fade(x, 0.05, 0.3), 0.75)


def manta_flap():
    dur = 0.9
    n = int(dur * SR)
    t = t_of(n)
    x = filt(noise(n), "lowpass", 700) * np.sin(np.pi * t / dur) ** 2
    place(x, bubbles(0.6, 40, 300, 900), 0.2, 0.3)
    save("manta_flap", fade(x, 0.01, 0.1), 0.6)


def manta_hurt():
    dur = 0.7
    x = whale(220, 140, dur) + filt(noise(int(dur * SR)), "bandpass", [300, 2000]) * np.exp(-t_of(int(dur * SR)) * 8) * 0.5
    save("manta_hurt", fade(x, 0.003, 0.1), 0.8)


def seahorse_ambient():
    """Seahorses click: a few dry, woody clicks with a tiny whistle."""
    dur = 1.0
    n = int(dur * SR)
    x = np.zeros(n)
    for i in range(int(rng.integers(3, 6))):
        k = int(0.03 * SR)
        tt = t_of(k)
        click = filt(noise(k), "bandpass", [1800, 5000]) * np.exp(-tt * 300)
        place(x, click, 0.05 + i * rng.uniform(0.09, 0.16), rng.uniform(0.6, 1.0))
    k = int(0.35 * SR)
    tt = t_of(k)
    whistle = np.sin(2 * np.pi * np.cumsum(1400 + 500 * tt / 0.35) / SR) * np.sin(np.pi * tt / 0.35) ** 2
    place(x, whistle, 0.55, 0.25)
    place(x, bubbles(0.5, 20, 600, 1600), 0.3, 0.2)
    save("seahorse_ambient", fade(x, 0.002, 0.05), 0.7)


def seahorse_hurt():
    dur = 0.45
    n = int(dur * SR)
    t = t_of(n)
    x = np.sin(2 * np.pi * np.cumsum(1300 - 700 * t / dur) / SR) * np.exp(-t * 6)
    x += filt(noise(n), "bandpass", [1500, 6000]) * np.exp(-t * 30) * 0.6
    save("seahorse_hurt", fade(x, 0.002, 0.05), 0.8)


def seahorse_dash():
    """The seahorse kicks its tail: a quick swirl upwards."""
    dur = 0.6
    n = int(dur * SR)
    x = whoosh(dur, 300, 3000, True) * 0.7
    place(x, bubbles(0.5, 90, 500, 2000), 0.0, 0.5)
    place(x, shimmer(0.5, 84, (0, 7, 12)), 0.1, 0.25)
    save("seahorse_dash", fade(x, 0.002, 0.08), 0.8)


def dolphin_whistle(f0, f1, dur, wobble=1.0):
    n = int(dur * SR)
    t = t_of(n)
    curve = f0 + (f1 - f0) * (0.5 - 0.5 * np.cos(np.pi * t / dur))
    f = curve * (1 + 0.04 * wobble * np.sin(2 * np.pi * 9 * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return (np.sin(ph) + 0.25 * np.sin(2 * ph)) * np.sin(np.pi * t / dur) ** 1.2


def atlantean_dolphin_ambient():
    """Whistles and clicks, with a glassy shimmer: dolphins that glow."""
    dur = 1.4
    n = int(dur * SR)
    x = np.zeros(n)
    place(x, dolphin_whistle(2200, 3600, 0.4), 0.0, 0.6)
    place(x, dolphin_whistle(3400, 2400, 0.35, 2.0), 0.45, 0.5)
    for i in range(10):
        k = int(0.01 * SR)
        place(x, filt(noise(k), "highpass", 3000) * np.exp(-t_of(k) * 500), 0.85 + i * 0.035, 0.6)
    place(x, shimmer(0.9, 88, (0, 4, 7, 11)), 0.3, 0.15)
    save("atlantean_dolphin_ambient", fade(x, 0.002, 0.08), 0.7)


def atlantean_dolphin_hurt():
    x = dolphin_whistle(3000, 1600, 0.4, 3.0)
    save("atlantean_dolphin_hurt", fade(x, 0.002, 0.05), 0.8)


def atlantean_dolphin_leap():
    dur = 0.9
    x = splash(dur, 6000) * 0.8
    place(x, dolphin_whistle(2400, 3800, 0.3), 0.05, 0.35)
    place(x, shimmer(0.7, 86, (0, 7, 12, 16)), 0.1, 0.3)
    save("atlantean_dolphin_leap", fade(x, 0.002, 0.1), 0.85)


def mount_saddle():
    """Climbing onto an Atlantean mount: a soft chime and a swirl of bubbles."""
    dur = 0.9
    x = bubbles(0.7, 60, 400, 1600) * 0.6
    x = np.pad(x, (0, int(dur * SR) - len(x)))
    place(x, shimmer(0.8, 76, (0, 7, 12, 19)), 0.05, 0.45)
    save("atlantean_mount_saddle", fade(x, 0.002, 0.1), 0.75)


def main():
    swim_theme()
    shark_lunge()
    manta_ambient()
    manta_flap()
    manta_hurt()
    seahorse_ambient()
    seahorse_hurt()
    seahorse_dash()
    atlantean_dolphin_ambient()
    atlantean_dolphin_hurt()
    atlantean_dolphin_leap()
    mount_saddle()


if __name__ == "__main__":
    main()
