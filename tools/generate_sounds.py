#!/usr/bin/env python3
"""
Procedural sound effect generator for the Green Lantern mod.

All sounds are synthesized from scratch (no samples), exported as mono
Ogg Vorbis files (mono is required by Minecraft for positional audio).

Usage:  pip install numpy soundfile
        python tools/generate_sounds.py
"""
import os
import numpy as np
import soundfile as sf

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "greenlantern", "sounds")
rng = np.random.default_rng(2814)  # Sector 2814 :)


def t_axis(dur):
    return np.linspace(0, dur, int(SR * dur), endpoint=False)


def env_adsr(n, a=0.01, d=0.1, s=0.7, r=0.2):
    a_n, d_n, r_n = int(a * SR), int(d * SR), int(r * SR)
    s_n = max(n - a_n - d_n - r_n, 0)
    e = np.concatenate([
        np.linspace(0, 1, a_n, endpoint=False),
        np.linspace(1, s, d_n, endpoint=False),
        np.full(s_n, s),
        np.linspace(s, 0, r_n),
    ])
    return np.pad(e, (0, max(0, n - len(e))))[:n]


def env_exp(n, k):
    return np.exp(-np.linspace(0, k, n))


def sweep(f0, f1, dur, shape="exp"):
    t = t_axis(dur)
    if shape == "exp":
        f = f0 * (f1 / f0) ** (t / dur)
    else:
        f = f0 + (f1 - f0) * (t / dur)
    phase = 2 * np.pi * np.cumsum(f) / SR
    return phase


def noise(n):
    return rng.uniform(-1, 1, n)


def lowpass(x, cutoff):
    # simple one-pole low-pass, cutoff may be array or scalar
    cutoff = np.broadcast_to(cutoff, x.shape)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        a = 1 - np.exp(-2 * np.pi * cutoff[i] / SR)
        acc += a * (x[i] - acc)
        y[i] = acc
    return y


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def shimmer(t, base, voices=5, detune=0.004):
    out = np.zeros_like(t)
    for v in range(voices):
        d = 1 + detune * (v - voices / 2)
        out += np.sin(2 * np.pi * base * d * t + rng.uniform(0, 6.28))
    return out / voices


def echo(x, delay=0.09, fb=0.35, taps=4):
    d = int(delay * SR)
    out = np.concatenate([x, np.zeros(d * taps)])
    for k in range(1, taps + 1):
        out[d * k:d * k + len(x)] += x * (fb ** k)
    return out


def normalize(x, peak=0.9):
    m = np.max(np.abs(x))
    return x if m == 0 else x / m * peak


def fade(x, in_ms=1.5, out_ms=10):
    x = x.copy()
    n_in, n_out = int(SR * in_ms / 1000), int(SR * out_ms / 1000)
    x[:n_in] *= np.linspace(0, 1, n_in)
    x[-n_out:] *= np.linspace(1, 0, n_out)
    return x


def save(name, x, peak=0.9):
    x = fade(normalize(x, peak))
    path = os.path.join(OUT, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sf.write(path, x.astype(np.float32), SR, format="OGG", subtype="VORBIS")
    print("wrote", os.path.relpath(path))


# ---------------------------------------------------------------- sounds

def ring_activate():
    dur = 1.6
    t = t_axis(dur)
    n = len(t)
    chord = sum(shimmer(t, f, 6) for f in (220, 277.2, 329.6, 440, 554.4))
    rise = np.sin(sweep(180, 1400, dur)) * 0.5
    air = lowpass(noise(n), np.linspace(400, 6000, n)) * 0.6
    body = (chord * 0.8 + rise + air) * env_adsr(n, 0.25, 0.3, 0.8, 0.7)
    sparkle = np.sin(2 * np.pi * 1760 * t) * env_exp(n, 6) * (t > 0.2) * 0.3
    return echo(body + sparkle, 0.11, 0.3)


def ring_deactivate():
    dur = 1.0
    t = t_axis(dur)
    n = len(t)
    fall = np.sin(sweep(900, 110, dur)) + 0.5 * np.sin(sweep(1350, 165, dur))
    air = lowpass(noise(n), np.linspace(5000, 300, n)) * 0.5
    return (fall + air) * env_adsr(n, 0.01, 0.2, 0.6, 0.6)


def blast_fire(seed_shift=0):
    dur = 0.45
    t = t_axis(dur)
    n = len(t)
    zap = np.sin(sweep(2400 + seed_shift, 180, dur)) + 0.4 * np.sign(np.sin(sweep(1200, 90, dur)))
    fizz = highpass(noise(n), 2500) * env_exp(n, 12) * 0.6
    sub = np.sin(sweep(160, 50, dur)) * env_exp(n, 8)
    return (zap * env_exp(n, 7) + fizz + sub * 0.8)


def blast_impact():
    dur = 0.9
    t = t_axis(dur)
    n = len(t)
    boom = np.sin(sweep(120, 35, dur)) * env_exp(n, 6)
    crackle = lowpass(noise(n), np.linspace(8000, 500, n)) * env_exp(n, 5)
    ring = shimmer(t, 660, 4) * env_exp(n, 9) * 0.3
    return boom * 1.2 + crackle * 0.9 + ring


def gun_fire(variant):
    dur = 0.16
    t = t_axis(dur)
    n = len(t)
    f0 = [1900, 2100, 1750][variant]
    zap = np.sin(sweep(f0, 300, dur)) * env_exp(n, 14)
    click = highpass(noise(n), 3000) * env_exp(n, 40)
    thump = np.sin(sweep(220, 70, dur)) * env_exp(n, 18)
    return zap + click * 0.7 + thump * 0.8


def bubble_up():
    dur = 1.2
    t = t_axis(dur)
    n = len(t)
    whoosh = lowpass(noise(n), 300 + 3000 * np.sin(np.pi * t / dur)) * 1.2
    hum = shimmer(t, 110, 6) + 0.6 * shimmer(t, 220, 6) + 0.3 * shimmer(t, 330, 4)
    wobble = 1 + 0.25 * np.sin(2 * np.pi * 6 * t)
    return (whoosh * env_adsr(n, 0.05, 0.3, 0.3, 0.6) +
            hum * wobble * env_adsr(n, 0.3, 0.2, 0.7, 0.5) * 0.8)


def bubble_hit():
    dur = 0.5
    t = t_axis(dur)
    n = len(t)
    ping = shimmer(t, 1318.5, 4) + 0.6 * shimmer(t, 1975.5, 4)
    thud = np.sin(sweep(300, 90, dur)) * env_exp(n, 12)
    return ping * env_exp(n, 8) * 0.8 + thud


def saw_summon():
    dur = 1.0
    t = t_axis(dur)
    n = len(t)
    spin = np.sign(np.sin(sweep(40, 320, dur))) * 0.5 + np.sin(sweep(80, 640, dur))
    spin = lowpass(spin, 2500)
    air = lowpass(noise(n), np.linspace(300, 4000, n)) * 0.6
    return (spin + air) * env_adsr(n, 0.05, 0.1, 0.9, 0.2)


def saw_loop():
    # seamless 1 second loop
    dur = 1.0
    t = t_axis(dur)
    n = len(t)
    f = 160  # integer number of cycles per second -> seamless
    teeth = (2 * ((t * f) % 1) - 1)  # sawtooth
    teeth = lowpass(teeth, 3000)
    harm = np.sin(2 * np.pi * 320 * t) * 0.4 + np.sin(2 * np.pi * 480 * t) * 0.2
    trem = 1 + 0.15 * np.sin(2 * np.pi * 20 * t)
    grit = lowpass(noise(n), 5000) * 0.25
    return (teeth + harm + grit) * trem


def saw_cut():
    dur = 0.5
    t = t_axis(dur)
    n = len(t)
    screech = np.sin(sweep(900, 1500, dur)) * (1 + 0.5 * np.sin(2 * np.pi * 45 * t))
    grind = highpass(noise(n), 1200) * 0.8
    return (screech * 0.7 + grind) * env_adsr(n, 0.01, 0.1, 0.8, 0.25)


def hammer_summon():
    dur = 0.9
    t = t_axis(dur)
    n = len(t)
    whoosh = lowpass(noise(n), 200 + 2500 * (t / dur) ** 2) * 1.3
    rise = shimmer(t, 146.8, 5) * env_adsr(n, 0.4, 0.1, 0.8, 0.3)
    return whoosh * env_adsr(n, 0.3, 0.2, 0.8, 0.2) + rise * 0.6


def hammer_impact():
    dur = 2.0
    t = t_axis(dur)
    n = len(t)
    sub = np.sin(sweep(90, 28, dur)) * env_exp(n, 3.5)
    body = lowpass(noise(n), np.linspace(3000, 150, n)) * env_exp(n, 4) * 1.4
    crack = highpass(noise(n), 2000) * env_exp(n, 30) * 1.2
    metal = shimmer(t, 196, 5) * env_exp(n, 5) * 0.4
    return echo(sub * 1.6 + body + crack + metal, 0.14, 0.25, 3)


def charge_loop():
    dur = 2.0
    t = t_axis(dur)
    n = len(t)
    rise = np.sin(sweep(110, 440, dur)) + 0.5 * np.sin(sweep(165, 660, dur))
    chorus = shimmer(t, 220, 6) * 0.5
    air = lowpass(noise(n), np.linspace(300, 3000, n)) * 0.3
    return (rise + chorus + air) * env_adsr(n, 0.2, 0.1, 0.9, 0.3)


def charge_complete():
    dur = 2.2
    t = t_axis(dur)
    n = len(t)
    notes = [(523.25, 0.0), (659.25, 0.08), (783.99, 0.16), (1046.5, 0.24)]
    out = np.zeros(n)
    for f, start in notes:
        s = int(start * SR)
        seg = t[: n - s]
        out[s:] += shimmer(seg, f, 5) * env_exp(len(seg), 3)
    bloom = lowpass(noise(n), 2500) * env_adsr(n, 0.05, 0.3, 0.2, 1.0) * 0.3
    return echo(out + bloom, 0.12, 0.3)


def construct_select():
    dur = 0.25
    t = t_axis(dur)
    n = len(t)
    blip = np.sin(sweep(900, 1500, dur, "lin")) * env_exp(n, 10)
    blip2 = np.sin(2 * np.pi * 2200 * t) * env_exp(n, 25) * 0.4
    return blip + blip2


def low_energy():
    dur = 0.6
    t = t_axis(dur)
    n = len(t)
    beep = np.sign(np.sin(2 * np.pi * 440 * t)) * 0.4 + np.sin(2 * np.pi * 880 * t)
    gate = ((t % 0.3) < 0.15).astype(float)
    return lowpass(beep, 3000) * gate * env_adsr(n, 0.005, 0.05, 0.8, 0.1)


def flight_whoosh():
    dur = 1.4
    t = t_axis(dur)
    n = len(t)
    band = lowpass(highpass(noise(n), 200), 600 + 1800 * np.sin(np.pi * t / dur) ** 2)
    hum = shimmer(t, 98, 5) * 0.3
    return (band * 1.5 + hum) * env_adsr(n, 0.4, 0.2, 0.8, 0.6)


def main():
    save("ring_activate", ring_activate())
    save("ring_deactivate", ring_deactivate())
    save("blast_fire", blast_fire())
    save("blast_impact", blast_impact())
    for i in range(3):
        save(f"gun_fire{i + 1}", gun_fire(i), 0.75)
    save("bubble_up", bubble_up())
    save("bubble_hit", bubble_hit())
    save("saw_summon", saw_summon())
    save("saw_loop", saw_loop(), 0.7)
    save("saw_cut", saw_cut(), 0.8)
    save("hammer_summon", hammer_summon())
    save("hammer_impact", hammer_impact(), 0.95)
    save("charge_loop", charge_loop(), 0.7)
    save("charge_complete", charge_complete())
    save("construct_select", construct_select(), 0.6)
    save("low_energy", low_energy(), 0.6)
    save("flight_whoosh", flight_whoosh(), 0.6)


if __name__ == "__main__":
    main()
