#!/usr/bin/env python3
"""
Audio of the Trench (v1.13.0): the clicking chatter of the creatures, their hunting screech, the
bite, the hurt and dying shrieks, the far-off clicks and creaks that come out of the caves of their
territory, the low sting when a player crosses into it, the war cry of a raid on Atlantis and the
membrane of a cocoon tearing open.

Everything is synthesized from scratch: no samples, no third-party audio.

Usage:  pip install numpy scipy soundfile
        python tools/generate_trench_audio.py
"""
import numpy as np

from generate_aquaman_audio import bubbles, noise, place, t_of, water
from generate_flight_audio import SR, fade, filt, save, sub_boom
from generate_kraken_audio import growl

rng = np.random.default_rng(1313)


def click(bright=3200, dur=0.012, ring=900):
    """One wet click: a sharp noise burst through a resonant band, with a short ring."""
    n = int(dur * SR)
    t = t_of(n)
    burst = filt(noise(n), "bandpass", [bright * 0.6, bright * 1.6]) * np.exp(-t * 600)
    tone = np.sin(2 * np.pi * ring * t) * np.exp(-t * 300) * 0.4
    return burst + tone


def click_train(dur, rate, bright=3200, jitter=0.35, accel=0.0, ring=900):
    """A burst of clicks (the creatures' echolocation-like chatter); accel > 0 speeds it up."""
    n = int(dur * SR)
    x = np.zeros(n)
    at = 0.0
    while at < dur - 0.02:
        place(x, click(bright * rng.uniform(0.8, 1.25), ring=ring * rng.uniform(0.8, 1.2)), at, rng.uniform(0.5, 1.0))
        r = rate * (1 + accel * at / dur)
        at += (1.0 / r) * rng.uniform(1 - jitter, 1 + jitter)
    return x


def shriek(f0, f1, dur, harsh=0.5):
    """A thin, piercing, inhuman cry: a high glide with beating partials and a rasp."""
    n = int(dur * SR)
    t = t_of(n)
    glide = f0 * (f1 / f0) ** (t / dur)
    ph = 2 * np.pi * np.cumsum(glide) / SR
    ph2 = 2 * np.pi * np.cumsum(glide * 1.013) / SR
    src = np.sin(ph) + 0.6 * np.sin(2 * ph2) + 0.35 * np.sin(3 * ph) + 0.2 * np.sin(4.02 * ph2)
    rasp = 1 + harsh * filt(rng.uniform(-1, 1, n), "lowpass", 90) * 6
    out = src * rasp
    out = filt(out, "highpass", 500)
    env = np.minimum(t / 0.03, 1.0) * np.minimum((dur - t) / (dur * 0.5), 1.0)
    return out * np.clip(env, 0, 1)


def murk(x, wet=0.35, seconds=0.9):
    """Underwater space: muffled, with a dull, short echo."""
    ir_n = int(seconds * SR)
    ir_t = t_of(ir_n)
    ir = rng.standard_normal(ir_n) * np.exp(-ir_t * 6)
    ir = filt(ir, "lowpass", 1400)
    ir /= np.max(np.abs(ir))
    from scipy.signal import fftconvolve
    tail = fftconvolve(x, ir)[: len(x)]
    tail /= max(1e-9, np.max(np.abs(tail)))
    dry = filt(x, "lowpass", 5200)
    return dry * (1 - wet) + tail * wet * np.max(np.abs(dry))


def pad(x, dur):
    out = np.zeros(int(dur * SR))
    out[: min(len(x), len(out))] = x[: len(out)]
    return out


def trench_idle():
    # Short chatter of clicks, a hiss and a wet gurgle, each one a little different.
    for i in range(3):
        dur = 1.2
        x = np.zeros(int(dur * SR))
        place(x, click_train(0.45 + 0.1 * i, 22 + 6 * i, 2800 + 400 * i, accel=0.8), 0.0, 0.9)
        place(x, growl(150 + 20 * i, 120, 0.5, 0.8, (420, 900, 1800)) * 0.5, 0.45, 0.6)
        place(x, bubbles(0.4, 30, 300, 900), 0.6, 0.3)
        save(f"trench_idle{i + 1}", fade(murk(x, 0.25), 0.002, 0.15), 0.8)


def trench_screech():
    # The hunting cry: a rattle of clicks rising into a piercing shriek.
    for i in range(2):
        dur = 1.4
        x = np.zeros(int(dur * SR))
        place(x, click_train(0.35, 30, 3500, accel=2.0), 0.0, 0.8)
        place(x, shriek(1300 + 200 * i, 2100 + 300 * i, 0.9, 0.6), 0.25, 1.0)
        place(x, shriek(900 + 120 * i, 1500, 0.8, 0.8) * 0.5, 0.3, 0.6)
        save(f"trench_screech{i + 1}", fade(murk(x, 0.3), 0.002, 0.2), 0.88)


def trench_hurt():
    for i in range(2):
        x = shriek(1700 + 250 * i, 1100, 0.35, 0.9)
        place(x, click_train(0.12, 40, 3000), 0.0, 0.6)
        save(f"trench_hurt{i + 1}", fade(murk(x, 0.2), 0.002, 0.08), 0.85)


def trench_death():
    dur = 1.8
    x = np.zeros(int(dur * SR))
    place(x, shriek(1600, 500, 1.2, 1.0), 0.0, 1.0)
    place(x, growl(130, 60, 1.4, 0.9, (380, 800, 1600)) * 0.7, 0.2, 0.7)
    place(x, bubbles(1.2, 50, 200, 1000), 0.5, 0.5)
    save("trench_death", fade(murk(x, 0.3), 0.002, 0.3), 0.85)


def trench_bite():
    for i in range(2):
        dur = 0.5
        n = int(dur * SR)
        t = t_of(n)
        snap = filt(noise(n), "bandpass", [1500, 6000]) * np.exp(-t * 90)
        crunch = filt(noise(n), "bandpass", [300, 1800]) * np.exp(-np.maximum(t - 0.03, 0) * 30) * (t > 0.03)
        thud = np.sin(2 * np.pi * 110 * t) * np.exp(-t * 30) * 0.6
        x = snap * 1.0 + crunch * 0.6 + thud
        place(x, click_train(0.08, 50, 3800), 0.02, 0.4 + 0.1 * i)
        save(f"trench_bite{i + 1}", fade(murk(x, 0.2), 0.001, 0.1), 0.9)


def trench_ambience():
    # Far off, out of the caves: echoing clicks, a creak of something big, a distant wail.
    for i in range(4):
        dur = 4.0
        n = int(dur * SR)
        t = t_of(n)
        x = filt(noise(n), "lowpass", 90) * 0.35 * (0.6 + 0.4 * np.sin(2 * np.pi * 0.2 * t + i))
        at = rng.uniform(0.1, 0.6)
        for _ in range(2 + i % 2):
            place(x, click_train(rng.uniform(0.4, 0.9), rng.uniform(12, 30), rng.uniform(1800, 3000), accel=rng.uniform(-0.3, 1.2)), at,
                  rng.uniform(0.4, 0.8))
            at += rng.uniform(0.8, 1.4)
            if at > dur - 1:
                break
        if i % 2 == 0:
            # A long creak (bone and rock grinding).
            cn = int(1.6 * SR)
            ct = t_of(cn)
            f = 70 + 25 * np.sin(2 * np.pi * 0.7 * ct)
            ph = 2 * np.pi * np.cumsum(f) / SR
            creak = np.sign(np.sin(ph)) * (np.sin(2 * np.pi * 23 * ct) > 0.2) * np.exp(-((ct - 0.8) ** 2) * 3)
            place(x, filt(creak, "bandpass", [150, 1200]) * 0.4, rng.uniform(1.0, 2.0), 0.6)
        else:
            place(x, shriek(900, 700, 1.4, 0.4) * 0.25, rng.uniform(1.4, 2.2), 0.5)
        x = murk(x, 0.6, 1.6)
        x = filt(x, "lowpass", 2600)
        save(f"trench_ambience{i + 1}", fade(x, 0.4, 0.8), 0.6)


def trench_territory():
    # Crossing into their territory: a low swell, a dissonant cluster, the deep closing in.
    dur = 4.5
    n = int(dur * SR)
    t = t_of(n)
    swell = np.minimum(t / 2.2, 1.0) * np.exp(-np.maximum(t - 2.6, 0) * 1.4)
    x = np.zeros(n)
    for f, g in ((41.2, 1.0), (43.6, 0.8), (61.7, 0.5), (87.3, 0.35), (92.5, 0.3)):
        x += np.sin(2 * np.pi * f * t + rng.uniform(0, 6)) * g
    x = x * swell
    x += filt(noise(n), "lowpass", 200) * swell * 0.5
    place(x, click_train(1.2, 18, 2400, accel=1.5), 1.8, 0.35)
    place(x, sub_boom(1.5, 0.8), 0.0, 0.6)
    save("trench_territory", fade(murk(x, 0.4, 1.4), 0.05, 0.6), 0.85)


def trench_raid():
    # The war cry of a raid: a hollow, dissonant blast through a great shell, then the swarm answers.
    dur = 6.0
    n = int(dur * SR)
    x = np.zeros(n)
    for f, g in ((73.4, 1.0), (77.8, 0.7), (110.0, 0.45)):
        horn = growl(f, f * 0.94, 2.6, 0.25, (300, 650, 1300)) * g
        place(x, horn, 0.0, 1.0)
    place(x, sub_boom(2.0, 1.0), 0.0, 0.7)
    for k in range(6):
        place(x, shriek(1200 + 150 * k, 1700 + 120 * k, 0.8, 0.7) * 0.35, 2.4 + k * 0.28, 0.8)
        place(x, click_train(0.5, 28, 3200, accel=1.0), 2.3 + k * 0.3, 0.5)
    save("trench_raid", fade(murk(x, 0.45, 1.6), 0.01, 0.8), 0.92)


def cocoon_burst():
    dur = 1.6
    n = int(dur * SR)
    t = t_of(n)
    # Membrane stretching (rising creak), the tear (a ripping crackle), then a gush of bubbles.
    stretch = filt(noise(n), "bandpass", [300, 1600]) * np.clip(t / 0.35, 0, 1) * (t < 0.4)
    rip_n = int(0.45 * SR)
    rt = t_of(rip_n)
    rip = filt(noise(rip_n), "bandpass", [900, 7000]) * (rng.uniform(0, 1, rip_n) > 0.75) * np.exp(-rt * 6)
    x = stretch * 0.5
    place(x, rip, 0.38, 1.0)
    place(x, bubbles(1.0, 70, 250, 1400), 0.45, 0.8)
    place(x, water(int(0.8 * SR)) * np.exp(-t_of(int(0.8 * SR)) * 3), 0.42, 0.5)
    save("cocoon_burst", fade(murk(x, 0.25), 0.002, 0.3), 0.9)


def main():
    trench_idle()
    trench_screech()
    trench_hurt()
    trench_death()
    trench_bite()
    trench_ambience()
    trench_territory()
    trench_raid()
    cocoon_burst()


if __name__ == "__main__":
    main()
