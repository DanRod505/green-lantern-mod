#!/usr/bin/env python3
"""
Audio of Wonder Woman (DC Universe expansion): the armor, the Lasso of Truth, the Bracelets of
Submission, the Amazon sword and shield, the Invisible Jet and her ORIGINAL flight theme.

Everything is synthesized from scratch (no samples, no third-party music). The theme is written for
this mod (a driving warrior's march in E minor, built on its own riff and melody) and is rendered as
two aligned stereo stems that loop seamlessly, like the other themes:

* wonder_woman_theme_base.ogg - the bowed low-string riff (overdriven), war drums and a dark pad
* wonder_woman_theme_peak.ogg - the horn melody, choir, cymbals and snare

The game plays the base stem while Wonder Woman flies fast and fades the peak stem in near her top
speed (she never breaks the sound barrier).

Usage:  pip install numpy scipy soundfile
        python tools/generate_wonder_woman_audio.py
"""
import os

import numpy as np
import soundfile as sf

from generate_flight_audio import (ROOT, SR, Track, brass_note, choir_note, crash, fade, filt, midi_hz, reverb, save,
                                   snare, strings_note, sub_boom, swell_cymbal, timpani, tom)

rng = np.random.default_rng(1941)


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


def metal(n, base=520, decay=3.0, partials=(1.0, 2.76, 5.4, 8.93, 13.3)):
    """A struck piece of metal (a bracelet, the shield): inharmonic partials that ring and fade."""
    t = t_of(n)
    x = np.zeros(n)
    for k, ratio in enumerate(partials):
        x += np.sin(2 * np.pi * base * ratio * t + k) * np.exp(-t * decay * (1 + k * 0.7)) / (1 + k * 0.5)
    return x


def golden(n, base=880):
    """A warm, bright chord with a slow shimmer: the sound of divine light (the lasso, the tiara)."""
    t = t_of(n)
    x = np.zeros(n)
    for k, ratio in enumerate((1.0, 1.25, 1.5, 2.0, 3.0)):
        x += np.sin(2 * np.pi * base * ratio * t + k * 0.7) / (1 + k * 0.5) * (0.85 + 0.15 * np.sin(2 * np.pi * (4 + k) * t))
    return x


def thunder(dur):
    n = int(dur * SR)
    t = t_of(n)
    crack = filt(noise(n), "highpass", 1500) * np.exp(-t * 25)
    roll = filt(noise(n), "lowpass", 260) * np.exp(-t * 1.6) * 2.0 * (1 + 0.5 * np.sin(2 * np.pi * 3.5 * t))
    return crack * 1.2 + roll


# ------------------------------------------------------------------------------------------ sfx

def wonder_woman_suit_up():
    """A bolt from Olympus: a thunderclap, a golden chord and a short horn call."""
    dur = 2.0
    n = int(dur * SR)
    t = t_of(n)
    x = thunder(dur) * 0.7
    x += golden(n, 440) * np.exp(-((t - 0.55) / 0.4) ** 2) * 0.3
    horn = np.zeros(n)
    for i, (m, d) in enumerate(((64, 0.18), (71, 0.18), (76, 0.7))):
        start = int((0.35 + i * 0.18) * SR)
        note = brass_note(m, d, 1.0, bright=1.1)
        k = min(len(note), n - start)
        horn[start:start + k] += note[:k]
    x += horn * 0.5
    x[: int(0.9 * SR)] += sub_boom(0.9, 1.0)[: int(0.9 * SR)] * 0.6
    save("wonder_woman_suit_up", fade(x, 0.001, 0.25), 0.9)


def wonder_woman_suit_down():
    dur = 1.1
    n = int(dur * SR)
    t = t_of(n)
    x = whoosh(dur, 200, 5000, False) * 0.7 + golden(n, 660) * np.exp(-t * 4) * 0.2
    save("wonder_woman_suit_down", fade(x, 0.002, 0.15), 0.8)


def lasso_throw():
    """The rope whistles through the air and cracks like a whip."""
    dur = 0.7
    n = int(dur * SR)
    t = t_of(n)
    swish = whoosh(0.45, 500, 6000, True)
    x = np.zeros(n)
    x[: len(swish)] += swish * 0.8
    i = int(0.4 * SR)
    crack = filt(noise(int(0.08 * SR)), "highpass", 2000) * np.exp(-t_of(int(0.08 * SR)) * 60)
    x[i:i + len(crack)] += crack * 1.4
    x += golden(n, 1320) * np.exp(-((t - 0.3) / 0.15) ** 2) * 0.08
    save("lasso_throw", fade(x, 0.001, 0.1), 0.8)


def lasso_capture():
    """The loop pulls tight (a creak of rope) and glows: a golden chime."""
    dur = 1.4
    n = int(dur * SR)
    t = t_of(n)
    creak = np.zeros(n)
    for k in range(5):
        start = int((0.02 + k * 0.045) * SR)
        g = filt(noise(int(0.04 * SR)), "bandpass", [300 + k * 60, 900 + k * 80]) * np.exp(-t_of(int(0.04 * SR)) * 50)
        creak[start:start + len(g)] += g
    chime = golden(n, 784) * np.exp(-t * 2.2) * np.minimum(1, t / 0.02)
    x = creak * 0.9 + chime * 0.35
    save("lasso_capture", fade(x, 0.001, 0.2), 0.8)


def lasso_pull():
    """A hard tug on the rope: a snap and a rushing whoosh."""
    dur = 0.8
    n = int(dur * SR)
    t = t_of(n)
    snap = filt(noise(n), "bandpass", [400, 3000]) * np.exp(-t * 35)
    x = snap * 1.2 + whoosh(dur, 200, 3000, False) * 0.7 + chirp(260, 120, dur, 0.6) * np.exp(-t * 6) * 0.2
    save("lasso_pull", fade(x, 0.0005, 0.1), 0.85)


def lasso_spin():
    """The lasso whirling around: rhythmic swishes speeding up, with a faint golden hum."""
    dur = 1.3
    n = int(dur * SR)
    t = t_of(n)
    x = np.zeros(n)
    pos = 0.0
    period = 0.24
    while pos < dur - 0.1:
        s = whoosh(min(0.2, period), 400, 5000, True)
        i = int(pos * SR)
        k = min(len(s), n - i)
        x[i:i + k] += s[:k] * 0.7
        pos += period
        period = max(0.12, period * 0.88)
    x += golden(n, 523) * np.sin(np.pi * t / dur) * 0.06
    save("lasso_spin", fade(x, 0.002, 0.1), 0.8)


def bracelet_guard():
    """The bracelets come up: a ringing metallic shimmer rising in pitch."""
    dur = 1.0
    n = int(dur * SR)
    t = t_of(n)
    x = metal(n, 660, 2.5) * 0.4 + chirp(600, 1500, dur, 0.5) * np.exp(-t * 3) * 0.12
    x += whoosh(0.4, 400, 4000, True)[: n] if n <= int(0.4 * SR) else np.pad(whoosh(0.4, 400, 4000, True), (0, n - int(0.4 * SR))) * 0.4
    save("bracelet_guard", fade(x, 0.001, 0.15), 0.75)


def bracelet_deflect():
    """A projectile glances off silver: a bright 'ping' with a spark of noise."""
    dur = 0.9
    n = int(dur * SR)
    t = t_of(n)
    x = metal(n, 1180, 4.0) * 0.6 + filt(noise(n), "highpass", 3000) * np.exp(-t * 60) * 0.8
    save("bracelet_deflect", fade(x, 0.0005, 0.15), 0.85)


def bracelet_shockwave():
    """The bracelets clash: a huge metallic clang, a boom and a ringing wave of force."""
    dur = 2.8
    n = int(dur * SR)
    t = t_of(n)
    clang = metal(n, 330, 1.2) * 0.7 + metal(n, 497, 1.6) * 0.5
    crack = filt(noise(n), "highpass", 1500) * np.exp(-t * 30)
    boom = sub_boom(dur, 1.0)
    wave = whoosh(dur, 120, 2500, False) * 0.6
    ring = golden(n, 220) * np.exp(-t * 1.2) * 0.12
    x = clang + crack * 1.2 + boom * 1.4 + wave + ring
    save("bracelet_shockwave", fade(x, 0.0005, 0.4), 0.97)


def amazon_sword_swing():
    """A blade cutting the air, with a faint ring of steel."""
    dur = 0.45
    n = int(dur * SR)
    t = t_of(n)
    x = whoosh(dur, 800, 9000, True) * 0.8 + metal(n, 1760, 7.0) * 0.12 * np.minimum(1, t / 0.15)
    save("amazon_sword_swing", fade(x, 0.002, 0.08), 0.8)


def shield_throw():
    """The shield spins off like a discus: a whirring hum that fades away."""
    dur = 0.9
    n = int(dur * SR)
    t = t_of(n)
    whirr = filt(noise(n), "bandpass", [300, 1800]) * (0.6 + 0.4 * np.sin(2 * np.pi * 28 * t))
    x = whirr * np.exp(-t * 2.5) * 0.9 + whoosh(dur, 300, 4000, False) * 0.6
    save("shield_throw", fade(x, 0.002, 0.15), 0.85)


def shield_hit():
    """The shield slams into something: a heavy metal clang and a thud."""
    dur = 1.0
    n = int(dur * SR)
    t = t_of(n)
    x = metal(n, 240, 3.0) * 0.6 + filt(noise(n), "lowpass", 400) * np.exp(-t * 18) * 1.4
    x += np.sin(2 * np.pi * np.cumsum(80 * (1 + 2 * np.exp(-t * 30))) / SR) * np.exp(-t * 8) * 0.6
    save("shield_hit", fade(x, 0.0005, 0.15), 0.9)


def shield_return():
    """Back on the arm: a whoosh coming in and a firm metal clink."""
    dur = 0.7
    n = int(dur * SR)
    t = t_of(n)
    x = whoosh(0.5, 300, 4000, True)
    x = np.pad(x, (0, n - len(x)))
    i = int(0.45 * SR)
    clink = metal(n - i, 880, 6.0)
    x[i:] += clink * 0.5
    save("shield_return", fade(x * 0.9, 0.002, 0.1), 0.8)


def jet_roar(n, pitch=1.0):
    t = t_of(n)
    whine = np.sin(2 * np.pi * np.cumsum(np.full(n, 1650.0 * pitch)) / SR) * 0.05
    roar = filt(noise(n), "bandpass", [80, 900]) * 1.2 + filt(noise(n), "bandpass", [1500, 5000]) * 0.4
    return roar + whine * (1 + 0.1 * np.sin(2 * np.pi * 3 * t))


def invisible_jet_summon():
    """The jet swoops down from the sky: a rising roar that swells and settles into a hum."""
    dur = 2.6
    n = int(dur * SR)
    t = t_of(n)
    env = np.sin(np.pi * np.clip(t / 1.8, 0, 1)) ** 1.5 * 0.9 + 0.25 * (t > 1.2)
    x = jet_roar(n) * env + golden(n, 587) * np.exp(-((t - 1.5) / 0.4) ** 2) * 0.12
    x += chirp(300, 900, dur, 0.6) * np.sin(np.pi * t / dur) * 0.05
    save("invisible_jet_summon", fade(x, 0.05, 0.4), 0.85)


def invisible_jet_engine():
    """A smooth jet hum (about 1.5 s, replayed while it flies; the pitch follows the speed)."""
    dur = 1.5
    n = int(dur * SR)
    t = t_of(n)
    env = np.minimum(1, t / 0.15) * np.minimum(1, (dur - t) / 0.15)
    save("invisible_jet_engine", fade(jet_roar(n) * env * 0.8, 0.01, 0.05), 0.6)


def invisible_jet_cloak():
    """The jet fades from sight: a glassy downward shimmer."""
    dur = 1.2
    n = int(dur * SR)
    t = t_of(n)
    x = chirp(2600, 500, dur, 0.6) * np.exp(-t * 2.2) * 0.2 + golden(n, 1046) * np.exp(-t * 3) * 0.15
    x += filt(noise(n), "highpass", 5000) * np.sin(np.pi * t / dur) * 0.15
    save("invisible_jet_cloak", fade(x, 0.002, 0.15), 0.7)


# ------------------------------------------------------------------------------------- the theme
# A warrior's march of our own: E minor, 132 BPM. The base stem is a driving low-string riff in
# 3+3+2 accents over war drums; the peak stem adds a soaring horn melody that climbs from the fifth
# to the octave, over a progression that turns to the major mode at the end (Em - C - D - Em,
# Am - C - Bsus - B, then G - D - C - B).

BPM = 132
BEAT = 60.0 / BPM
BAR = 4 * BEAT
BARS = 16
LOOP = BARS * BAR

CHORDS = {
    "Em": (40, [52, 55, 59]), "C": (36, [48, 52, 55]), "D": (38, [50, 54, 57]), "Am": (45, [45, 48, 52]),
    "G": (43, [55, 59, 62]), "B": (47, [47, 51, 54]), "Bsus": (47, [47, 52, 54]),
}
PROGRESSION = ["Em", "C", "D", "Em", "Am", "C", "Bsus", "B", "Em", "C", "D", "Em", "G", "D", "C", "B"]

# Horn melody (note, beats), one entry per bar.
MELODY = [
    [(71, 1.5), (74, 0.5), (76, 2)],
    [(79, 1), (76, 1), (72, 2)],
    [(74, 1.5), (78, 0.5), (81, 1), (78, 1)],
    [(76, 4)],
    [(76, 1), (79, 1), (81, 1.5), (84, 0.5)],
    [(83, 2), (79, 2)],
    [(78, 1), (76, 1), (75, 2)],
    [(75, 3), (71, 1)],
    [(71, 1.5), (74, 0.5), (76, 2)],
    [(79, 1), (81, 1), (83, 2)],
    [(81, 1.5), (78, 0.5), (74, 1), (78, 1)],
    [(76, 3), (79, 1)],
    [(83, 1.5), (81, 0.5), (79, 1), (74, 1)],
    [(78, 2), (81, 2)],
    [(79, 1), (76, 1), (72, 1), (76, 1)],
    [(75, 2), (78, 1), (83, 1)],
]

# The riff: 8 eighth notes per bar; the accents fall 3+3+2.
RIFF = [0, 0, 12, 0, 7, 0, 10, 12]
ACCENTS = [1.0, 0.55, 0.6, 1.0, 0.55, 0.6, 1.0, 0.7]


def overdrive(x, drive=2.2):
    return np.tanh(x * drive) / np.tanh(drive)


def render_base(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # The low-string riff, bowed hard and overdriven (a gritty, electric edge).
        for i, iv in enumerate(RIFF):
            note = overdrive(strings_note(root + 12 + iv, BEAT * 0.48, ACCENTS[i], spiccato=True))
            track.add(note, t0 + i * BEAT * 0.5, 0.3, pan=-0.25)
            track.add(strings_note(root + iv, BEAT * 0.48, ACCENTS[i] * 0.8, spiccato=True), t0 + i * BEAT * 0.5, 0.22, pan=0.25)
        # Dark pad.
        for j, m in enumerate(triad):
            track.add(strings_note(m, BAR * 1.05), t0, 0.08, pan=(j - 1) * 0.6)
        # War drums: big toms on the accents, timpani on the downbeat.
        for i, acc in enumerate(ACCENTS):
            if acc >= 1.0:
                track.add(tom(40, 1.0), t0 + i * BEAT * 0.5, 0.3, pan=-0.2)
            elif i % 2 == 1:
                track.add(tom(45, 0.5), t0 + i * BEAT * 0.5, 0.16, pan=0.2)
        track.add(timpani(root + 12 if root < 40 else root, 1.0, 0.9), t0, 0.35)
        if b % 4 == 3:
            for s in range(4):
                track.add(tom(43 + s, 0.5 + 0.12 * s), t0 + (3 + s * 0.25) * BEAT, 0.2)


def render_peak(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        for j, m in enumerate(triad):
            track.add(choir_note(m + 12, BAR * 1.05), t0, 0.09, pan=(1 - j) * 0.5)
        track.add(sub_boom(0.8, 0.7), t0, 0.3)
        track.add(snare(0.9), t0 + BEAT, 0.25)
        track.add(snare(0.9), t0 + 3 * BEAT, 0.25)
        track.add(snare(0.5), t0 + 3.5 * BEAT, 0.15)
        if b % 4 == 0:
            track.add(crash(2.2), t0, 0.28)
        if b % 8 == 7:
            track.add(swell_cymbal(BAR), t0, 0.2)
        # The melody on horns, doubled an octave below.
        t = t0
        for m, beats in MELODY[b]:
            track.add(brass_note(m - 12, beats * BEAT * 0.96, 1.0, bright=1.25), t, 0.22, pan=0.1)
            track.add(brass_note(m - 24, beats * BEAT * 0.96, 0.8, bright=0.9), t, 0.1, pan=-0.15)
            t += beats * BEAT


def render_stem(fn):
    track = Track(LOOP * 2)
    fn(track, 0.0)
    fn(track, LOOP)
    l, r = reverb(track.l, track.r, seconds=2.0, wet=0.26)
    a, b = int(LOOP * SR), int(2 * LOOP * SR)
    return np.stack([l[a:b], r[a:b]], axis=1)


def master(x):
    x = filt(x.T, "highpass", 30).T
    return np.tanh(x * 1.3) / np.tanh(1.3)


def wonder_woman_theme():
    base = master(render_stem(render_base))
    peak = master(render_stem(render_peak))
    scale = 0.85 / np.max(np.abs(base + peak))
    for name, stem in (("wonder_woman_theme_base", base), ("wonder_woman_theme_peak", peak)):
        path = os.path.join(ROOT, "music", name + ".ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sf.write(path, (stem * scale).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        print("wrote", os.path.relpath(path), f"{len(stem) / SR:.2f}s")


def main():
    wonder_woman_suit_up()
    wonder_woman_suit_down()
    lasso_throw()
    lasso_capture()
    lasso_pull()
    lasso_spin()
    bracelet_guard()
    bracelet_deflect()
    bracelet_shockwave()
    amazon_sword_swing()
    shield_throw()
    shield_hit()
    shield_return()
    invisible_jet_summon()
    invisible_jet_engine()
    invisible_jet_cloak()
    wonder_woman_theme()


if __name__ == "__main__":
    main()
