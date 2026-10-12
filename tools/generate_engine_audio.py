#!/usr/bin/env python3
"""
Sounds of the character engine (presets batch 5, version 2.5.0; batch 6, 2.6.0): a set for each of
the 15 classes of ability (3 variations each, the game picks one at random), the accent of the
supreme abilities and the sound of each of the 15 transformations of the catalog. Everything is synthesized here (tools/herolib/audio.py): no samples,
recordings, voices or existing music.

Files: assets/greenlantern/sounds/engine/<class>_<1..3>.ogg and engine/transform_<id>.ogg.

Usage:  pip install numpy scipy soundfile
        python3 tools/generate_engine_audio.py            (all of them)
        python3 tools/generate_engine_audio.py natureza   (only the named ones; the others come out the same)
"""
from herolib.audio import (bell, choir_note, pluck, expsweep, fade, filt, impact, mono_reverb, noise, np, ns, place, save, seed, sine, sizzle,
                           sparkle, sub_boom, t_of, whoosh, zap)

SR = 44100


def env(n, attack=0.005, decay=6.0):
    t = t_of(n)
    return np.minimum(1, t / attack) * np.exp(-t * decay)


def tone(f0, f1, dur, decay=6.0):
    n = ns(dur)
    return sine(expsweep(f0, f1, n), n) * env(n, 0.003, decay)


def metal(f, dur, vel=1.0):
    """A metallic clank: inharmonic partials that die fast."""
    n = ns(dur)
    t = t_of(n)
    x = sum(np.sin(2 * np.pi * f * r * t) * g * np.exp(-t * d) for r, g, d in ((1, 1, 14), (2.41, 0.6, 18), (3.93, 0.4, 24), (5.7, 0.25, 30)))
    return vel * x * np.minimum(1, t / 0.001)


def mix(dur, *parts):
    x = np.zeros(ns(dur))
    for part, at, gain in parts:
        place(x, part, at, gain)
    return x


# ---- the 10 classes of ability -----------------------------------------------------------------------

def golpe(v):  # a punch: thump and a slap of air
    return mix(0.45, (impact(0.4, 1.0), 0, 1.0), (filt(noise(ns(0.08)), "bandpass", [800 + 200 * v, 3500]) * env(ns(0.08), 0.001, 40), 0, 0.6))


def disparo(v):  # a shot: a falling energy zap with a little air
    return mix(0.5, (zap(0.32, 2000 + 300 * v, 260), 0, 1.0), (whoosh(0.3, 1500, 6000), 0.02, 0.35))


def explosao(v):  # a blast: a deep boom with crackle
    n = ns(1.2)
    crack = sizzle(n, 800, 5000) * np.exp(-t_of(n) * 4)
    return mix(1.3, (sub_boom(1.2, 1.0), 0, 1.0), (crack, 0, 0.5), (impact(0.6, 0.8), 0, 0.8 + 0.1 * v))


def mobilidade(v):  # mobility: a rush of air
    return mix(0.6, (whoosh(0.55, 300 + 80 * v, 5000), 0, 1.0), (tone(200, 420, 0.4, 5), 0.05, 0.2))


def defesa(v):  # a shield: a rising hum and a glassy ring
    hum = tone(140 + 15 * v, 220, 0.7, 2.5)
    return mix(0.9, (hum, 0, 0.8), (bell(880 + 110 * v, 0.7, 0.6), 0.08, 0.5), (filt(noise(ns(0.5)), "bandpass", [2000, 6000]) * env(ns(0.5), 0.05, 5), 0, 0.15))


def controle(v):  # control: a warbling tone that bends down
    n = ns(0.8)
    t = t_of(n)
    wob = sine(expsweep(700 + 60 * v, 300, n) * (1 + 0.08 * np.sin(2 * np.pi * 9 * t)), n) * env(n, 0.02, 3)
    return mix(0.9, (wob, 0, 0.9), (sparkle(0.6, 1200, 6), 0.1, 0.3))


def invocacao(v):  # a summon: a swell and a chord that opens
    n = ns(1.0)
    swell = filt(noise(n), "bandpass", [300, 1800]) * np.sin(np.pi * t_of(n) / 1.0) ** 2
    chord = sum(tone(f, f, 0.9, 2.5) for f in (220 * 2 ** (v / 12), 277 * 2 ** (v / 12), 330 * 2 ** (v / 12)))
    return mix(1.1, (swell, 0, 0.5), (chord, 0.15, 0.5))


def evasao(v):  # evasion: a quick swish down
    return mix(0.35, (whoosh(0.28, 600, 7000 - 500 * v, rise=False), 0, 1.0))


def fortalecimento(v):  # empowering: a rising shimmer
    return mix(1.0, (tone(300 + 30 * v, 900, 0.8, 1.8), 0, 0.6), (sparkle(0.8, 1600, 10), 0.1, 0.5), (whoosh(0.6, 400, 3000), 0, 0.3))


def suprema(v):  # the accent of a supreme ability: a choir hit over a boom
    choir = sum(choir_note(m + v, 1.4, 0.9) for m in (57, 61, 64))
    return mix(1.8, (sub_boom(1.6, 1.0), 0, 0.9), (choir, 0.05, 0.7), (impact(0.7, 1.0), 0, 0.7))


def coleta_e_terreno(v):  # gathering and terrain: earth crunching and stones tumbling
    n = ns(0.6)
    crunch = filt(noise(n), "bandpass", [150 + 40 * v, 1400]) * env(n, 0.004, 9)
    return mix(0.7, (crunch, 0, 1.0), (impact(0.35, 0.7), 0, 0.6), (metal(1300 + 150 * v, 0.15, 0.3), 0.12, 0.5), (metal(1700 + 120 * v, 0.12, 0.25), 0.22, 0.4))


def construcao(v):  # building: blocks set down one after another, a clack and a ring
    clack = filt(noise(ns(0.06)), "bandpass", [900, 3000]) * env(ns(0.06), 0.001, 50)
    return mix(0.8, (clack, 0, 0.8), (clack, 0.14, 0.7), (clack, 0.28, 0.9), (bell(660 + 80 * v, 0.5, 0.5), 0.3, 0.5), (impact(0.3, 0.5), 0.28, 0.4))


def transmutacao(v):  # transmutation: a tone that bends up and turns into a shimmer
    n = ns(0.9)
    t = t_of(n)
    bend = sine(expsweep(320 + 40 * v, 980, n) * (1 + 0.05 * np.sin(2 * np.pi * 6 * t)), n) * env(n, 0.03, 2.8)
    return mix(1.0, (bend, 0, 0.7), (sparkle(0.6, 2200, 12), 0.3, 0.5), (sizzle(ns(0.4), 3000, 8000) * env(ns(0.4), 0.05, 6), 0.2, 0.2))


def natureza(v):  # nature: leaves rustling and a soft wooden chime
    n = ns(0.9)
    rustle = filt(noise(n), "bandpass", [1800, 7000]) * np.sin(np.pi * t_of(n) / 0.9) ** 2
    chime = sum(tone(f, f, 0.7, 4.0) for f in (392 * 2 ** (v / 12), 523 * 2 ** (v / 12)))
    return mix(1.0, (rustle, 0, 0.45), (chime, 0.1, 0.45), (pluck(67 + v, 0.5, 0.6), 0.25, 0.4))


def deteccao(v):  # detection: a sonar ping and its echo
    ping = tone(1250 + 90 * v, 1180 + 90 * v, 0.6, 5)
    return mix(1.2, (ping, 0, 0.8), (ping, 0.45, 0.35), (whoosh(0.3, 2000, 6000), 0, 0.15))


def logistica(v):  # logistics: a pull of air and a pop as things arrive
    pop = tone(500 + 60 * v, 900, 0.12, 25)
    return mix(0.7, (whoosh(0.4, 400, 3000), 0, 0.7), (pop, 0.32, 0.8), (pop, 0.42, 0.5))


CLASSES = {"golpe": golpe, "disparo": disparo, "explosao": explosao, "mobilidade": mobilidade, "defesa": defesa, "controle": controle,
           "invocacao": invocacao, "evasao": evasao, "fortalecimento": fortalecimento, "suprema": suprema}
# The 6 classes of the world that came with the new model of abilities (2.6.0): made after everything
# else, so the sounds made before them stay the same.
WORLD_CLASSES = {"coleta_e_terreno": coleta_e_terreno, "construcao": construcao, "transmutacao": transmutacao, "natureza": natureza,
                 "deteccao": deteccao, "logistica": logistica}

# ---- the 15 transformations -------------------------------------------------------------------------


def thunder():
    n = ns(2.2)
    t = t_of(n)
    rumble = filt(noise(n), "lowpass", 260) * 2.5 * (0.6 + 0.4 * np.sin(2 * np.pi * 2.1 * t)) * np.exp(-t / 1.0)
    crack = filt(noise(ns(0.12)), "highpass", 1500) * np.exp(-np.arange(ns(0.12)) / ns(0.03))
    return mix(2.3, (rumble, 0, 1.0), (sub_boom(2.0, 0.8), 0, 0.6), (crack, 0, 1.4))


def ring_hum():
    n = ns(2.0)
    t = t_of(n)
    hum = sum(np.sin(2 * np.pi * f * t) * g for f, g in ((110, 1), (220, 0.5), (330, 0.3), (440.5, 0.2)))
    hum *= np.minimum(1, t / 0.6) * np.minimum(1, (2.0 - t) / 0.4)
    oath = sum(choir_note(m, 1.2, 0.6) for m in (52, 59, 64))
    return mix(2.1, (hum, 0, 0.6), (oath, 0.7, 0.6), (bell(1320, 0.8, 0.4), 1.2, 0.5))


def krypton_sun():
    choir = sum(choir_note(m, 1.6, 0.8) for m in (60, 64, 67, 72))
    return mix(2.1, (choir, 0, 0.7), (whoosh(1.0, 200, 3000), 1.0, 0.6), (bell(1047, 0.8, 0.5), 1.0, 0.4))


def speed_blur():
    n = ns(1.6)
    zz = sine(expsweep(200, 1400, n), n) * np.minimum(1, t_of(n) / 0.4) * np.exp(-t_of(n) * 0.8)
    return mix(1.8, (zz, 0, 0.5), (sizzle(n, 2500, 9000) * 0.5, 0, 0.6), (zap(0.3, 2400, 300), 1.3, 0.8))


def shadows():
    flaps = np.zeros(ns(1.8))
    for i in range(14):
        place(flaps, filt(noise(ns(0.06)), "bandpass", [300, 1500]) * env(ns(0.06), 0.005, 50), 0.05 + i * 0.11, 0.5 + 0.3 * (i % 2))
    cape = whoosh(0.5, 200, 1500, rise=False)
    return mix(2.0, (flaps, 0, 1.0), (cape, 1.4, 0.8))


def armor():
    x = np.zeros(ns(2.2))
    for i in range(5):
        place(x, metal(900 + 120 * i, 0.3), 0.15 + i * 0.3, 0.7)
    motor = sine(expsweep(60, 180, ns(0.8)), ns(0.8)) * np.minimum(1, t_of(ns(0.8)) / 0.3) * 0.6
    place(x, motor + sizzle(ns(0.8), 3000, 8000) * 0.1, 1.4, 0.8)
    return x


def mystic():
    n = ns(2.0)
    whisper = filt(noise(n), "bandpass", [1800, 4500]) * (0.5 + 0.5 * np.sin(2 * np.pi * 3 * t_of(n))) * np.minimum(1, t_of(n) / 0.5)
    return mix(2.2, (whisper * np.exp(-t_of(n) * 0.6), 0, 0.5), (bell(440, 1.8, 0.9), 0.9, 0.8), (bell(660, 1.4, 0.5), 1.1, 0.5))


def divine():
    choir = sum(choir_note(m, 1.5, 0.8) for m in (55, 59, 62, 67))
    return mix(2.1, (choir, 0, 0.7), (metal(1300, 0.6), 1.2, 0.6), (metal(1600, 0.5), 1.35, 0.5))


def tide():
    n = ns(2.0)
    t = t_of(n)
    surge = filt(noise(n), "lowpass", 900) * np.minimum(1, t / 0.9) * np.exp(-np.maximum(0, t - 1.2) * 4)
    crash = filt(noise(ns(0.9)), "bandpass", [500, 6000]) * env(ns(0.9), 0.01, 4)
    return mix(2.2, (surge, 0, 0.8), (crash, 1.1, 0.9))


def element():
    n = ns(1.8)
    roar = filt(noise(n), "bandpass", [200, 2500]) * np.sin(np.pi * t_of(n) / 1.8)
    return mix(2.0, (roar, 0, 0.8), (sizzle(n, 1500, 7000) * 0.3, 0, 0.6), (impact(0.5, 0.6), 1.4, 0.6))


def beast():
    n = ns(1.2)
    t = t_of(n)
    growl = filt(sine(expsweep(90, 60, n), n) * 0.6 + np.sign(sine(expsweep(90, 60, n), n)) * 0.4, "lowpass", 900)
    growl *= np.minimum(1, t / 0.15) * np.exp(-t * 1.2) * (1 + 0.3 * np.sin(2 * np.pi * 23 * t))
    x = np.zeros(ns(2.0))
    for i in range(6):
        place(x, filt(noise(ns(0.03)), "highpass", 2000) * env(ns(0.03), 0.001, 80), 0.1 + i * 0.09, 0.6)
    place(x, growl, 0.7, 1.0)
    return x


def boom_tube():
    return mix(2.1, (sub_boom(1.8, 1.0), 0.1, 1.0), (tone(55, 35, 1.6, 1.5), 0, 0.8), (whoosh(0.8, 150, 1200), 0.6, 0.5))


def arsenal():
    x = np.zeros(ns(1.8))
    place(x, metal(2200, 0.15), 0.1, 0.6)
    place(x, metal(1800, 0.2), 0.25, 0.7)
    for i in range(3):
        place(x, whoosh(0.18, 1500, 8000), 0.5 + i * 0.22, 0.6)
    place(x, metal(1400, 0.3), 1.3, 0.9)
    return x


def show():
    n = ns(0.12)
    x = np.zeros(ns(2.0))
    for i in range(6):  # a laugh: short bursts of a falling buzz
        f = 320 - i * 18
        burst = np.sign(sine(np.full(n, f), n)) * 0.3 + sine(np.full(n, f * 2), n) * 0.3
        place(x, filt(burst, "bandpass", [500, 2600]) * env(n, 0.01, 25), 0.1 + i * 0.13, 0.7)
    horn = sum(sine(np.full(ns(0.5), f), ns(0.5)) for f in (392, 494, 587)) * env(ns(0.5), 0.02, 3)
    place(x, horn, 1.1, 0.6)
    place(x, sparkle(0.6, 2000, 10), 1.1, 0.4)
    return x


def wings():
    x = np.zeros(ns(1.8))
    for i in range(3):
        place(x, whoosh(0.4, 200, 2000), 0.1 + i * 0.35, 0.7)
    place(x, sparkle(0.8, 2400, 12), 0.6, 0.6)
    return x


TRANSFORMATIONS = {"raio": thunder, "anel": ring_hum, "sol": krypton_sun, "velocidade": speed_blur, "sombras": shadows, "armadura": armor,
                   "mistico": mystic, "divino": divine, "mare": tide, "elemento": element, "fera": beast, "tubo": boom_tube,
                   "arsenal": arsenal, "espetaculo": show, "asas": wings}


def classes(table, only):
    for name, make in table.items():
        for v in range(3):
            sound = fade(mono_reverb(make(v), 0.8, 0.12), 0.002, 0.05)
            if not only or name in only:
                save("engine/" + name + "_" + str(v + 1), sound, 0.8)


def main(only=()):
    seed(2500)
    classes(CLASSES, only)
    for name, make in TRANSFORMATIONS.items():
        sound = fade(mono_reverb(make(), 1.2, 0.18), 0.005, 0.15)
        if not only or "transform_" + name in only:
            save("engine/transform_" + name, sound, 0.85)
    classes(WORLD_CLASSES, only)


if __name__ == "__main__":
    import sys
    main(tuple(sys.argv[1:]))
