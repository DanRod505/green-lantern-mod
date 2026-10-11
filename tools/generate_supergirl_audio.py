#!/usr/bin/env python3
"""
Audio of Supergirl (DC Universe expansion): the suit, the heat bolts, the meteor dash, the thunder
clap, the frost breath, the super hearing, the Kryptonian throw, the solar flare, the barrel roll,
the full pendant, Krypto (bark, hurt, whine, flight and her whistle) and her ORIGINAL theme.

Everything is synthesized from scratch here: oscillators, filtered noise and envelopes only. No
samples, no recordings and no third-party music or melodies. The theme is written for this mod (a
quick, bright pop-orchestral piece in D major at 138 BPM, younger and lighter than Superman's
B-flat anthem, built on its own melody) and is rendered as two aligned stereo stems that loop
seamlessly, like the other themes:

* supergirl_theme_base.ogg - driving 16th-note strings, a plucked synth riff, bass and a pop beat
* supergirl_theme_peak.ogg - the horn and trumpet melody, glockenspiel sparkle, choir and cymbals

The sound effects are short mono files (so the game can place them in the world).

Usage:  pip install numpy scipy soundfile
        python tools/generate_supergirl_audio.py
"""
import os

import numpy as np
import soundfile as sf

from generate_flight_audio import (ROOT, SR, Track, brass_note, choir_note, crash, fade, filt, midi_hz, reverb, save,
                                   snare, strings_note, sub_boom, swell_cymbal, timpani)

rng = np.random.default_rng(1959)


# ----------------------------------------------------------------------------------------- utils

def noise(n):
    return rng.uniform(-1, 1, n)


def t_of(n):
    return np.arange(n) / SR


def ns(dur):
    return int(dur * SR)


def sine(freq, n, ph0=0.0):
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    return np.sin(2 * np.pi * (np.cumsum(f) / SR + ph0))


def saw(freq, n, ph0=0.0):
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    p = (np.cumsum(f) / SR + ph0) % 1.0
    return 2 * p - 1


def expsweep(f0, f1, n, curve=1.0):
    return f0 * (f1 / f0) ** (np.linspace(0, 1, n, endpoint=False) ** curve)


def place(x, y, at, gain=1.0):
    i = int(at * SR)
    if i >= len(x):
        return
    k = min(len(y), len(x) - i)
    x[i:i + k] += y[:k] * gain


def mono_reverb(x, seconds=1.5, wet=0.3):
    l, r = reverb(x, x.copy(), seconds=seconds, wet=wet)
    return (l + r) * 0.5


def whoosh(dur, lo=300, hi=4000, rise=True):
    n = ns(dur)
    t = t_of(n)
    env = np.sin(np.pi * t / dur) ** 1.5
    x = np.zeros(n)
    steps = 24
    seg = n // steps
    for s in range(steps):
        a = s / steps if rise else 1 - s / steps
        c = lo * (hi / lo) ** a
        part = filt(noise(seg + 400), "bandpass", [c * 0.6, min(c * 1.6, SR / 2 - 100)])[200:200 + seg]
        x[s * seg:s * seg + len(part)] = part
    return x * env


def bell(f, dur, vel=1.0):
    """Glockenspiel: bright inharmonic partials that ring and fade."""
    n = ns(dur)
    t = t_of(n)
    x = np.zeros(n)
    for ratio, g, d in ((1.0, 1.0, 3.0), (2.76, 0.4, 6), (5.4, 0.2, 10), (8.9, 0.08, 16)):
        x += np.sin(2 * np.pi * f * ratio * t) * g * np.exp(-t * d)
    return vel * x * np.minimum(1, t / 0.002)


def sizzle(n, lo=2500, hi=9000):
    """Crackling heat: sparse random pops on a bed of hiss."""
    pops = (rng.uniform(0, 1, n) < 0.03) * noise(n) * 3
    return filt(pops, "bandpass", [lo, hi]) + filt(noise(n), "bandpass", [lo, hi]) * 0.25


def sparkle(dur, base=1500, count=18):
    """Small golden chimes scattered in time (the House of El's sunlight)."""
    n = ns(dur)
    x = np.zeros(n)
    for at in np.sort(rng.uniform(0, dur * 0.8, count)):
        f = base * 2 ** rng.choice([0, 4 / 12, 7 / 12, 1, 16 / 12])
        place(x, bell(f, 0.5, rng.uniform(0.3, 0.8)), at)
    return x


# ------------------------------------------------------------------------------------------ sfx

def supergirl_suit_up():
    dur = 2.0
    n = ns(dur)
    t = t_of(n)
    x = np.zeros(n)
    # Cape snap and a rising rush of air.
    place(x, whoosh(0.9, 400, 5000, True) * 0.6, 0.0)
    cn = ns(0.12)
    place(x, filt(noise(cn), "bandpass", [800, 5000]) * np.exp(-t_of(cn) * 50) * 1.2, 0.85)
    # A warm, rising D major chord swelling with sunlight.
    for m, d in ((62, 0.0), (66, 0.06), (69, 0.12), (74, 0.18)):
        cn = n - ns(0.4 + d)
        ct = t_of(cn)
        tone = sum(np.sin(2 * np.pi * midi_hz(m) * k * ct) / k ** 1.4 for k in (1, 2, 3, 4))
        place(x, tone * np.minimum(1, ct / 0.5) * np.exp(-ct * 1.3) * 0.18, 0.4 + d)
    place(x, sparkle(1.4, 1760, 14) * 0.18, 0.55)
    place(x, sub_boom(0.8, 0.6) * 0.5, 0.88)
    save("supergirl_suit_up", fade(mono_reverb(x, 1.6, 0.3), 0.003, 0.2), 0.9)


def supergirl_suit_down():
    dur = 1.3
    n = ns(dur)
    x = np.zeros(n)
    place(x, whoosh(0.8, 3500, 300, False) * 0.5, 0.0)
    for i, m in enumerate((74, 69, 66)):
        cn = ns(0.6)
        ct = t_of(cn)
        place(x, np.sin(2 * np.pi * midi_hz(m) * ct) * np.exp(-ct * 5) * 0.25, 0.1 + i * 0.12)
    save("supergirl_suit_down", fade(mono_reverb(x, 1.0, 0.25), 0.003, 0.15), 0.75)


def supergirl_heat_bolt():
    """A short, bright 'pew' of heat: two quick bolts with a hot crackle."""
    dur = 0.55
    n = ns(dur)
    x = np.zeros(n)
    for at in (0.0, 0.07):
        bn = ns(0.35)
        bt = t_of(bn)
        tone = sine(expsweep(2400, 500, bn, 0.6), bn) + 0.4 * saw(expsweep(1200, 250, bn, 0.6), bn)
        tone = filt(tone, "lowpass", 5000) * np.exp(-bt * 10)
        place(x, tone * 0.5 + sizzle(bn) * np.exp(-bt * 8) * 0.35, at)
    save("supergirl_heat_bolt", fade(x, 0.001, 0.05), 0.8)


def supergirl_heat_hit():
    dur = 0.6
    n = ns(dur)
    t = t_of(n)
    thump = sine(140 * (1 + 2 * np.exp(-t * 30)), n) * np.exp(-t * 14)
    hiss = sizzle(n, 1500, 8000) * np.exp(-t * 6)
    save("supergirl_heat_hit", fade(thump * 0.8 + hiss * 0.6, 0.001, 0.1), 0.75)


def supergirl_meteor_dash():
    """A rushing streak through the air that ends in a fiery boom."""
    dur = 1.1
    n = ns(dur)
    x = np.zeros(n)
    place(x, whoosh(0.6, 250, 6000, True) * 1.1, 0.0)
    rn = ns(0.6)
    rt = t_of(rn)
    roar = filt(noise(rn), "lowpass", 900) * np.sin(np.pi * rt / 0.6) * 0.7
    place(x, roar, 0.0)
    bn = ns(0.6)
    bt = t_of(bn)
    place(x, sub_boom(0.6, 1.0) * 0.9 + filt(noise(bn), "bandpass", [200, 3000]) * np.exp(-bt * 12) * 0.6, 0.48)
    save("supergirl_meteor_dash", fade(mono_reverb(x, 0.9, 0.2), 0.002, 0.15), 0.92)


def supergirl_thunder_clap():
    """Two hands meeting: a sharp crack, a deep pressure wave and a rolling rumble."""
    dur = 2.0
    n = ns(dur)
    t = t_of(n)
    crack = filt(noise(n), "highpass", 1200) * np.exp(-t * 60) * 1.6
    wave = sine(38 * (1 + 2.5 * np.exp(-t * 12)), n) * np.exp(-t * 2.4) * 1.3
    rumble = filt(noise(n), "lowpass", 300) * np.exp(-t * 1.6) * (0.7 + 0.3 * np.sin(2 * np.pi * 7 * t))
    ring = sine(expsweep(900, 300, n), n) * np.exp(-t * 5) * 0.15
    x = np.tanh((crack + wave + rumble * 0.9 + ring) * 1.2)
    save("supergirl_thunder_clap", fade(mono_reverb(x, 2.2, 0.4), 0.0005, 0.3), 0.97)


def supergirl_frost_breath():
    """An icy exhale with crystals forming and cracking."""
    dur = 1.8
    n = ns(dur)
    t = t_of(n)
    env = np.minimum(1, t / 0.15) * np.minimum(1, (dur - t) / 0.5)
    breath = filt(noise(n), "bandpass", [1200, 7000]) * env * 0.7
    x = breath.copy()
    for at in np.sort(rng.uniform(0.2, dur - 0.2, 26)):
        cn = ns(0.08)
        ct = t_of(cn)
        f = rng.uniform(2500, 6000)
        place(x, np.sin(2 * np.pi * f * ct) * np.exp(-ct * 70) * rng.uniform(0.2, 0.5), at)
    for at in (0.6, 1.0, 1.35):
        cn = ns(0.1)
        place(x, filt(noise(cn), "highpass", 2500) * np.exp(-t_of(cn) * 60) * 0.7, at)
    save("supergirl_frost_breath", fade(mono_reverb(x, 1.2, 0.25), 0.01, 0.2), 0.75)


def supergirl_frost_melt():
    dur = 1.4
    n = ns(dur)
    x = np.zeros(n)
    for at in np.sort(rng.uniform(0, dur - 0.2, 14)):
        dn = ns(0.12)
        dt = t_of(dn)
        f = rng.uniform(700, 1500)
        place(x, sine(f * (1 + 0.8 * dt / 0.12), dn) * np.exp(-dt * 40) * rng.uniform(0.2, 0.5), at)
    place(x, filt(noise(n), "bandpass", [600, 3000]) * 0.08, 0)
    save("supergirl_frost_melt", fade(x, 0.01, 0.2), 0.55)


def supergirl_hearing_on():
    """The world opens up: a soft rising sweep into a high, airy tone."""
    dur = 1.0
    n = ns(dur)
    t = t_of(n)
    sweep = sine(expsweep(300, 1800, n, 0.7), n) * np.sin(np.pi * t / dur) * 0.25
    air = filt(noise(n), "bandpass", [3000, 9000]) * np.minimum(1, t / 0.6) * np.exp(-t * 2) * 0.3
    pad = (sine(1174.7, n) + sine(1760, n) * 0.5) * np.minimum(1, t / 0.4) * np.exp(-t * 3) * 0.15
    save("supergirl_hearing_on", fade(mono_reverb(sweep + air + pad, 1.2, 0.35), 0.01, 0.2), 0.6)


def supergirl_hearing_off():
    dur = 0.6
    n = ns(dur)
    t = t_of(n)
    sweep = sine(expsweep(1800, 300, n, 0.7), n) * np.exp(-t * 4) * 0.25
    save("supergirl_hearing_off", fade(mono_reverb(sweep, 0.6, 0.2), 0.005, 0.1), 0.5)


def supergirl_hearing_pulse():
    """A heartbeat in the distance: two soft, muffled thumps."""
    dur = 0.7
    n = ns(dur)
    x = np.zeros(n)
    for at, v in ((0.0, 1.0), (0.22, 0.7)):
        bn = ns(0.25)
        bt = t_of(bn)
        place(x, sine(55 * (1 + 0.6 * np.exp(-bt * 30)), bn) * np.exp(-bt * 14) * v, at)
    save("supergirl_hearing_pulse", fade(filt(x, "lowpass", 400), 0.002, 0.1), 0.6)


def supergirl_throw_grab():
    dur = 0.5
    n = ns(dur)
    t = t_of(n)
    thud = sine(90 * (1 + 1.2 * np.exp(-t * 25)), n) * np.exp(-t * 12)
    grit = filt(noise(n), "bandpass", [300, 2500]) * np.exp(-t * 20) * 0.6
    creak = sine(expsweep(220, 160, n), n) * np.exp(-t * 6) * 0.15
    save("supergirl_throw_grab", fade(thud + grit + creak, 0.001, 0.08), 0.75)


def supergirl_throw_hurl():
    dur = 0.9
    n = ns(dur)
    x = np.zeros(n)
    place(x, whoosh(0.7, 200, 3000, False) * 1.2, 0.0)
    gn = ns(0.15)
    place(x, filt(noise(gn), "bandpass", [150, 1200]) * np.exp(-t_of(gn) * 25), 0.0, 0.6)
    save("supergirl_throw_hurl", fade(x, 0.002, 0.1), 0.85)


def supergirl_solar_flare():
    """All her sunlight at once: a rising charge, a blinding blast and a long ringing fade."""
    dur = 3.4
    n = ns(dur)
    x = np.zeros(n)
    cn = ns(0.7)
    ct = t_of(cn)
    charge = sum(sine(expsweep(f, f * 4, cn, 1.5), cn) / (i + 1) for i, f in enumerate((220, 277, 330)))
    place(x, charge * (ct / 0.7) ** 2 * 0.3, 0.0)
    place(x, filt(noise(cn), "highpass", 3000) * (ct / 0.7) ** 3 * 0.4, 0.0)
    at = 0.7
    bn = n - ns(at)
    bt = t_of(bn)
    blast = sine(42 * (1 + 3 * np.exp(-bt * 10)), bn) * np.exp(-bt * 1.5) * 1.4
    burst = filt(noise(bn), "bandpass", [150, 6000]) * np.exp(-bt * 3) * 0.9
    glow = sum(sine(midi_hz(m), bn) for m in (74, 78, 81, 86)) * np.exp(-bt * 0.9) * 0.08
    place(x, np.tanh(blast + burst) + glow, at)
    place(x, sparkle(2.4, 2093, 22) * 0.12, at + 0.1)
    save("supergirl_solar_flare", fade(mono_reverb(x, 2.8, 0.45), 0.002, 0.5), 0.98)


def supergirl_roll():
    dur = 0.7
    n = ns(dur)
    t = t_of(n)
    swirl = whoosh(dur, 500, 4500, True) * (1 + 0.6 * np.sin(2 * np.pi * 9 * t))
    cape = filt(noise(n), "bandpass", [600, 2500]) * (np.sin(2 * np.pi * 18 * t) > 0.6) * np.sin(np.pi * t / dur) * 0.4
    save("supergirl_roll", fade(swirl + cape, 0.005, 0.1), 0.7)


def supergirl_solar_full():
    dur = 1.2
    n = ns(dur)
    x = np.zeros(n)
    for i, m in enumerate((74, 78, 81, 86)):
        place(x, bell(midi_hz(m + 12), 0.9, 0.5), i * 0.09)
    save("supergirl_solar_full", fade(mono_reverb(x, 1.2, 0.3), 0.002, 0.15), 0.6)


def bark(f0=420, vel=1.0):
    """One dog bark: a pitched growl with vowel-like formants and a breathy burst, falling at the end."""
    dur = 0.22
    n = ns(dur)
    t = t_of(n)
    f = f0 * (1 + 0.35 * np.exp(-t * 40)) * (1 - 0.25 * t / dur)
    voice = saw(f, n) + 0.5 * saw(f * 1.01, n, 0.3)
    voice = filt(voice, "bandpass", [500, 1300]) * 1.2 + filt(voice, "bandpass", [1800, 2600]) * 0.6
    breath = filt(noise(n), "bandpass", [800, 4000]) * 0.5
    env = np.minimum(1, t / 0.006) * np.exp(-t * 14)
    return vel * np.tanh((voice + breath) * env * 1.5)


def krypto_bark():
    dur = 0.6
    n = ns(dur)
    x = np.zeros(n)
    place(x, bark(440, 1.0), 0.0)
    place(x, bark(400, 0.85), 0.24)
    save("krypto_bark", fade(mono_reverb(x, 0.4, 0.1), 0.001, 0.05), 0.85)


def krypto_hurt():
    dur = 0.45
    n = ns(dur)
    t = t_of(n)
    f = 900 * (1 + 0.3 * np.sin(np.pi * t / dur)) * (1 - 0.3 * t / dur)
    yelp = filt(saw(f, n), "bandpass", [700, 3000]) * np.minimum(1, t / 0.01) * np.exp(-t * 7)
    save("krypto_hurt", fade(yelp, 0.002, 0.08), 0.75)


def krypto_whine():
    dur = 0.9
    n = ns(dur)
    t = t_of(n)
    f = 800 + 300 * np.sin(np.pi * t / dur) + 30 * np.sin(2 * np.pi * 7 * t)
    tone = sine(f, n) + 0.3 * sine(f * 2, n) + filt(noise(n), "bandpass", [1500, 4000]) * 0.05
    save("krypto_whine", fade(tone * np.sin(np.pi * t / dur) ** 0.7 * 0.6, 0.01, 0.1), 0.55)


def krypto_fly():
    """Krypto streaking past: a whoosh with a flapping little cape."""
    dur = 1.0
    n = ns(dur)
    t = t_of(n)
    w = whoosh(dur, 400, 3500, True)
    flap = filt(noise(n), "bandpass", [500, 2000]) * (np.sin(2 * np.pi * 14 * t) > 0.5) * np.sin(np.pi * t / dur) * 0.35
    save("krypto_fly", fade(w + flap, 0.01, 0.15), 0.6)


def krypto_whistle():
    """Her whistle for him: two notes, the second sliding up (a call, not a tune)."""
    dur = 0.9
    n = ns(dur)
    x = np.zeros(n)
    for at, f0, f1, d in ((0.0, 1900, 2100, 0.25), (0.33, 1700, 2600, 0.45)):
        wn = ns(d)
        wt = t_of(wn)
        f = f0 + (f1 - f0) * (wt / d) ** 1.5
        f = f * (1 + 0.006 * np.sin(2 * np.pi * 6 * wt))
        tone = sine(f, wn) + filt(noise(wn), "bandpass", [1500, 3500]) * 0.08
        env = np.minimum(1, wt / 0.03) * np.minimum(1, (d - wt) / 0.05)
        place(x, tone * env * 0.6, at)
    save("krypto_whistle", fade(mono_reverb(x, 0.8, 0.2), 0.005, 0.1), 0.65)


# ------------------------------------------------------------------------------------- the theme
# A bright tune of our own: D major, 138 BPM, 16 bars. The strings drive in 16ths, a plucked synth
# plays a skipping riff, and the horns sing a melody that leaps up a sixth and keeps climbing, with a
# turn to B minor and G in the middle and a big A (dominant) that rolls back home to D.

BPM = 138
BEAT = 60.0 / BPM
BAR = 4 * BEAT
BARS = 16
LOOP = BARS * BAR

CHORDS = {
    "D": (38, [62, 66, 69]), "A": (33, [61, 64, 69]), "Bm": (35, [62, 66, 71]), "G": (31, [62, 67, 71]),
    "Em": (40, [64, 67, 71]), "F#m": (42, [61, 66, 69]), "Asus": (33, [62, 64, 69]),
}
PROGRESSION = ["D", "A", "Bm", "G", "D", "A", "G", "Asus", "Bm", "F#m", "G", "D", "Em", "G", "Asus", "A"]

MELODY = [
    [(69, 1.5), (78, 0.5), (76, 1), (74, 1)],
    [(76, 2), (73, 1), (69, 1)],
    [(71, 1.5), (78, 0.5), (79, 1), (78, 1)],
    [(74, 3), (71, 1)],
    [(69, 1.5), (78, 0.5), (81, 1), (79, 1)],
    [(78, 2), (76, 1), (73, 1)],
    [(74, 1), (76, 1), (79, 1), (83, 1)],
    [(81, 4)],
    [(83, 1.5), (81, 0.5), (78, 1), (74, 1)],
    [(73, 1.5), (76, 0.5), (78, 2)],
    [(79, 1), (81, 1), (83, 1), (86, 1)],
    [(85, 1), (86, 2), (81, 1)],
    [(83, 1.5), (79, 0.5), (76, 1), (79, 1)],
    [(83, 1.5), (86, 0.5), (88, 2)],
    [(86, 2), (85, 1), (83, 1)],
    [(85, 3), (81, 1)],
]


def kick(vel=1.0):
    n = ns(0.4)
    t = t_of(n)
    body = sine(52 * (1 + 2.0 * np.exp(-t * 30)), n) * np.exp(-t * 8)
    click = filt(noise(n), "highpass", 2500) * np.exp(-t * 220) * 0.35
    return vel * np.tanh((body + click) * 1.5)


def clap(vel=1.0):
    n = ns(0.3)
    t = t_of(n)
    x = np.zeros(n)
    for d in (0.0, 0.011, 0.022):
        place(x, filt(noise(ns(0.25)), "bandpass", [900, 5000]) * np.exp(-t_of(ns(0.25)) * 22), d)
    return vel * x


def hat(vel=1.0):
    n = ns(0.05)
    t = t_of(n)
    return vel * filt(noise(n), "highpass", 8000) * np.exp(-t * 80)


def pluck(m, dur, vel=1.0):
    """Bright plucked synth (a filtered saw with a quick decay)."""
    n = ns(dur)
    t = t_of(n)
    f = midi_hz(m)
    osc = saw(f, n) * 0.6 + saw(f * 1.005, n, 0.4) * 0.4
    x = filt(osc, "lowpass", 1200) + filt(osc, "lowpass", 5000) * np.exp(-t * 25)
    return vel * x * np.minimum(1, t / 0.002) * np.exp(-t * 7)


def bass(m, dur, vel=1.0):
    n = ns(dur)
    t = t_of(n)
    f = midi_hz(m)
    x = filt(saw(f, n) + sine(f / 2, n), "lowpass", 600)
    env = np.minimum(1, t / 0.005) * np.minimum(1, (dur - t) / 0.02) * (0.6 + 0.4 * np.exp(-t * 6))
    return vel * x * env


RIFF = [0, 12, 7, 12, 4, 12, 7, 14]  # 8th-note pluck pattern over the chord root


def render_base(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Strings: 16th-note pulse on the triad, accented on the beats.
        for s in range(16):
            m = triad[s % 3 if s % 4 else 0]
            vel = 1.0 if s % 4 == 0 else 0.6
            track.add(strings_note(m, BEAT * 0.25, vel, spiccato=True), t0 + s * BEAT / 4, 0.09,
                      pan=-0.35 if s % 2 else 0.35)
        # Plucked riff.
        for e, iv in enumerate(RIFF):
            track.add(pluck(root + 24 + iv, BEAT * 0.45, 0.9 if e % 2 == 0 else 0.7), t0 + e * BEAT / 2, 0.1,
                      pan=0.25)
        # Bass: root on 1 and the 'and' of 2, fifth on 3.
        for at, iv, d in ((0, 0, 1.4), (1.5, 0, 0.5), (2, 7, 1), (3, 0, 0.9)):
            track.add(bass(root + iv, BEAT * d, 1.0), t0 + at * BEAT, 0.3)
        # Pop beat: kick on 1 and 3 (and the 'and' of 3), clap on 2 and 4, 8th hats.
        for at in (0, 2, 2.5):
            track.add(kick(), t0 + at * BEAT, 0.5)
        for at in (1, 3):
            track.add(clap(), t0 + at * BEAT, 0.22)
            track.add(snare(0.6), t0 + at * BEAT, 0.12)
        for e in range(8):
            track.add(hat(0.9 if e % 2 else 0.6), t0 + e * BEAT / 2, 0.1, pan=0.3)
        if b % 4 == 3:
            for i in range(4):
                track.add(snare(0.4 + 0.15 * i), t0 + (3 + i / 4) * BEAT, 0.12)


def render_peak(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Trumpet sings the melody and a horn doubles it an octave below.
        t = t0
        for m, beats in MELODY[b]:
            d = beats * BEAT * 0.97
            track.add(brass_note(m, d, 1.0, bright=1.2), t, 0.2, pan=-0.15)
            track.add(brass_note(m - 12, d, 0.7, bright=0.8), t, 0.1, pan=0.2)
            t += beats * BEAT
        # Glockenspiel sparkle on the off-beats.
        for e in (1, 3, 5, 7):
            track.add(bell(midi_hz(triad[e // 2 % 3] + 24), 0.6, 0.5), t0 + e * BEAT / 2, 0.07, pan=0.45)
        # Choir pad on the chord.
        for m in triad:
            track.add(choir_note(m, BAR * 0.98, 0.6), t0, 0.05, pan=-0.3)
        # Cymbals and timpani.
        if b % 4 == 0:
            track.add(crash(2.4), t0, 0.28, pan=-0.2)
            track.add(timpani(root + 12, 1.4, 1.0), t0, 0.22)
        if b % 8 == 7:
            track.add(swell_cymbal(BAR), t0, 0.2)
        track.add(sub_boom(0.7, 0.5), t0, 0.2)


def render_stem(fn):
    """Renders two loop cycles and keeps the second one, so the reverb wraps around the loop."""
    track = Track(LOOP * 2)
    fn(track, 0.0)
    fn(track, LOOP)
    l, r = reverb(track.l, track.r, seconds=1.8, wet=0.22)
    a, b = int(LOOP * SR), int(2 * LOOP * SR)
    return np.stack([l[a:b], r[a:b]], axis=1)


def master(x):
    x = filt(x.T, "highpass", 30).T
    return np.tanh(x * 1.3) / np.tanh(1.3)


def supergirl_theme():
    base = master(render_stem(render_base))
    peak = master(render_stem(render_peak))
    scale = 0.85 / np.max(np.abs(base + peak))
    for name, stem in (("supergirl_theme_base", base), ("supergirl_theme_peak", peak)):
        path = os.path.join(ROOT, "music", name + ".ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sf.write(path, (stem * scale).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        print("wrote", os.path.relpath(path), f"{len(stem) / SR:.2f}s")


def main():
    supergirl_suit_up()
    supergirl_suit_down()
    supergirl_heat_bolt()
    supergirl_heat_hit()
    supergirl_meteor_dash()
    supergirl_thunder_clap()
    supergirl_frost_breath()
    supergirl_frost_melt()
    supergirl_hearing_on()
    supergirl_hearing_off()
    supergirl_hearing_pulse()
    supergirl_throw_grab()
    supergirl_throw_hurl()
    supergirl_solar_flare()
    supergirl_roll()
    supergirl_solar_full()
    krypto_bark()
    krypto_hurt()
    krypto_whine()
    krypto_fly()
    krypto_whistle()
    supergirl_theme()


if __name__ == "__main__":
    main()
