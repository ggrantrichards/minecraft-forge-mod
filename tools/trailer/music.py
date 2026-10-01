"""The trailer's score: an original ambient piece in D minor, synthesised from scratch with numpy
(no samples, no borrowed music), in the spirit of the game's own soundtrack - soft piano over warm
pads, building to a swell for the title.

    python3 tools/trailer/music.py out.wav [length_seconds]

compose.py calls score(sections) with the trailer's own timings, so the build-ups land on the cuts.
"""
import sys
import wave

import numpy as np

RATE = 44100
A4 = 440.0
NAMES = {"C": -9, "D": -7, "E": -5, "F": -4, "G": -2, "A": 0, "B": 2}


def hz(note):
    """'D4', 'Bb3', 'F#5' -> frequency."""
    name, octave = note[:-1], int(note[-1])
    semis = NAMES[name[0]] + (1 if "#" in name else -1 if "b" in name else 0) + (octave - 4) * 12
    return A4 * 2 ** (semis / 12)


# i - VI - III - VII, then the lift for the finale: iv - VI - III - V
CHORDS = {
    "Dm": ["D3", "A3", "D4", "F4", "A4"],
    "Bb": ["Bb2", "F3", "Bb3", "D4", "F4"],
    "F": ["F2", "C3", "F3", "A3", "C4"],
    "C": ["C3", "G3", "C4", "E4", "G4"],
    "Gm": ["G2", "D3", "G3", "Bb3", "D4"],
    "A": ["A2", "E3", "A3", "C#4", "E4"],
    "Dadd9": ["D3", "A3", "D4", "E4", "F4", "A4"],
}
VERSE = ["Dm", "Bb", "F", "C"]
LIFT = ["Gm", "Bb", "F", "A"]
BAR = 4.0                       # seconds per chord (60 bpm, four beats)


def env(n, attack, release):
    e = np.ones(n)
    a = min(n, int(attack * RATE))
    r = min(n - a, int(release * RATE))
    if a:
        e[:a] = np.linspace(0, 1, a) ** 2
    if r:
        e[n - r:] *= np.linspace(1, 0, r) ** 2
    return e


def pad(freqs, dur, bright, attack=1.6, release=2.2):
    """Detuned, softly filtered saws: a warm string-like pad."""
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    out = np.zeros(n)
    for f in freqs:
        for cents in (-7, 0, 6):
            ff = f * 2 ** (cents / 1200)
            phase = np.random.rand() * 6.28
            for h in range(1, 16):
                if ff * h > 9000:
                    break
                amp = (1 / h) / np.sqrt(1 + (ff * h / bright) ** 4)
                out += amp * np.sin(2 * np.pi * ff * h * t + phase * h)
    out *= env(n, attack, release) / (len(freqs) * 3)
    # a slow breath in the swell
    out *= 0.85 + 0.15 * np.sin(2 * np.pi * t / dur * 0.5)
    return out


def piano(f, dur=3.5, vel=1.0):
    """A soft felt piano: a few slightly stretched partials, each dying away at its own rate."""
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    out = np.zeros(n)
    for h, (amp, decay) in enumerate([(1.0, 1.6), (0.45, 1.0), (0.22, 0.6), (0.12, 0.4), (0.06, 0.25)], start=1):
        fh = f * h * (1 + 0.0004 * h * h)
        out += amp * np.exp(-t / decay) * np.sin(2 * np.pi * fh * t)
    out *= np.minimum(1, t / 0.004) * vel * 0.32
    return out


def bell(f, dur=4.0, vel=1.0):
    """A celesta-like star: a pure tone with a bright, quickly fading overtone."""
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    out = np.sin(2 * np.pi * f * t) * np.exp(-t / 1.4) + 0.4 * np.sin(2 * np.pi * f * 4.01 * t) * np.exp(-t / 0.25)
    return out * np.minimum(1, t / 0.002) * vel * 0.12


def bass(f, dur):
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    out = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(4 * np.pi * f * t)
    return out * env(n, 0.6, 1.2) * 0.30


def thump(vel=1.0):
    """A low, soft drum: a falling sine with a breath of noise."""
    n = int(0.9 * RATE)
    t = np.arange(n) / RATE
    f = 46 + 50 * np.exp(-t / 0.05)
    body = np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.exp(-t / 0.28)
    noise = np.convolve(np.random.randn(n), np.ones(24) / 24, mode="same") * np.exp(-t / 0.03) * 0.25
    return (body + noise) * vel * 0.55


def boom():
    """The hit under the title: a deep drum and a long rumble."""
    n = int(6.0 * RATE)
    t = np.arange(n) / RATE
    f = 34 + 70 * np.exp(-t / 0.08)
    body = np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.exp(-t / 1.6)
    rumble = np.convolve(np.random.randn(n), np.ones(400) / 400, mode="same") * np.exp(-t / 1.2) * 0.6
    return (body + rumble) * 0.8


def riser(dur):
    """Air rushing upward into the title."""
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    noise = np.random.randn(n)
    out = np.zeros(n)
    # band-limited noise whose band climbs: smooth it less and less as it rises
    for k, w in enumerate([240, 120, 60, 30, 15]):
        seg = np.convolve(noise, np.ones(w) / w, mode="same")
        weight = np.clip((t / dur) * 5 - k, 0, 1) * np.clip(k + 1 - (t / dur) * 5, 0, 1)
        out += seg * weight
    return out * (t / dur) ** 2 * 0.5


def place(track, sound, at, pan=0.0):
    i = int(at * RATE)
    if i >= track.shape[1]:
        return
    sound = sound[: track.shape[1] - i]
    track[0, i:i + len(sound)] += sound * np.sqrt(0.5 * (1 - pan))
    track[1, i:i + len(sound)] += sound * np.sqrt(0.5 * (1 + pan))


def reverb(track, seconds=3.2, wet=0.38):
    """Convolution with a decaying-noise hall, a little different on each side."""
    n = int(seconds * RATE)
    t = np.arange(n) / RATE
    out = np.empty_like(track)
    for ch in range(2):
        ir = np.random.randn(n) * np.exp(-t / (seconds / 4.5))
        ir[: int(0.012 * RATE)] = 0        # a short pre-delay
        ir /= np.sqrt(np.sum(ir ** 2))
        size = 1 << int(np.ceil(np.log2(track.shape[1] + n)))
        wetsig = np.fft.irfft(np.fft.rfft(track[ch], size) * np.fft.rfft(ir, size), size)[: track.shape[1]]
        out[ch] = track[ch] * (1 - wet) + wetsig * wet * 1.6
    return out


def score(length, sections):
    """sections: dict of times (seconds) - 'bloom' (the pads and arpeggios open up), 'pulse' (the drums
    come in), 'lift' (the finale's chords), 'title' (the hit under the logo)."""
    np.random.seed(7)
    track = np.zeros((2, int((length + 1) * RATE)))
    bloom, pulse, lift, title = sections["bloom"], sections["pulse"], sections["lift"], sections["title"]

    # chords: the verse loops until the lift, then the lift's four, then the last chord rings under the title
    bars = []
    at = 0.0
    while at < lift - 0.01:
        bars.append((at, VERSE[len(bars) % 4]))
        at += BAR
    lift_bar = (title - at) / 4
    for k in range(4):
        bars.append((at, LIFT[k]))
        at += lift_bar
    bars.append((title, "Dadd9"))

    for i, (start, name) in enumerate(bars):
        end = bars[i + 1][0] if i + 1 < len(bars) else length
        dur = end - start + 2.0
        last = name == "Dadd9"
        bright = 700 if start < bloom else 1300 if start < lift else 2200
        level = 0.28 if start < bloom else 0.5 if start < pulse else 0.62 if start < lift else 1.0
        if last:
            dur, bright, level = length - start + 0.5, 1800, 1.0
        place(track, pad([hz(n) for n in CHORDS[name]], dur, bright, attack=0.8 if start >= lift else 1.6,
                         release=3.5 if last else 2.0) * level, start)
        root = hz(CHORDS[name][0]) / 2
        if start >= 4.0:
            place(track, bass(root, dur) * (0.35 if start < bloom else 0.7 if start < lift else 1.0), start)

        # the piano: one note a beat before the bloom, then a gentle arpeggio, then the melody in the finale
        notes = CHORDS[name][1:]
        beat = (end - start) / 4
        if last:
            for k, nn in enumerate(["D4", "A4", "D5", "E5", "F5", "A5"]):
                place(track, piano(hz(nn), 6.0, 0.9 - k * 0.08), start + 0.35 + k * 0.22, pan=-0.3 + k * 0.12)
            continue
        if start < bloom:
            if start >= 2.0:
                place(track, piano(hz(notes[-1]) , 4.0, 0.8), start + 0.05, pan=-0.2)
                place(track, piano(hz(notes[-2]), 4.0, 0.55), start + beat * 2.5, pan=0.2)
        elif start < lift:
            pattern = [0, 2, 3, 1, 3, 2, 3, 1]
            for k in range(8):
                place(track, piano(hz(notes[pattern[k]]) * 2, 2.5, 0.42 if k % 2 else 0.6), start + k * beat / 2,
                      pan=(-0.35 if k % 2 else 0.35))
        else:
            melody = {"Gm": ["D5", "Bb4", "A4", "G4"], "Bb": ["F5", "D5", "C5", "Bb4"],
                      "F": ["A5", "F5", "E5", "C5"], "A": ["E5", "C#5", "A4", "E5"]}[name]
            for k, nn in enumerate(melody):
                place(track, piano(hz(nn), 3.0, 0.85), start + k * beat, pan=0.1)
                place(track, piano(hz(notes[k % len(notes)]), 2.5, 0.35), start + k * beat + beat / 2, pan=-0.3)

        # stars: a few bells drifting above, more as it builds
        if start >= bloom:
            for k in range(2 if start < lift else 4):
                nn = notes[np.random.randint(len(notes))]
                place(track, bell(hz(nn) * 4, vel=0.8), start + np.random.rand() * (end - start),
                      pan=np.random.uniform(-0.8, 0.8))

        # the drums: on the first and third beat, then every beat in the finale
        if start >= pulse:
            hits = 4 if start >= lift else 2
            for k in range(hits):
                place(track, thump(1.0 if k == 0 else 0.6), start + k * (end - start) / hits)

    place(track, riser(3.0), title - 3.0)
    place(track, boom(), title)

    track = reverb(track)
    # fade out at the very end; a short fade in
    n = track.shape[1]
    track[:, : int(0.5 * RATE)] *= np.linspace(0, 1, int(0.5 * RATE))
    tail = int(3.0 * RATE)
    end = int(length * RATE)
    track[:, end - tail:end] *= np.linspace(1, 0, tail) ** 1.5
    track[:, end:] = 0
    track = track[:, :end]
    peak = np.max(np.abs(track))
    return track / peak * 0.89


def write(path, track):
    data = (np.clip(track.T, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data.tobytes())


if __name__ == "__main__":
    length = float(sys.argv[2]) if len(sys.argv) > 2 else 130.0
    write(sys.argv[1], score(length, {"bloom": 14.0, "pulse": 43.0, "lift": length - 30.0, "title": length - 9.0}))
