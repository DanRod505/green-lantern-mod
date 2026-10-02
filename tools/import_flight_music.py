#!/usr/bin/env python3
"""
Imports a song as the "Lanterns" flight theme.

Writes two Ogg Vorbis files used by the game:

* music/lanterns_theme_full.ogg   - the whole song (played from the start when you fly fast)
* music/lanterns_theme_climax.ogg - the song from the climax onwards (the game jumps here,
                                    with a crossfade, the moment you break the sound barrier)

Usage:
    pip install numpy soundfile
    python tools/import_flight_music.py <song.mp3|wav|ogg|flac> [climax_seconds]

If climax_seconds is omitted it is detected automatically: the strongest onset (spectral
flux) in the loudest part of the song.
"""
import os
import sys

import numpy as np
import soundfile as sf

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "greenlantern", "sounds", "music")
PEAK = 0.89  # about -1 dBFS, headroom for the Vorbis encoder


def detect_climax(mono, sr):
    """Strongest onset inside the loudest 30 seconds of the song."""
    hop, n = sr // 100, 2048
    win = np.hanning(n)
    rms = np.array([np.sqrt(np.mean(mono[i:i + sr] ** 2)) for i in range(0, len(mono) - sr, sr)])
    # Loudest 30 s window; the climax starts at (or a little before) it.
    loud = int(np.argmax(np.convolve(rms, np.ones(30), "valid")))
    start, end = max(0, loud - 20), loud + 30
    seg = mono[start * sr:end * sr]
    frames = np.array([np.abs(np.fft.rfft(seg[i:i + n] * win)) for i in range(0, len(seg) - n, hop)])
    flux = np.maximum(0, np.diff(np.log1p(frames), axis=0)).sum(1)
    return start + (int(np.argmax(flux)) + 1) * hop / sr


def trim_silence(data, threshold=1e-3):
    loud = np.where(np.abs(data).max(axis=1) > threshold)[0]
    return data[loud[0]:loud[-1] + 1] if len(loud) else data


def write(path, data, sr):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with sf.SoundFile(path, "w", sr, data.shape[1], format="OGG", subtype="VORBIS") as f:
        # Written in blocks: libsndfile's Vorbis encoder can crash on very large single writes.
        for i in range(0, len(data), 16384):
            f.write(data[i:i + 16384].astype(np.float32))
    print("wrote", os.path.relpath(path), f"{len(data) / sr:.1f}s")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    data, sr = sf.read(sys.argv[1], always_2d=True)
    if data.shape[1] == 1:
        data = np.repeat(data, 2, axis=1)
    data = trim_silence(data[:, :2])
    data = data * (PEAK / np.max(np.abs(data)))
    climax = float(sys.argv[2]) if len(sys.argv) > 2 else detect_climax(data.mean(axis=1), sr)
    print(f"climax at {int(climax // 60)}:{climax % 60:05.2f}")

    # Short fades so neither file starts or ends with a click.
    fade = int(0.01 * sr)
    full = data.copy()
    full[-fade:] *= np.linspace(1, 0, fade)[:, None]
    cut = data[max(0, int((climax - 0.02) * sr)):].copy()
    cut[:fade] *= np.linspace(0, 1, fade)[:, None]
    cut[-fade:] *= np.linspace(1, 0, fade)[:, None]

    write(os.path.join(OUT, "lanterns_theme_full.ogg"), full, sr)
    write(os.path.join(OUT, "lanterns_theme_climax.ogg"), cut, sr)
    print(f"Set CLIMAX_SECONDS = {climax - 0.02:.2f} in client/flight/FlightAudio.java if it changed.")


if __name__ == "__main__":
    main()
