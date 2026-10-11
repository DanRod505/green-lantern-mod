#!/usr/bin/env python3
"""
Audio of Cyborg (DC Universe expansion): the suit, the sonic cannon, the shoulder missiles, the tech
scan, the machine hack, the EMP burst, self repair, the battery, the Boom Tube and his ORIGINAL theme.

Everything is synthesized from scratch here: oscillators, filtered noise and envelopes only. No
samples, no recordings and no third-party music or melodies. The theme is written for this mod (a dark,
driving electronic piece in C minor at 116 BPM, built on its own bass line, arpeggio and lead melody)
and is rendered as two aligned stereo stems that loop seamlessly, like the other themes:

* cyborg_theme_base.ogg - deep synth bass pulse, mechanical drum beat (kick, snare, hats, clanks) and
  a dark arpeggio with echoes
* cyborg_theme_peak.ogg - saturated "guitar" power chords, the lead synth melody and big cymbals

The sound effects are short mono files (so the game can place them in the world).

Usage:  pip install numpy scipy soundfile
        python tools/generate_cyborg_audio.py
"""
import os

import numpy as np
import soundfile as sf

from generate_flight_audio import ROOT, SR, Track, crash, fade, filt, midi_hz, reverb, save, snare, sub_boom, swell_cymbal

rng = np.random.default_rng(1980)


# ----------------------------------------------------------------------------------------- utils

def noise(n):
    return rng.uniform(-1, 1, n)


def t_of(n):
    return np.arange(n) / SR


def ns(dur):
    return int(dur * SR)


def phase_of(freq, n):
    """Phase in cycles (0..1 wrapping) of an oscillator whose frequency may be an array."""
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    return np.cumsum(f) / SR, f / SR


def _blep(p, dt):
    out = np.zeros_like(p)
    a = p < dt
    x = p[a] / dt[a]
    out[a] = x + x - x * x - 1
    b = p > 1 - dt
    x = (p[b] - 1) / dt[b]
    out[b] = x * x + x + x + 1
    return out


def saw(freq, n, ph0=0.0):
    """PolyBLEP (alias-reduced) sawtooth."""
    ph, dt = phase_of(freq, n)
    p = (ph + ph0) % 1.0
    return 2 * p - 1 - _blep(p, dt)


def square(freq, n, ph0=0.0):
    ph, dt = phase_of(freq, n)
    p1 = (ph + ph0) % 1.0
    p2 = (ph + ph0 + 0.5) % 1.0
    return (2 * p1 - 1 - _blep(p1, dt)) - (2 * p2 - 1 - _blep(p2, dt))


def sine(freq, n, ph0=0.0):
    ph, _ = phase_of(freq, n)
    return np.sin(2 * np.pi * (ph + ph0))


def sweep(f0, f1, n, curve=1.0):
    t = np.linspace(0, 1, n, endpoint=False)
    return f0 + (f1 - f0) * t ** curve


def expsweep(f0, f1, n):
    return f0 * (f1 / f0) ** np.linspace(0, 1, n, endpoint=False)


def place(x, y, at, gain=1.0):
    i = int(at * SR)
    if i >= len(x):
        return
    k = min(len(y), len(x) - i)
    x[i:i + k] += y[:k] * gain


def bitcrush(x, bits=5, hold=6):
    q = 2 ** (bits - 1)
    y = np.round(x * q) / q
    idx = (np.arange(len(y)) // hold) * hold
    return y[idx]


def clank(f0=900, dur=0.25, vel=1.0):
    """A metal plate locking: inharmonic ringing modes plus a sharp click."""
    n = ns(dur)
    t = t_of(n)
    x = np.zeros(n)
    for ratio, g, d in ((1.0, 1.0, 22), (2.76, 0.6, 30), (5.40, 0.4, 42), (8.93, 0.25, 60)):
        x += np.sin(2 * np.pi * f0 * ratio * t + rng.uniform(0, 6)) * g * np.exp(-t * d)
    click = filt(noise(n), "highpass", 2500) * np.exp(-t * 180)
    thud = np.sin(2 * np.pi * 110 * t) * np.exp(-t * 35)
    return vel * (x * 0.5 + click * 0.9 + thud * 0.6)


def servo(freqs, n, grit=0.3):
    """Electric motor whine: a buzzy tone with gear ripple, following a frequency curve."""
    ripple = 1 + 0.015 * np.sin(2 * np.pi * np.cumsum(freqs / 7.0) / SR)
    tone = saw(freqs * ripple, n) * 0.6 + square(freqs * 2.01, n) * 0.25
    tone = filt(tone, "bandpass", [300, 5000])
    hiss = filt(noise(n), "bandpass", [2000, 7000]) * grit
    return tone + hiss * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * np.cumsum(freqs / 3.0) / SR)))


def beep(f, dur, vel=1.0, kind="sine", decay=6.0):
    n = ns(dur)
    t = t_of(n)
    osc = {"sine": sine, "square": square, "saw": saw}[kind](f, n)
    if kind != "sine":
        osc = filt(osc, "lowpass", 5000)
    return vel * osc * np.minimum(1, t / 0.004) * np.exp(-t * decay)


def chime(f, dur):
    """Bright digital bell: a few partials with fast decays on top."""
    n = ns(dur)
    t = t_of(n)
    x = np.zeros(n)
    for ratio, g, d in ((1.0, 1.0, 3.5), (2.0, 0.5, 5), (3.0, 0.3, 7), (4.2, 0.2, 10), (5.0, 0.12, 14)):
        x += np.sin(2 * np.pi * f * ratio * t) * g * np.exp(-t * d)
    return x * np.minimum(1, t / 0.003)


def mono_reverb(x, seconds=1.5, wet=0.3):
    l, r = reverb(x, x.copy(), seconds=seconds, wet=wet)
    return (l + r) * 0.5


def crackle(n, density_curve, lo=1500, hi=9000):
    """Sparse random electric pops; density_curve (0..1 array) is the chance scale."""
    pops = (rng.uniform(0, 1, n) < 0.02 * density_curve) * noise(n) * 3
    return filt(pops, "bandpass", [lo, hi])


# ------------------------------------------------------------------------------------------ sfx

def cyborg_suit_up():
    dur = 2.3
    n = ns(dur)
    t = t_of(n)
    x = np.zeros(n)
    # Servos whirring up while the plates come together.
    sn = ns(1.6)
    sv = servo(sweep(180, 900, sn, 0.8), sn) * np.sin(np.pi * t_of(sn) / 1.6) ** 0.6 * 0.35
    place(x, sv, 0.0)
    # Plates locking piece by piece.
    for at, f0, v in ((0.22, 700, 0.8), (0.42, 820, 0.85), (0.6, 640, 0.9), (0.82, 950, 0.9),
                      (1.0, 760, 1.0), (1.18, 1100, 1.0), (1.36, 880, 1.1)):
        place(x, clank(f0, 0.28, v), at, 0.6)
    # Rising power-on hum.
    hn = ns(1.75)
    hf = sweep(45, 130, hn, 1.4)
    hum = sum(saw(hf * k, hn) / k for k in (1, 2, 3)) * (t_of(hn) / 1.75) ** 1.6
    place(x, filt(hum, "lowpass", 1800) * 0.35, 0.0)
    # Bright digital chime when everything is online.
    place(x, chime(1568, 0.7) * 0.35, 1.6)
    place(x, chime(2349, 0.6) * 0.25, 1.68)
    place(x, sub_boom(0.6, 1.0) * 0.4, 1.6)
    save("cyborg_suit_up", fade(x, 0.002, 0.15), 0.9)


def cyborg_suit_down():
    dur = 1.5
    n = ns(dur)
    x = np.zeros(n)
    # Plates unlocking: lighter clicks with a pneumatic hiss.
    for at, f0 in ((0.02, 1200), (0.16, 1000), (0.3, 1350), (0.44, 900)):
        place(x, clank(f0, 0.2, 0.6), at, 0.5)
        hn = ns(0.18)
        place(x, filt(noise(hn), "highpass", 3000) * np.exp(-t_of(hn) * 20), at + 0.01, 0.35)
    # Servo whine falling and the power going down.
    sn = ns(1.2)
    place(x, servo(expsweep(1100, 120, sn), sn, 0.2) * (1 - t_of(sn) / 1.2) ** 1.2 * 0.35, 0.1)
    hn = ns(1.3)
    hf = expsweep(120, 30, hn)
    hum = (saw(hf, hn) + saw(hf * 2, hn) * 0.5) * (1 - t_of(hn) / 1.3) ** 1.5
    place(x, filt(hum, "lowpass", 1200) * 0.3, 0.15)
    save("cyborg_suit_down", fade(x, 0.002, 0.12), 0.8)


def cyborg_sonic_cannon():
    dur = 1.4
    n = ns(dur)
    x = np.zeros(n)
    # Charge whine.
    cn = ns(0.3)
    ct = t_of(cn)
    place(x, (sine(expsweep(500, 2600, cn), cn) + 0.3 * square(expsweep(250, 1300, cn), cn)) * (ct / 0.3) ** 2 * 0.25, 0)
    # Blast.
    at = 0.3
    bn = n - ns(at)
    bt = t_of(bn)
    thump = sine(60 * (1 + 2.5 * np.exp(-bt * 25)), bn) * np.exp(-bt * 5)
    burst = filt(noise(bn), "bandpass", [180, 1600]) * np.exp(-bt * 9)
    fall = filt(square(expsweep(700, 70, bn), bn), "lowpass", 2500) * np.exp(-bt * 4)
    wobble = 1 + 0.5 * np.sin(2 * np.pi * 28 * bt) * np.exp(-bt * 5)
    blast = thump * 1.6 + burst * 1.1 + fall * 0.5 * wobble
    place(x, np.tanh(blast * 1.5), at)
    save("cyborg_sonic_cannon", fade(x, 0.002, 0.15), 0.85)


def cyborg_missile_launch():
    dur = 0.9
    n = ns(dur)
    t = t_of(n)
    pop = filt(noise(n), "bandpass", [400, 4000]) * np.exp(-t * 60)
    hiss = filt(noise(n), "highpass", 2500) * np.exp(-t * 3.5) * np.minimum(1, t / 0.02)
    # Whoosh moving away: the band falls as the rocket leaves.
    lo = filt(noise(n), "bandpass", [800, 2400]) * (1 - t / dur)
    hi = filt(noise(n), "bandpass", [2400, 7000]) * np.exp(-t * 6)
    tone = sine(expsweep(1800, 600, n), n) * np.exp(-t * 4) * 0.12
    x = pop * 1.2 + hiss * 0.6 + (lo + hi) * np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 0.5 * 0.8 + tone
    save("cyborg_missile_launch", fade(x, 0.001, 0.12), 0.8)


def cyborg_missile_explode():
    dur = 1.2
    n = ns(dur)
    t = t_of(n)
    crack = filt(noise(n), "highpass", 1500) * np.exp(-t * 45)
    body = sine(70 * (1 + 3 * np.exp(-t * 30)), n) * np.exp(-t * 7)
    rumble = filt(noise(n), "lowpass", 400) * np.exp(-t * 5) * 1.2
    sparks = crackle(n, np.exp(-t * 3) * 3, 1200, 8000) * 0.8
    x = np.tanh((crack * 1.3 + body * 1.4 + rumble + sparks) * 1.3)
    save("cyborg_missile_explode", fade(x, 0.0005, 0.15), 0.72)


def cyborg_scan_on():
    dur = 0.85
    n = ns(dur)
    x = np.zeros(n)
    sn = ns(0.55)
    st = t_of(sn)
    sw = filt(square(expsweep(250, 2000, sn), sn), "lowpass", 4500) * np.sin(np.pi * st / 0.55) * 0.2
    place(x, sw + sine(expsweep(500, 4000, sn), sn) * np.sin(np.pi * st / 0.55) * 0.1, 0)
    place(x, beep(1760, 0.12, 0.35, "square", 18), 0.5)
    place(x, beep(2349, 0.2, 0.35, "square", 14), 0.62)
    save("cyborg_scan_on", fade(mono_reverb(x, 0.8, 0.15), 0.002, 0.08), 0.7)


def cyborg_scan_off():
    dur = 0.6
    n = ns(dur)
    t = t_of(n)
    sw = filt(square(expsweep(2000, 220, n), n), "lowpass", 4000) * (1 - t / dur) * 0.2
    sw += sine(expsweep(3200, 300, n), n) * (1 - t / dur) ** 2 * 0.1
    save("cyborg_scan_off", fade(sw, 0.002, 0.08), 0.65)


def cyborg_scan_ping():
    dur = 0.6
    n = ns(dur)
    t = t_of(n)
    f = 1320 * (1 + 0.02 * np.exp(-t * 20))
    x = sine(f, n) * np.minimum(1, t / 0.008) * np.exp(-t * 9)
    x += sine(f * 2, n) * np.exp(-t * 18) * 0.15
    echo = np.zeros(n)
    place(echo, x, 0.16, 0.3)
    save("cyborg_scan_ping", fade(x + echo, 0.002, 0.1), 0.5)


def cyborg_hack():
    dur = 1.65
    n = ns(dur)
    x = np.zeros(n)
    # Data chatter: random blips, some stuttered (repeated), all bit-crushed.
    at = 0.0
    while at < 1.08:
        seg = rng.uniform(0.018, 0.06)
        f = rng.choice([440, 660, 880, 990, 1320, 1760, 2200, 2640, 3080]) * rng.uniform(0.97, 1.03)
        bn = ns(seg)
        blip = square(f, bn) * np.minimum(1, t_of(bn) / 0.002) * (1 - t_of(bn) / seg) ** 0.5
        reps = 3 if rng.uniform() < 0.25 else 1
        for r in range(reps):
            place(x, blip, at + r * seg * 0.6, rng.uniform(0.15, 0.3))
        if rng.uniform() < 0.3:
            nn = ns(seg)
            place(x, filt(noise(nn), "highpass", 3000), at, 0.12)
        at += seg * (reps * 0.6 + 0.4) + rng.uniform(0, 0.015)
    x[:ns(1.12)] = bitcrush(x[:ns(1.12)], 4, 5)
    x = filt(x, "lowpass", 7000)
    # Access granted: two clean rising tones.
    place(x, beep(988, 0.16, 0.45, "square", 6), 1.2)
    place(x, beep(1480, 0.35, 0.45, "square", 5), 1.36)
    save("cyborg_hack", fade(x, 0.002, 0.1), 0.75)


def cyborg_emp():
    dur = 2.3
    n = ns(dur)
    x = np.zeros(n)
    # Electric build-up: crackles get denser and a buzz rises.
    bn = ns(0.7)
    bt = t_of(bn)
    build = (bt / 0.7) ** 2
    place(x, crackle(bn, build * 4) * 0.7, 0)
    place(x, filt(saw(sweep(60, 240, bn, 1.5), bn), "bandpass", [100, 3000]) * build * 0.25, 0)
    # The thud.
    at = 0.7
    tn = n - ns(at)
    tt = t_of(tn)
    thud = sine(42 * (1 + 2 * np.exp(-tt * 18)), tn) * np.exp(-tt * 3.2)
    blast = filt(noise(tn), "lowpass", 900) * np.exp(-tt * 8)
    snap = filt(noise(tn), "highpass", 2000) * np.exp(-tt * 40)
    # Buzzing tail: mains-like hum, flickering, with dying crackles.
    flick = 0.6 + 0.4 * (np.sin(2 * np.pi * 9 * tt) > 0)
    buzz = filt(saw(100, tn) + square(50, tn) * 0.5, "bandpass", [80, 2500]) * flick * np.exp(-tt * 2) * 0.35
    sparks = crackle(tn, np.exp(-tt * 2.5) * 2) * 0.5
    place(x, np.tanh((thud * 1.8 + blast + snap * 0.8) * 1.4) + buzz + sparks, at)
    save("cyborg_emp", fade(x, 0.002, 0.25), 0.95)


def cyborg_repair():
    dur = 2.0
    n = ns(dur)
    t = t_of(n)
    env = np.minimum(1, t / 0.25) * np.minimum(1, (dur - t) / 0.4)
    hum = (sine(220, n) + 0.5 * sine(330.5, n) + 0.3 * sine(440.8, n)) * (0.8 + 0.2 * np.sin(2 * np.pi * 3 * t)) * 0.18
    shim = np.zeros(n)
    for k, f in enumerate((2093, 2637, 3136, 3951, 4699)):
        trem = 0.5 + 0.5 * np.sin(2 * np.pi * (5 + k * 1.7) * t + k)
        shim += sine(f * (1 + 0.003 * np.sin(2 * np.pi * 0.8 * t + k)), n) * trem / (1 + k * 0.4)
    clicks = np.zeros(n)
    for at in np.sort(rng.uniform(0.05, dur - 0.1, 40)):
        cn = ns(0.012)
        place(clicks, filt(noise(cn), "highpass", 4000) * np.exp(-t_of(cn) * 400), at, rng.uniform(0.2, 0.5))
    x = (hum + shim * 0.07 + clicks * 0.6) * env
    save("cyborg_repair", fade(mono_reverb(x, 1.0, 0.2), 0.005, 0.1), 0.6)


def cyborg_battery_full():
    dur = 0.9
    n = ns(dur)
    x = np.zeros(n)
    for i, m in enumerate((84, 88, 91)):
        place(x, beep(midi_hz(m), 0.5 if i == 2 else 0.25, 0.3, "saw", 7 if i == 2 else 12), i * 0.11)
        place(x, beep(midi_hz(m + 12), 0.2, 0.08, "sine", 14), i * 0.11)
    save("cyborg_battery_full", fade(mono_reverb(x, 0.7, 0.15), 0.002, 0.1), 0.65)


def cyborg_boom_tube():
    """The Boom Tube: a huge resonant boom and a howling tunnel of sound, drowned in reverb."""
    dur = 2.6
    n = ns(dur)
    t = t_of(n)
    boom = sub_boom(dur, 1.0) * 1.4 + sine(32 * (1 + 3 * np.exp(-t * 14)), n) * np.exp(-t * 1.6)
    crack = filt(noise(n), "highpass", 1000) * np.exp(-t * 22)
    # Tunnel: noise through a comb filter whose delay sweeps (a flanging whoosh) plus a resonant chord.
    src = filt(noise(n), "bandpass", [150, 6000])
    delay = (0.0012 + 0.009 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.7 * t))) * SR
    idx = np.clip(np.arange(n) - delay.astype(int), 0, n - 1)
    tunnel = src + src[idx] * 0.9
    tunnel = (filt(tunnel, "lowpass", 2500) * np.sin(np.pi * t / dur) ** 0.8)
    swirl = np.zeros(n)
    for f in (65.4, 98.0, 130.8, 196.0, 311.1):
        swirl += saw(f * (1 + 0.01 * np.sin(2 * np.pi * 0.5 * t + f)), n) / (1 + f / 100)
    swirl = filt(swirl, "lowpass", 1400) * np.minimum(1, t / 0.08) * np.exp(-t * 1.1) * 0.5
    rise = sine(expsweep(200, 1600, n), n) * np.sin(np.pi * t / dur) ** 2 * 0.05
    x = np.tanh((boom + crack * 0.7 + tunnel * 0.9 + swirl + rise) * 1.1)
    x = mono_reverb(x, 2.6, 0.55)
    save("cyborg_boom_tube", fade(x, 0.001, 0.4), 0.97)


# ------------------------------------------------------------------------------------- the theme
# Our own dark electronic piece: C minor, 116 BPM, 16 bars. The bass pulses in 16ths, the arpeggio
# climbs the chord over two octaves, and the lead melody rises in steps to a high G and resolves
# through the dominant (G major, with its B natural) back to C minor.

BPM = 116
BEAT = 60.0 / BPM
BAR = 4 * BEAT
BARS = 16
LOOP = BARS * BAR

CHORDS = {
    "Cm": (36, [60, 63, 67]), "Ab": (32, [56, 60, 63]), "Bb": (34, [58, 62, 65]), "Eb": (39, [58, 63, 67]),
    "Fm": (41, [56, 60, 65]), "G": (31, [55, 59, 62]),
}
PROGRESSION = ["Cm", "Cm", "Ab", "Bb", "Cm", "Eb", "Fm", "G", "Ab", "Bb", "Cm", "Cm", "Fm", "Ab", "G", "G"]

MELODY = [
    [(72, 1.5), (75, 0.5), (79, 2)],
    [(77, 1), (75, 1), (74, 1), (75, 1)],
    [(72, 1.5), (68, 0.5), (72, 1), (75, 1)],
    [(74, 3), (70, 1)],
    [(72, 1.5), (75, 0.5), (79, 1), (84, 1)],
    [(82, 2), (79, 1), (77, 1)],
    [(80, 1.5), (79, 0.5), (77, 1), (75, 1)],
    [(74, 2), (71, 1), (74, 1)],
    [(75, 1), (77, 1), (79, 1), (80, 1)],
    [(82, 2), (79, 1), (77, 1)],
    [(79, 1.5), (84, 0.5), (84, 2)],
    [(82, 1), (79, 1), (75, 1), (79, 1)],
    [(80, 2), (84, 1), (80, 1)],
    [(84, 1.5), (82, 0.5), (80, 1), (79, 1)],
    [(79, 2), (83, 2)],
    [(86, 3), (83, 1)],
]


def kick(vel=1.0):
    n = ns(0.45)
    t = t_of(n)
    body = sine(48 * (1 + 2.2 * np.exp(-t * 28)), n) * np.exp(-t * 7)
    click = filt(noise(n), "highpass", 2000) * np.exp(-t * 200) * 0.4
    return vel * np.tanh((body + click) * 1.5)


def hat(vel=1.0, open_=False):
    n = ns(0.25 if open_ else 0.06)
    t = t_of(n)
    metal = sum(square(f, n) for f in (3150, 4270, 5830, 7110)) * 0.25
    x = filt(metal + noise(n), "highpass", 7000)
    return vel * x * np.exp(-t * (14 if open_ else 70))


def mech_clank(vel=1.0):
    return vel * filt(clank(rng.uniform(500, 700), 0.2, 1.0), "highpass", 300)


def bass_note(m, dur, vel=1.0):
    n = ns(dur)
    t = t_of(n)
    f = midi_hz(m)
    osc = saw(f, n) + saw(f * 1.006, n, 0.3) + sine(f / 2, n) * 1.2
    cutoff_env = np.exp(-t * 18)
    lo = filt(osc, "lowpass", 300)
    hi = filt(osc, "lowpass", 1400) * cutoff_env
    env = np.minimum(1, t / 0.004) * np.exp(-t * 4) * np.minimum(1, (dur - t) / 0.01)
    return vel * np.tanh((lo + hi) * env * 1.2)


def arp_note(m, dur, vel=1.0):
    n = ns(dur)
    t = t_of(n)
    f = midi_hz(m)
    osc = square(f, n) * 0.6 + saw(f * 1.004, n) * 0.4
    x = filt(osc, "lowpass", 900) + filt(osc, "lowpass", 3500) * np.exp(-t * 30)
    return vel * x * np.minimum(1, t / 0.003) * np.exp(-t * 9)


def power_chord(root, dur, vel=1.0, mute=False):
    """Synth 'guitar': root, fifth and octave of detuned saws pushed through heavy saturation."""
    n = ns(dur)
    t = t_of(n)
    x = np.zeros(n)
    for iv in (0, 7, 12):
        f = midi_hz(root + iv)
        for d in (-0.004, 0.0, 0.005):
            x += saw(f * (1 + d), n, rng.uniform())
    x = filt(x, "highpass", 90)
    x = np.tanh(x * 3.5)
    x = filt(x, "lowpass", 1800 if mute else 3800)
    env = np.minimum(1, t / 0.003) * (np.exp(-t * 14) if mute else (0.4 + 0.6 * np.exp(-t * 2.5)))
    env *= np.minimum(1, (dur - t) / 0.015)
    return vel * x * env


def lead_note(m, dur, vel=1.0):
    n = ns(dur)
    t = t_of(n)
    f = midi_hz(m) * (1 + 0.006 * np.sin(2 * np.pi * 5.5 * t) * np.clip((t - 0.15) / 0.2, 0, 1))
    osc = sum(saw(f * (1 + d), n, rng.uniform()) for d in (-0.007, -0.0025, 0.0, 0.0025, 0.007)) / 5
    osc += square(f / 2, n) * 0.25
    bright = filt(osc, "lowpass", 5000)
    soft = filt(osc, "lowpass", 1600)
    open_ = np.clip(t / 0.08, 0, 1)
    x = soft * (1 - open_ * 0.6) + bright * open_ * 0.6
    env = np.minimum(1, t / 0.01) * (0.75 + 0.25 * np.exp(-t * 4)) * np.minimum(1, (dur - t) / 0.05)
    return vel * np.tanh(x * 1.6 * env)


def ride(vel=1.0):
    n = ns(0.6)
    t = t_of(n)
    metal = sum(sine(f, n) for f in (5120, 6830, 8410, 9370)) * 0.2
    return vel * (filt(noise(n), "highpass", 6000) * 0.6 + metal) * np.exp(-t * 7)


def echo(track, x, start, gain, pan, taps=3, time=BEAT * 0.75, fb=0.4):
    for k in range(taps + 1):
        track.add(x, start + k * time, gain * fb ** k, pan=pan if k % 2 == 0 else -pan)


def render_base(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        # Bass pulse in 16ths: root with an octave jump at the end of each beat.
        for s in range(16):
            m = root + (12 if s % 4 == 3 else 0)
            vel = 1.0 if s % 4 == 0 else 0.7
            track.add(bass_note(m, BEAT / 4 * 0.95, vel), t0 + s * BEAT / 4, 0.32)
        # Mechanical beat: kick on 1, 2-and, 3; snare on 2 and 4; 16th hats; a clank on the off-beat.
        for k_at in (0, 1.5, 2, 3.75 if b % 2 else 2.75):
            track.add(kick(), t0 + k_at * BEAT, 0.55)
        track.add(snare(0.9), t0 + BEAT, 0.3)
        track.add(snare(0.9), t0 + 3 * BEAT, 0.3)
        for s in range(16):
            open_ = s % 8 == 6
            track.add(hat(0.9 if s % 2 == 0 else 0.55, open_), t0 + s * BEAT / 4, 0.12, pan=0.3)
        track.add(mech_clank(0.6), t0 + 2.5 * BEAT, 0.12, pan=-0.4)
        if b % 4 == 3:
            track.add(mech_clank(0.8), t0 + 3.5 * BEAT, 0.14, pan=0.4)
        # Dark arpeggio in 16ths over two octaves, with ping-pong echoes.
        notes = triad + [m + 12 for m in triad]
        order = [0, 1, 2, 3, 4, 5, 4, 3]
        for s in range(16):
            m = notes[order[s % 8]]
            echo(track, arp_note(m, BEAT * 0.4, 0.9 if s % 4 == 0 else 0.6), t0 + s * BEAT / 4, 0.09, 0.45,
                 taps=2)


def render_peak(track, offset):
    for b, name in enumerate(PROGRESSION):
        t0 = offset + b * BAR
        root, triad = CHORDS[name]
        pc = root + 12 if root < 36 else root
        # Power chords: a big hit on 1, palm-muted chugs, a hit on 3-and that rings.
        track.add(power_chord(pc, BEAT * 0.98, 1.0), t0, 0.16, pan=-0.45)
        track.add(power_chord(pc, BEAT * 0.98, 1.0), t0, 0.16, pan=0.45)
        for e in (2, 3, 4, 7):  # 8th-note positions
            track.add(power_chord(pc, BEAT * 0.45, 0.8, mute=True), t0 + e * BEAT / 2, 0.14,
                      pan=-0.4 if e % 2 else 0.4)
        track.add(power_chord(pc, BEAT * 0.98, 0.9), t0 + 2.5 * BEAT, 0.14, pan=0.0)
        # Cymbals.
        if b % 4 == 0:
            track.add(crash(2.6), t0, 0.3, pan=-0.2)
        if b % 8 == 7:
            track.add(swell_cymbal(BAR), t0, 0.22)
        for e in range(8):
            track.add(ride(0.8 if e % 2 == 0 else 0.5), t0 + e * BEAT / 2, 0.08, pan=0.35)
        track.add(sub_boom(0.8, 0.6), t0, 0.25)
        # Lead melody with a quiet octave-down double and a delay.
        t = t0
        for m, beats in MELODY[b]:
            note = lead_note(m, beats * BEAT * 0.97, 1.0)
            echo(track, note, t, 0.2, 0.15, taps=2, time=BEAT * 0.75, fb=0.25)
            track.add(lead_note(m - 12, beats * BEAT * 0.97, 0.7), t, 0.08, pan=-0.2)
            t += beats * BEAT


def render_stem(fn):
    """Renders two loop cycles and keeps the second one, so echoes and reverb wrap around the loop."""
    track = Track(LOOP * 2)
    fn(track, 0.0)
    fn(track, LOOP)
    l, r = reverb(track.l, track.r, seconds=1.8, wet=0.22)
    a, b = int(LOOP * SR), int(2 * LOOP * SR)
    return np.stack([l[a:b], r[a:b]], axis=1)


def master(x):
    x = filt(x.T, "highpass", 30).T
    return np.tanh(x * 1.3) / np.tanh(1.3)


def cyborg_theme():
    base = master(render_stem(render_base))
    peak = master(render_stem(render_peak))
    scale = 0.85 / np.max(np.abs(base + peak))
    for name, stem in (("cyborg_theme_base", base), ("cyborg_theme_peak", peak)):
        path = os.path.join(ROOT, "music", name + ".ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sf.write(path, (stem * scale).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        print("wrote", os.path.relpath(path), f"{len(stem) / SR:.2f}s")


def main():
    cyborg_suit_up()
    cyborg_suit_down()
    cyborg_sonic_cannon()
    cyborg_missile_launch()
    cyborg_missile_explode()
    cyborg_scan_on()
    cyborg_scan_off()
    cyborg_scan_ping()
    cyborg_hack()
    cyborg_emp()
    cyborg_repair()
    cyborg_battery_full()
    cyborg_boom_tube()
    cyborg_theme()


if __name__ == "__main__":
    main()
