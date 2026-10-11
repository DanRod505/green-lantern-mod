"""Original synthesized sound for a hero: oscillators, filtered noise and envelopes, no samples. Sound
effects are short mono files; a theme is two looping stereo stems (base and peak) that the game mixes
by flight or action intensity, like every hero's theme.

    from herolib.audio import *

    def heat_bolt():
        x = whoosh(0.25, 800, 6000) + sizzle(ns(0.25)) * 0.3
        save("supergirl_heat_bolt", fade(x, 0.001, 0.05), 0.8)

    theme = Theme("supergirl_theme", bpm=138, bars=16)
    def base(track, offset): track.add(kick(), offset + theme.beat * 0, 0.8) ...
    def peak(track, offset): ...
    theme.render(base, peak)

The orchestral instruments (strings_note, brass_note, choir_note, timpani, tom, snare, crash,
sub_boom, swell_cymbal), Track, reverb, filt, fade, normalize, midi_hz and save come from the flight
theme generator (tools/generate_flight_audio.py), which every hero's audio already shared. Needs
numpy, scipy and soundfile (pip install numpy scipy soundfile).
"""
import os
import sys

import numpy as np
import soundfile as sf

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

from generate_flight_audio import (ROOT, SR, Track, brass_note, choir_note, crash, fade, filt, midi_hz,  # noqa: E402,F401
                                   normalize, reverb, save, snare, strings_note, sub_boom, swell_cymbal, timpani, tom)

rng = np.random.default_rng(1)


def seed(value):
    """Reseeds the noise, so a hero's script writes the same files every time."""
    global rng
    rng = np.random.default_rng(value)


# ---- basics ----------------------------------------------------------------------------------------

def ns(dur):
    """Seconds to samples."""
    return int(dur * SR)


def t_of(n):
    """The time of each of n samples."""
    return np.arange(n) / SR


def noise(n):
    return rng.uniform(-1, 1, n)


def sine(freq, n, ph0=0.0):
    """A sine; freq may be a number or one frequency per sample (a sweep)."""
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    return np.sin(2 * np.pi * (np.cumsum(f) / SR + ph0))


def saw(freq, n, ph0=0.0):
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    p = (np.cumsum(f) / SR + ph0) % 1.0
    return 2 * p - 1


def expsweep(f0, f1, n, curve=1.0):
    """Frequencies gliding from f0 to f1 over n samples (pass to sine or saw)."""
    return f0 * (f1 / f0) ** (np.linspace(0, 1, n, endpoint=False) ** curve)


def chirp(f0, f1, dur, curve=1.0):
    n = ns(dur)
    t = t_of(n)
    return np.sin(2 * np.pi * np.cumsum(f0 + (f1 - f0) * (t / dur) ** curve) / SR)


def place(x, y, at, gain=1.0):
    """Mixes y into x starting at `at` seconds (whatever runs past the end is dropped)."""
    i = int(at * SR)
    if i >= len(x):
        return
    k = min(len(y), len(x) - i)
    x[i:i + k] += y[:k] * gain


def mono_reverb(x, seconds=1.5, wet=0.3):
    l, r = reverb(x, x.copy(), seconds=seconds, wet=wet)
    return (l + r) * 0.5


# ---- effects ---------------------------------------------------------------------------------------

def whoosh(dur, lo=300, hi=4000, rise=True):
    """Air rushing past: band-passed noise sweeping from lo to hi (or down when rise is False)."""
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
    """Crackling heat or electricity: sparse random pops on a bed of hiss."""
    pops = (rng.uniform(0, 1, n) < 0.03) * noise(n) * 3
    return filt(pops, "bandpass", [lo, hi]) + filt(noise(n), "bandpass", [lo, hi]) * 0.25


def sparkle(dur, base=1500, count=18):
    """Small chimes scattered in time (magic, light, a full charge)."""
    n = ns(dur)
    x = np.zeros(n)
    for at in np.sort(rng.uniform(0, dur * 0.8, count)):
        f = base * 2 ** rng.choice([0, 4 / 12, 7 / 12, 1, 16 / 12])
        place(x, bell(f, 0.5, rng.uniform(0.3, 0.8)), at)
    return x


def impact(dur=0.8, vel=1.0):
    """A heavy hit: a sub drop with a burst of low noise."""
    n = ns(dur)
    t = t_of(n)
    body = sine(expsweep(120, 35, n), n) * np.exp(-t * 6)
    crack = filt(noise(n), "lowpass", 1800) * np.exp(-t * 25)
    return vel * np.tanh((body + crack * 0.7) * 1.8)


def zap(dur=0.3, f0=1800, f1=200, vel=1.0):
    """An energy shot: a falling square-ish tone with a little noise."""
    n = ns(dur)
    t = t_of(n)
    tone = np.sign(sine(expsweep(f0, f1, n), n)) * 0.5 + saw(expsweep(f0 * 0.5, f1 * 0.5, n), n) * 0.3
    return vel * filt(tone + noise(n) * 0.1, "lowpass", 6000) * np.exp(-t * 6)


# ---- drums and synths for themes ------------------------------------------------------------------

def kick(vel=1.0):
    n = ns(0.4)
    t = t_of(n)
    body = sine(52 * (1 + 2.0 * np.exp(-t * 30)), n) * np.exp(-t * 8)
    click = filt(noise(n), "highpass", 2500) * np.exp(-t * 220) * 0.35
    return vel * np.tanh((body + click) * 1.5)


def clap(vel=1.0):
    n = ns(0.3)
    x = np.zeros(n)
    for d in (0.0, 0.011, 0.022):
        place(x, filt(noise(ns(0.25)), "bandpass", [900, 5000]) * np.exp(-t_of(ns(0.25)) * 22), d)
    return vel * x


def hat(vel=1.0):
    n = ns(0.05)
    t = t_of(n)
    return vel * filt(noise(n), "highpass", 8000) * np.exp(-t * 80)


def pluck(m, dur, vel=1.0):
    """Bright plucked synth (a filtered saw with a quick decay); m is a MIDI note."""
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


# ---- themes ----------------------------------------------------------------------------------------

def master(x):
    """Gentle mastering of a stereo stem: no rumble, soft saturation."""
    x = filt(x.T, "highpass", 30).T
    return np.tanh(x * 1.3) / np.tanh(1.3)


class Theme:
    """
    A hero theme in two aligned, seamlessly looping stereo stems, music/<name>_base.ogg and
    music/<name>_peak.ogg. Each layer is a function (track, offset) that adds notes to the Track
    starting at `offset` seconds; it is rendered twice and the second cycle kept, so the reverb
    tail wraps around the loop.
    """

    def __init__(self, name, bpm, bars=16, beats_per_bar=4, reverb_seconds=1.8, wet=0.22):
        self.name = name
        self.beat = 60.0 / bpm
        self.bar = beats_per_bar * self.beat
        self.loop = bars * self.bar
        self.reverb_seconds = reverb_seconds
        self.wet = wet

    def stem(self, layer):
        track = Track(self.loop * 2)
        layer(track, 0.0)
        layer(track, self.loop)
        l, r = reverb(track.l, track.r, seconds=self.reverb_seconds, wet=self.wet)
        a, b = int(self.loop * SR), int(2 * self.loop * SR)
        return master(np.stack([l[a:b], r[a:b]], axis=1))

    def render(self, base, peak):
        """Writes both stems, scaled together so base and peak played at once never clip."""
        stems = {"base": self.stem(base), "peak": self.stem(peak)}
        scale = 0.85 / max(1e-9, np.max(np.abs(stems["base"] + stems["peak"])))
        for layer, stem in stems.items():
            path = os.path.join(ROOT, "music", f"{self.name}_{layer}.ogg")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            sf.write(path, (stem * scale).astype(np.float32), SR, format="OGG", subtype="VORBIS")
            print("wrote", os.path.relpath(path), f"{len(stem) / SR:.2f}s")
