"""The trailer's soundtrack: the mod's own music (the tracks in assets/astralend/sounds/music and
records - see sounds/credits.txt), and every sound the world made while the trailer was filmed,
played back from the game's own sound files as you'd have heard them from where the camera was.

The game films without a sound device, so ClientTrailer (with TrailerSound) writes the sounds down
instead, in <frames dir>/sounds.txt:

    cam <shot> <frame> <x> <y> <z> <yaw>                       where the camera is on each frame
    sound <shot> <frame> <event> <category> <volume> <pitch> <x> <y> <z>

compose.py calls build() with the edit's timings. The game's sounds are found through the asset index
Gradle downloaded (TRAILER_ASSETS: the folder holding indexes/ and objects/); the mod's own from its
resources. Needs numpy and ffmpeg.
"""
import hashlib
import json
import os
import subprocess
from pathlib import Path

import numpy as np

RATE = 44100
FPS = 24
MOD = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend"
FFMPEG = os.environ.get("FFMPEG", "ffmpeg")
# How loud each kind of sound is in the mix (Minecraft's own sound categories).
CATEGORY = {"master": 1.0, "hostile": 1.0, "player": 0.95, "players": 0.95, "neutral": 0.8, "block": 0.75,
            "blocks": 0.75, "ambient": 0.5, "weather": 0.5, "voice": 1.0, "ui": 0.0, "music": 0.0, "record": 0.0}

_decoded = {}


def decode(path):
    """A sound file as float32 stereo at RATE, shape (2, n)."""
    path = str(path)
    if path not in _decoded:
        raw = subprocess.run([FFMPEG, "-v", "error", "-i", path, "-f", "f32le", "-ac", "2", "-ar", str(RATE), "-"],
                             capture_output=True).stdout
        _decoded[path] = np.frombuffer(raw, np.float32).reshape(-1, 2).T.copy() if raw else np.zeros((2, 0), np.float32)
    return _decoded[path]


class Library:
    """Sound events -> the files they play, from the game's sounds.json and the mod's."""

    def __init__(self):
        self.objects = {}
        self.events = {}
        assets = os.environ.get("TRAILER_ASSETS", "")
        index = None
        if assets and (Path(assets) / "indexes").is_dir():
            for f in sorted((Path(assets) / "indexes").glob("*.json"), key=lambda p: p.stat().st_size, reverse=True):
                index = json.loads(f.read_text())
                break
        if index:
            for key, entry in index.get("objects", {}).items():
                h = entry["hash"]
                self.objects[key] = Path(assets) / "objects" / h[:2] / h
        vanilla = None
        sj = os.environ.get("TRAILER_SOUNDS_JSON", "")
        if sj and os.path.exists(sj) and os.path.getsize(sj) > 0:
            vanilla = json.loads(Path(sj).read_text())
        elif "minecraft/sounds.json" in self.objects:
            vanilla = json.loads(self.objects["minecraft/sounds.json"].read_text())
        for name, entry in (vanilla or {}).items():
            self.events["minecraft:" + name] = entry
        mod = MOD / "sounds.json"
        if mod.exists():
            for name, entry in json.loads(mod.read_text()).items():
                self.events["astralend:" + name] = entry
        print(f"soundtrack: {len(self.objects)} game files indexed, {len(self.events)} sound events known")

    def file(self, name):
        ns, _, path = name.partition(":") if ":" in name else ("minecraft", "", name)
        if ns == "minecraft":
            return self.objects.get(f"minecraft/sounds/{path}.ogg")
        p = MOD / "sounds" / f"{path}.ogg"
        return p if p.exists() else None

    def variants(self, event, depth=0):
        """[(file, volume, pitch, weight)] that `event` can play."""
        entry = self.events.get(event)
        if entry is None or depth > 4:
            return []
        out = []
        for s in entry.get("sounds", []):
            s = {"name": s} if isinstance(s, str) else s
            vol, pitch, weight = s.get("volume", 1.0), s.get("pitch", 1.0), s.get("weight", 1)
            name = s["name"] if ":" in s["name"] else ("minecraft:" if event.startswith("minecraft:") else "astralend:") + s["name"]
            if s.get("type") == "event":
                out += [(f, v * vol, p * pitch, w * weight) for f, v, p, w in self.variants(name, depth + 1)]
            else:
                f = self.file(name)
                if f is not None and Path(f).exists():
                    out.append((f, vol, pitch, weight))
        return out


def music_bed(segments, seconds):
    """segments: [(at, file, offset, gain_db)] - from `at` seconds into the video, `file` (under the mod's
    sounds folder) from `offset` seconds into it; each crossfades into the next over 1.5 s, and the last
    fades out over the final 4 s."""
    n = int(seconds * RATE)
    bed = np.zeros((2, n), np.float32)
    fade = int(1.5 * RATE)
    for i, (at, file, offset, gain) in enumerate(segments):
        data = decode(MOD / "sounds" / file)
        start = int(at * RATE)
        end = int(segments[i + 1][0] * RATE) + fade if i + 1 < len(segments) else n
        o = int(offset * RATE)
        length = min(end - start, max(0, data.shape[1] - o))
        if length <= 0:
            continue
        piece = data[:, o:o + length]
        # every track brought to the same loudness first (some are much quieter than others), then the
        # section's own gain
        loud = piece[:, np.abs(piece).max(axis=0) > 1e-3]
        rms = float(np.sqrt(np.mean(loud ** 2))) if loud.size else 0.0
        level = min(4.0, max(0.25, 0.12 / rms)) if rms > 0 else 1.0
        piece = piece * level * (10 ** (gain / 20))
        env = np.ones(length, np.float32)
        k = min(fade, length)
        if i > 0:
            env[:k] *= np.linspace(0, 1, k)
        if i + 1 < len(segments):
            env[-k:] *= np.linspace(1, 0, k)
        bed[:, start:start + length] += piece * env
    tail = int(4.0 * RATE)
    bed[:, -tail:] *= np.linspace(1, 0, tail) ** 1.5
    return bed


def read_log(path):
    cams, sounds = {}, []
    if not path or not Path(path).exists():
        return cams, sounds
    for line in Path(path).read_text().splitlines():
        w = line.split()
        if not w:
            continue
        if w[0] == "cam" and len(w) == 7:
            cams[(int(w[1]), int(w[2]))] = tuple(float(v) for v in w[3:7])
        elif w[0] == "sound" and len(w) == 10:
            sounds.append((int(w[1]), int(w[2]), w[3], w[4], float(w[5]), float(w[6]), float(w[7]), float(w[8]), float(w[9])))
    return cams, sounds


def effects(log, shot_starts, seconds, library, report=None):
    """The sounds the world made, placed in time by the frame they were made on, faded with distance
    the way Minecraft fades them (to silence at 16 blocks, or 16 x the volume when louder), and panned
    by where they were from the camera."""
    n = int(seconds * RATE)
    out = np.zeros((2, n), np.float32)
    cams, sounds = read_log(log)
    played, missing, far = 0, {}, 0
    seen = {}
    for shot, frame, event, category, volume, pitch, x, y, z in sounds:
        if shot not in shot_starts:
            continue
        key = (shot, frame, event)
        seen[key] = seen.get(key, 0) + 1
        if seen[key] > 2:
            continue   # the same sound many times in one frame: twice is enough
        cam = cams.get((shot, frame)) or cams.get((shot, max(0, frame - 1)))
        if cam is None:
            continue
        variants = library.variants(event)
        if not variants:
            missing[event] = missing.get(event, 0) + 1
            continue
        h = int(hashlib.md5(f"{event}{shot}{frame}{x:.1f}".encode()).hexdigest(), 16)
        total = sum(v[3] for v in variants)
        pick = h % max(1, total)
        for f, vol, pt, weight in variants:
            if pick < weight:
                break
            pick -= weight
        dx, dy, dz = x - cam[0], y - cam[1], z - cam[2]
        dist = (dx * dx + dy * dy + dz * dz) ** 0.5
        reach = 16.0 * max(1.0, volume)
        gain = min(1.0, volume * vol) * max(0.0, 1.0 - dist / reach) * CATEGORY.get(category, 0.8)
        if gain <= 0.01:
            far += 1
            continue
        yaw = np.radians(cam[3])
        right = (-np.cos(yaw), -np.sin(yaw))         # facing south (+z), your right hand is west (-x)
        flat = (dx * dx + dz * dz) ** 0.5
        pan = float(np.clip((dx * right[0] + dz * right[1]) / flat, -1, 1)) * 0.8 if flat > 0.5 else 0.0
        data = decode(f)
        rate = pitch * pt
        if abs(rate - 1.0) > 0.01 and data.shape[1] > 1:
            idx = np.arange(0, data.shape[1] - 1, rate)
            data = np.vstack([np.interp(idx, np.arange(data.shape[1]), data[c]) for c in range(2)]).astype(np.float32)
        mono = data.mean(axis=0) * gain
        i = int(((shot_starts[shot] + frame) / FPS) * RATE)
        if i >= n:
            continue
        mono = mono[:n - i]
        out[0, i:i + len(mono)] += mono * np.sqrt(0.5 * (1 - pan))
        out[1, i:i + len(mono)] += mono * np.sqrt(0.5 * (1 + pan))
        played += 1
    lines = [f"soundtrack: {played} of {len(sounds)} sounds placed ({far} too far off to hear)"]
    if missing:
        lines.append("soundtrack: no file for " + ", ".join(f"{k} x{v}" for k, v in sorted(missing.items(), key=lambda kv: -kv[1])[:20]))
    for line in lines:
        print(line)
    if report:
        Path(report).write_text("\n".join(lines) + "\n")
    return out


def build(seconds, segments, log, shot_starts, report=None):
    """The whole soundtrack, stereo (2, n): the music under the world's sounds, the music dipping a
    little whenever something loud happens."""
    library = Library()
    bed = music_bed(segments, seconds) * 0.55
    sfx = effects(log, shot_starts, seconds, library, report) * 0.9
    # the music gives way a little to loud moments
    level = np.abs(sfx).max(axis=0)
    k = int(0.25 * RATE)
    kernel = np.ones(k, np.float32) / k
    env = np.clip(np.convolve(level, kernel, "same") * 2.5, 0, 1)
    mix = bed * (1 - 0.35 * env) + sfx
    # a gentle limiter, then a little headroom
    mix = np.tanh(mix * 1.1) / np.tanh(1.1)
    peak = float(np.max(np.abs(mix))) or 1.0
    return (mix / peak * 0.93).astype(np.float32)
