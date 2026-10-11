#!/usr/bin/env python3
"""
Audio of Shazam: the magic word with its thunder, a sound per power and an ORIGINAL two-layer theme
(adventurous and a little playful: pizzicato strings and a marching snare in the base, heroic brass,
bells and a thunder roll in the peak). Everything is synthesized here (tools/herolib/audio.py): no
samples, recordings, voices or existing music. The "Shazam!" is a synthesized vowel shout, not a voice.

Usage:  pip install numpy scipy soundfile
        python3 tools/generate_shazam_audio.py
"""
from herolib.audio import (Theme, bass, bell, brass_note, expsweep, choir_note, crash, fade, filt, impact, kick, midi_hz, mono_reverb, noise, np, ns,
                           place, save, seed, sine, sizzle, snare, sparkle, strings_note, sub_boom, swell_cymbal, timpani, whoosh, zap)

seed(644)


def thunder(dur=2.4, vel=1.0):
    """A crack of lightning and its rolling thunder: a bright snap, then low rumbling noise that comes and goes."""
    n = ns(dur)
    t = np.arange(n) / 44100
    crack = filt(noise(ns(0.12)), "highpass", 1500) * np.exp(-np.arange(ns(0.12)) / ns(0.03))
    rumble = filt(noise(n), "lowpass", 260) * 2.5
    roll = (0.55 + 0.45 * np.sin(2 * np.pi * 2.3 * t) * np.sin(2 * np.pi * 0.7 * t)) * np.exp(-t / (dur * 0.45))
    x = rumble * roll
    place(x, sub_boom(dur, 0.8), 0.0, 0.6)
    place(x, crack, 0.0, 1.4)
    return x * vel


def shout(dur=0.55):
    """A synthesized vowel shout ("sha-ZAM"), two syllables of formant-shaped buzz: no recorded voice."""
    def syllable(d, f0, f1, formants):
        m = ns(d)
        wave = sine(expsweep(f0, f1, m), m)
        src = wave * 0.2 + np.sign(wave) * 0.3
        out = sum(filt(src, "bandpass", (f * 0.85, f * 1.15)) * g for f, g in formants)
        return fade(out, 0.01, d * 0.4)
    sha = filt(noise(ns(0.12)), "bandpass", (2200, 5200)) * 0.5
    a1 = syllable(0.14, 200, 230, [(750, 1.0), (1150, 0.6), (2500, 0.2)])
    zam = syllable(dur - 0.22, 260, 190, [(800, 1.0), (1250, 0.7), (2600, 0.25)])
    z = filt(noise(ns(0.05)), "bandpass", (3500, 7000)) * 0.4
    x = np.zeros(ns(dur))
    place(x, sha, 0.0)
    place(x, a1, 0.08)
    place(x, z, 0.2)
    place(x, zam, 0.22)
    return x


def suit_up():
    x = np.zeros(ns(2.8))
    place(x, shout(0.6), 0.0, 0.9)
    place(x, thunder(2.4), 0.45, 1.0)
    place(x, sparkle(1.2, 2200, 24), 0.55, 0.35)
    place(x, sizzle(ns(0.6)), 0.45, 0.4)
    save("shazam_suit_up", fade(mono_reverb(x, 1.6, 0.3), 0.003, 0.4), 0.85)


def suit_down():
    x = np.zeros(ns(2.2))
    place(x, thunder(2.0, 0.9), 0.0)
    place(x, whoosh(0.8, 3000, 300, rise=False), 0.1, 0.4)
    save("shazam_suit_down", fade(mono_reverb(x, 1.3, 0.25), 0.003, 0.3), 0.85)


def zeus_lightning():
    x = zap(0.35, 2400, 300) * 0.8 + sizzle(ns(0.35)) * 0.6
    place(x, filt(noise(ns(0.05)), "highpass", 2000), 0.0, 0.8)
    save("shazam_zeus_lightning", fade(mono_reverb(x, 0.6, 0.15), 0.001, 0.08), 0.85)


def lightning_hit():
    x = sizzle(ns(0.3), 2000, 9000) * 0.8 + impact(0.3, 0.5)[:ns(0.3)] * 0.5
    save("shazam_lightning_hit", fade(x, 0.001, 0.1), 0.8)


def thunder_call():
    x = np.zeros(ns(3.0))
    place(x, sizzle(ns(0.3)), 0.0, 0.5)
    place(x, thunder(2.8, 1.0), 0.05)
    place(x, impact(0.9, 1.0), 0.05, 0.7)
    save("shazam_thunder_call", fade(mono_reverb(x, 1.8, 0.3), 0.002, 0.4), 0.95)


def mercury_dash():
    x = whoosh(0.5, 400, 7000) + sizzle(ns(0.5)) * 0.25
    save("shazam_mercury_dash", fade(mono_reverb(x, 0.7, 0.2), 0.002, 0.12), 0.85)


def hercules_slam():
    x = impact(1.2, 1.0) + timpani(38, 1.2, 1.0)[:ns(1.2)] * 0.6
    place(x, filt(noise(ns(0.6)), "lowpass", 900) * np.exp(-np.arange(ns(0.6)) / ns(0.2)), 0.02, 0.6)
    save("shazam_hercules_slam", fade(mono_reverb(x, 1.2, 0.2), 0.001, 0.3), 0.95)


def achilles_roar():
    """A battle cry: a low brass blare with a growl of noise and a rising choir."""
    d = 1.1
    x = brass_note(43, d, 1.0, 1.4) + brass_note(50, d, 0.8, 1.2) + choir_note(55, d, 0.5)
    growl = filt(noise(ns(d)), "bandpass", (200, 900)) * (0.5 + 0.5 * np.sin(2 * np.pi * 28 * np.arange(ns(d)) / 44100))
    x = x[:ns(d)] + growl * 0.4
    save("shazam_achilles_roar", fade(mono_reverb(x, 1.2, 0.25), 0.02, 0.3), 0.9)


def atlas_on():
    x = np.zeros(ns(1.0))
    place(x, sub_boom(1.0, 1.0), 0.0)
    place(x, bass(31, 1.0, 0.8), 0.0, 0.5)
    place(x, bell(midi_hz(55), 0.9, 0.3), 0.05)
    save("shazam_atlas_on", fade(mono_reverb(x, 1.0, 0.2), 0.01, 0.3), 0.85)


def atlas_off():
    x = np.zeros(ns(0.6))
    place(x, sub_boom(0.6, 0.6), 0.0)
    place(x, whoosh(0.6, 800, 200, rise=False), 0.0, 0.3)
    save("shazam_atlas_off", fade(x, 0.01, 0.2), 0.7)


def solomon_on():
    x = np.zeros(ns(1.2))
    for i, m in enumerate([76, 79, 83, 88]):
        place(x, bell(midi_hz(m), 0.9, 0.6), i * 0.08)
    save("shazam_solomon_on", fade(mono_reverb(x, 1.4, 0.35), 0.002, 0.3), 0.75)


def solomon_off():
    x = np.zeros(ns(0.9))
    for i, m in enumerate([83, 79, 76]):
        place(x, bell(midi_hz(m), 0.7, 0.5), i * 0.07)
    save("shazam_solomon_off", fade(mono_reverb(x, 1.0, 0.3), 0.002, 0.25), 0.65)


def magic_full():
    x = np.zeros(ns(0.9))
    place(x, sparkle(0.8, 1800, 14), 0.0)
    place(x, bell(midi_hz(84), 0.8, 0.5), 0.0)
    save("shazam_magic_full", fade(mono_reverb(x, 1.0, 0.3), 0.002, 0.2), 0.7)


theme = Theme("shazam_theme", bpm=132, bars=16)

# D major with a playful flat seventh: D, Bb, C, D / G, A, F, A
CHORDS = [(50, 54, 57), (46, 50, 53), (48, 52, 55), (50, 54, 57), (43, 47, 50), (45, 49, 52), (41, 45, 48), (45, 49, 52)] * 2


def base(track, offset):
    """Pizzicato strings bouncing on every eighth, a marching snare and a walking bass."""
    for bar, chord in enumerate(CHORDS):
        at = offset + bar * theme.bar
        for e in range(8):
            note = chord[[0, 1, 2, 1][e % 4]] + 12
            track.add(strings_note(note, theme.beat * 0.4, 0.55, spiccato=True), at + e * theme.beat / 2, 0.35, -0.3 + 0.6 * (e % 2))
        track.add(bass(chord[0] - 12, theme.beat * 1.8, 0.8), at, 0.5)
        track.add(bass(chord[0] - 5, theme.beat * 1.8, 0.7), at + 2 * theme.beat, 0.45)
        for beat in range(4):
            track.add(kick(0.8), at + beat * theme.beat, 0.4 if beat % 2 == 0 else 0.2)
            track.add(snare(0.5), at + beat * theme.beat + theme.beat / 2, 0.18, 0.2)
        track.add(snare(0.9), at + 3 * theme.beat, 0.3, 0.2)
        track.add(snare(0.6), at + 3.75 * theme.beat, 0.2, 0.2)


MELODY = [  # (beat, midi, length in beats): the heroic call, rising like a bolt
    (0, 62, 1), (1, 66, 0.5), (1.5, 69, 0.5), (2, 74, 2), (4, 72, 1), (5, 70, 1), (6, 69, 2),
    (8, 67, 1), (9, 69, 0.5), (9.5, 70, 0.5), (10, 72, 1.5), (11.5, 74, 0.5), (12, 76, 3), (15, 74, 1),
]


def peak(track, offset):
    """Heroic brass with the melody twice, bells answering and timpani and thunder marking the bars."""
    for half in range(2):
        start = offset + half * 8 * theme.bar
        for rep in range(2):
            base_at = start + rep * 4 * theme.bar
            for beat, m, length in MELODY:
                track.add(brass_note(m, theme.beat * length * 0.95, 0.9, 1.2), base_at + beat * theme.beat, 0.45, -0.15)
                track.add(brass_note(m - 12, theme.beat * length * 0.95, 0.6), base_at + beat * theme.beat, 0.25, 0.15)
            for beat in (2, 6, 12):
                track.add(bell(midi_hz(MELODY[0][1] + 24 + (beat % 5)), 1.2, 0.5), base_at + (beat + 0.5) * theme.beat, 0.12, 0.4)
        for bar in range(8):
            at = start + bar * theme.bar
            track.add(timpani(38, 1.0, 0.8), at, 0.35)
            if bar % 4 == 3:
                track.add(timpani(45, 0.6, 0.7), at + 3 * theme.beat, 0.3)
                track.add(timpani(45, 0.6, 0.7), at + 3.5 * theme.beat, 0.3)
        track.add(crash(2.5, 0.8), start, 0.25)
        track.add(swell_cymbal(theme.bar, 0.6), start + 7 * theme.bar, 0.2)
        track.add(thunder(3.0, 0.7), start + 4 * theme.bar, 0.35)


if __name__ == "__main__":
    suit_up()
    suit_down()
    zeus_lightning()
    lightning_hit()
    thunder_call()
    mercury_dash()
    hercules_slam()
    achilles_roar()
    atlas_on()
    atlas_off()
    solomon_on()
    solomon_off()
    magic_full()
    theme.render(base, peak)
