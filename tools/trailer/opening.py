"""Cuts the trailer as an anime opening: the shots the game filmed (client/ClientTrailer.java), cut hard on
the beat of the music, with white flashes and slash wipes between them, zoom punches and shakes on the
hits, freeze-frame cards for each creature (its name slammed in, a tint over the picture, speed lines),
the title slammed in two seconds in, bold captions for the places,
and an end card. Under a minute, and it opens on the game, not on black.

    python3 tools/trailer/opening.py <frames dir> <out dir> [scenes.txt]
    python3 tools/trailer/opening.py --remix <video> <sounds.txt> <out dir>    a new soundtrack only

The edit is the "clip" lines of scenes.txt (the shots are filmed in the order of the other lines; each has
an id=). A clip plays one filmed shot:

    clip <id> <frames on screen> [from=F] [speed=S] [fx=a,b,...] [punch=f,f] [freeze=f] [tint=rrggbb] | text

    from      the filmed frame it starts on (default 0); speed: filmed frames per frame shown (2 = twice
              as fast; 0.5 = slow motion)
    fx        flash   a white flash as it cuts in        slash   a diagonal wipe in from the last clip
              title   the title slammed in over it       card    at freeze=, it stops on a character card
              end     the end card over it               zoom    a slow push in over the clip
              out     a white flash as it ends
    punch     frames (of the clip) where a hit lands: a zoom punch, a shake and a colour split
    text      a caption, or for a card "Name / what it is"

The soundtrack is the music track from 30.5 s in (its beat falls every 12 frames from the first) under the
sounds the world made while it was filmed (soundtrack.py), each heard where its frame is shown.
Needs numpy, Pillow and ffmpeg, and the font Inter (Black).
"""
import math
import os
import random
import shlex
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
import music  # noqa: E402
import soundtrack  # noqa: E402

W, H, FPS = 1920, 1080, 24
ROOT = Path(__file__).resolve().parents[2]
SCENES = Path(__file__).with_name("scenes.txt")
ITEMS = ROOT / "src/main/resources/assets/astralend/textures/item"
MUSIC = ("music/space_music.ogg", 30.495, 0.0)    # the track, where in it the opening starts, gain
EGGS = ["hushback", "drift_whale", "mirror_wretch", "watcher", "hydragrip", "curator"]


def font_path(names):
    for d in ["/usr/share/fonts/opentype/inter", "/usr/share/fonts/truetype/inter"]:
        for n in names:
            p = Path(d) / n
            if p.exists():
                return str(p)
    out = subprocess.run(["fc-match", "-f", "%{file}", names[0].split(".")[0]], capture_output=True, text=True).stdout
    return out or None


BLACK = font_path(["Inter-Black.otf", "Inter-Black.ttf"])
BLACK_ITALIC = font_path(["Inter-BlackItalic.otf", "Inter-BlackItalic.ttf"]) or BLACK
_fonts = {}


def font(path, size):
    key = (path, size)
    if key not in _fonts:
        _fonts[key] = ImageFont.truetype(path, size) if path else ImageFont.load_default()
    return _fonts[key]


# ---- the edit

def parse(path=SCENES):
    """The shots (in filming order, numbered as the game numbers them) and the clips."""
    shots, clips, index = {}, [], 0
    for line in Path(path).read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        head, _, text = line.partition("|")
        words = head.split()
        if words[0] in ("card", "logo", "vo", "music"):
            continue
        if words[0] == "clip":
            opts = dict(w.split("=", 1) for w in words[3:] if "=" in w)
            clips.append({"id": words[1], "cut": int(words[2]), "from": float(opts.get("from", 0)),
                          "speed": float(opts.get("speed", 1)), "fx": set(opts.get("fx", "").split(",")) - {""},
                          "punch": [int(p) for p in opts.get("punch", "").split(",") if p],
                          "freeze": int(opts["freeze"]) if "freeze" in opts else None,
                          "tint": tuple(int(opts.get("tint", "b02a4a")[i:i + 2], 16) for i in (0, 2, 4)),
                          "text": text.strip()})
            continue
        opts = dict(w.split("=", 1) for w in words[2:] if "=" in w)
        shots[opts.get("id", f"s{index}")] = {"index": index, "shot": words[0], "frames": int(words[1])}
        index += 1
    return shots, clips


class Footage:
    def __init__(self, folder):
        self.folder = Path(folder)
        self.cache = {}
        self.missing = 0

    def get(self, shot, i):
        i = int(max(0, min(shot["frames"] - 1, i)))
        key = (shot["index"], i)
        if key not in self.cache:
            if len(self.cache) > 40:
                self.cache.pop(next(iter(self.cache)))
            p = self.folder / f"{shot['index']:02d}_{shot['shot']}_{i:04d}.png"
            if not p.exists():
                p = p.with_suffix(".jpg")
            if p.exists():
                img = Image.open(p).convert("RGB")
                self.cache[key] = img if img.size == (W, H) else img.resize((W, H), Image.LANCZOS)
            else:
                self.missing += 1
                self.cache[key] = Image.new("RGB", (W, H), (8, 6, 14))
        return self.cache[key]


# ---- the camera: zoom, punch and shake

def frame_with_camera(img, scale, dx, dy, angle=0.0):
    if abs(scale - 1) < 1e-3 and abs(dx) < 0.5 and abs(dy) < 0.5 and abs(angle) < 0.01:
        return img
    a = math.radians(angle)
    c, s = math.cos(a) / scale, math.sin(a) / scale
    cx, cy = W / 2, H / 2
    # output (x, y) samples input (c*(x-cx) + s*(y-cy) + cx - dx, -s*(x-cx) + c*(y-cy) + cy - dy)
    return img.transform((W, H), Image.AFFINE, (c, s, cx - c * cx - s * cy - dx, -s, c, cy + s * cx - c * cy - dy), Image.BICUBIC)


def punch_amount(j, punches):
    """How hard the last hit still shakes the frame (1 the moment it lands, gone in 8 frames)."""
    best = 0.0
    for p in punches:
        if 0 <= j - p < 8:
            best = max(best, (1 - (j - p) / 8) ** 2)
    return best


# ---- colour

def grade(arr):
    """The whole film's look: a little more contrast and colour, a soft vignette, a touch of grain."""
    lum = arr.mean(axis=2, keepdims=True)
    arr = lum + (arr - lum) * 1.18                  # saturation
    arr = (arr - 128) * 1.08 + 128                  # contrast
    return arr * VIGNETTE + (np.random.rand(H, W, 1).astype(np.float32) - 0.5) * 7


def duotone(arr, tint):
    """The freeze-frame look: the picture in two colours, dark ink and the creature's own."""
    lum = np.clip((arr.mean(axis=2, keepdims=True) - 20) / 200, 0, 1) ** 0.8
    dark = np.array([10, 6, 20], np.float32)
    light = np.array(tint, np.float32)
    hi = np.array([255, 245, 240], np.float32)
    return np.where(lum < 0.6, dark + (light - dark) * (lum / 0.6), light + (hi - light) * ((lum - 0.6) / 0.4))


def split_colour(arr, amount):
    if amount <= 0.01:
        return arr
    k = int(2 + 12 * amount)
    out = arr.copy()
    out[:, k:, 0] = arr[:, :-k, 0]
    out[:, :-k, 2] = arr[:, k:, 2]
    return out


def speed_lines(j, seed, strength=1.0):
    """Lines rushing out from the middle of the frame, as an anime cuts to its hero."""
    rnd = random.Random(seed + j // 2)
    img = Image.new("L", (W // 2, H // 2), 0)
    d = ImageDraw.Draw(img)
    cx, cy = W // 4, H // 4
    for _ in range(90):
        a = rnd.uniform(0, math.tau)
        r0 = rnd.uniform(170, 300)
        r1 = r0 + rnd.uniform(300, 700)
        w = rnd.choice([1, 1, 2, 3])
        d.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * r1, cy + math.sin(a) * r1)],
               fill=int(rnd.uniform(90, 200) * strength), width=w)
    return np.asarray(img.resize((W, H), Image.BILINEAR), np.float32)[:, :, None] / 255


# ---- type

def text_layer(text, path, size, fill=(255, 255, 255), stroke=0, stroke_fill=(0, 0, 0), spacing=0, shear=0.0):
    """Text as an RGBA image (letter-spaced, sheared to lean forward if asked)."""
    f = font(path, size)
    advances = [f.getlength(ch) for ch in text]
    total = sum(advances) + spacing * (len(text) - 1) + stroke * 2 + size * 0.3 + 8
    asc, desc = f.getmetrics()
    h = asc + desc + stroke * 2 + 8
    img = Image.new("RGBA", (int(total + abs(shear) * h) + 8, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x = 4 + stroke + abs(shear) * h * (1 if shear > 0 else 0)
    for ch, w in zip(text, advances):
        d.text((x, 4 + stroke), ch, font=f, fill=fill, stroke_width=stroke, stroke_fill=stroke_fill)
        x += w + spacing
    if shear:
        img = img.transform(img.size, Image.AFFINE, (1, shear, -shear * h if shear > 0 else 0, 0, 1, 0), Image.BICUBIC)
    return img


def paste(arr, layer, x, y, alpha=1.0, scale=1.0):
    """Composite an RGBA layer onto the frame array, its centre at (x, y)."""
    if alpha <= 0.01:
        return
    if abs(scale - 1) > 1e-3:
        layer = layer.resize((max(1, int(layer.width * scale)), max(1, int(layer.height * scale))), Image.BICUBIC)
    a = np.asarray(layer, np.float32)
    x0, y0 = int(x - a.shape[1] / 2), int(y - a.shape[0] / 2)
    xs, ys, xe, ye = max(0, x0), max(0, y0), min(W, x0 + a.shape[1]), min(H, y0 + a.shape[0])
    if xe <= xs or ye <= ys:
        return
    sub = a[ys - y0:ye - y0, xs - x0:xe - x0]
    m = sub[:, :, 3:] / 255 * alpha
    arr[ys:ye, xs:xe] = arr[ys:ye, xs:xe] * (1 - m) + sub[:, :, :3] * m


def ease_out(t):
    t = max(0.0, min(1.0, t))
    return 1 - (1 - t) ** 3


# ---- the overlays

def caption(arr, text, j, cut, tint):
    """A place's name: a slab of colour slashing in from the left, the name in heavy type over it."""
    size = 96
    big = text_layer(text.upper(), BLACK_ITALIC, size, stroke=5, stroke_fill=(10, 6, 20), spacing=2)
    while big.width > W - 460 and size > 40:
        size -= 6
        big = text_layer(text.upper(), BLACK_ITALIC, size, stroke=5, stroke_fill=(10, 6, 20), spacing=2)
    t_in = ease_out(j / 5)
    t_out = ease_out((j - (cut - 4)) / 4) if j >= cut - 4 else 0.0
    slide = (1 - t_in) * -900 + t_out * 1400
    y = H - 210
    # the slab: a slanted bar of the tint, then a thin white line under it
    slab = Image.new("RGBA", (big.width + 260, 120), (0, 0, 0, 0))
    ImageDraw.Draw(slab).polygon([(40, 0), (slab.width, 0), (slab.width - 40, 120), (0, 120)], fill=tint + (225,))
    paste(arr, slab, 120 + slab.width / 2 + slide * 1.15, y)
    line = Image.new("RGBA", (big.width + 380, 6), (255, 255, 255, 230))
    paste(arr, line, 80 + line.width / 2 + slide * 1.3, y + 72)
    paste(arr, big, 200 + big.width / 2 + slide, y - 4)


def character_card(arr, text, j, tint, seed):
    """The creature's card, from the moment the picture stops: its name slammed in, and what it is."""
    parts = [s.strip() for s in text.split("/")] + [""]
    name, role = parts[0].upper(), parts[1].upper()
    arr[:] = duotone(arr, tint)
    lines = speed_lines(j, seed, 0.8)
    arr[:] = arr * (1 - lines * 0.55) + 255 * lines * 0.55
    # a dark band across the lower third, at an angle
    band = Image.new("L", (W, H), 0)
    ImageDraw.Draw(band).polygon([(0, H * 0.60), (W, H * 0.52), (W, H * 0.80), (0, H * 0.88)], fill=200)
    b = np.asarray(band, np.float32)[:, :, None] / 255
    arr[:] = arr * (1 - b * 0.82) + np.array([8, 4, 14], np.float32) * b * 0.82
    slam = ease_out(j / 4)
    size = 172
    title = text_layer(name, BLACK_ITALIC, size, fill=(255, 255, 255), stroke=6, stroke_fill=tint, spacing=4)
    while title.width > W * 0.78 and size > 80:
        size -= 8
        title = text_layer(name, BLACK_ITALIC, size, fill=(255, 255, 255), stroke=6, stroke_fill=tint, spacing=4)
    shadow = text_layer(name, BLACK_ITALIC, size, fill=(0, 0, 0), spacing=4)
    x = W * 0.42 + (1 - slam) * 60 + j * 1.2
    paste(arr, shadow, x + 14, H * 0.69 + 12, alpha=0.6 * slam, scale=1.0 + (1 - slam) * 0.6)
    paste(arr, title, x, H * 0.69, alpha=min(1.0, 0.2 + slam), scale=1.0 + (1 - slam) * 0.6)
    if role:
        r = text_layer(role, BLACK, 38, fill=tint, stroke=2, stroke_fill=(0, 0, 0), spacing=8)
        paste(arr, r, W * 0.42 - title.width / 2 + r.width / 2 + 30 + j * 0.6, H * 0.69 - size * 0.72, alpha=ease_out((j - 3) / 4))


def title_card(arr, j):
    """THE ASTRAL END, slammed in: heavy, wide-set type, a thin rule above and below, the picture
    darkened behind it."""
    t = ease_out(j / 5)
    arr[:] = arr * (1 - 0.45 * t)
    big = text_layer("THE ASTRAL END", BLACK, 150, fill=(255, 255, 255), stroke=0, spacing=14)
    glow = big.filter(ImageFilter.GaussianBlur(18))
    g = np.asarray(glow, np.float32)
    tinted = Image.fromarray(np.dstack([np.full(g.shape[:2], 170), np.full(g.shape[:2], 90), np.full(g.shape[:2], 255),
                                        g[:, :, 3]]).astype(np.uint8), "RGBA")
    scale = 1.0 + (1 - t) * 0.5 - min(j, 72) * 0.0006
    paste(arr, tinted, W / 2, H / 2 - 20, alpha=t, scale=scale)
    paste(arr, big, W / 2, H / 2 - 20, alpha=t, scale=scale)
    rule = Image.new("RGBA", (int(big.width * 1.05 * ease_out((j - 2) / 6)) + 1, 4), (255, 255, 255, 220))
    paste(arr, rule, W / 2, H / 2 - 130, alpha=t)
    paste(arr, rule, W / 2, H / 2 + 92, alpha=t)


def end_card(arr, j):
    """Where to get it: the creatures' eggs in a row, the title, the versions, Modrinth."""
    blurred = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(10))
    arr[:] = np.asarray(blurred, np.float32) * 0.55
    eggs = [Image.open(ITEMS / f"{n}_spawn_egg.png").convert("RGBA").resize((144, 144), Image.NEAREST)
            for n in EGGS if (ITEMS / f"{n}_spawn_egg.png").exists()]
    for k, egg in enumerate(eggs):
        appear = ease_out((j - k * 2) / 5)
        x = W / 2 + (k - (len(eggs) - 1) / 2) * 180
        paste(arr, egg, x, H * 0.24 + (1 - appear) * -40, alpha=appear)
    title_card(arr, j + 5)
    v = text_layer("MINECRAFT 26.3  &  1.21.1   ·   FORGE", BLACK, 44, fill=(225, 220, 240), spacing=6)
    paste(arr, v, W / 2, H * 0.66, alpha=ease_out((j - 8) / 6))
    pill_text = text_layer("DOWNLOAD ON MODRINTH", BLACK, 52, fill=(255, 255, 255), spacing=6)
    pill = Image.new("RGBA", (pill_text.width + 90, 96), (0, 0, 0, 0))
    ImageDraw.Draw(pill).rounded_rectangle([0, 0, pill.width - 1, 95], radius=48, fill=(27, 217, 106, 255))
    pill.alpha_composite(pill_text, (45, (96 - pill_text.height) // 2))
    paste(arr, pill, W / 2, H * 0.78, alpha=ease_out((j - 12) / 6), scale=1.0 + 0.03 * math.sin(j / 5) * (j > 18))
    credit = text_layer("Music: \"Space Music\" by HitCtrl (CC-BY 3.0)", BLACK, 22, fill=(150, 145, 165), spacing=2)
    paste(arr, credit, W / 2, H - 40, alpha=ease_out((j - 12) / 6))


def flash(arr, amount):
    if amount > 0.01:
        arr[:] = arr * (1 - amount) + 255 * amount


def _vignette():
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    r = np.sqrt(((x - W / 2) / (W / 2)) ** 2 + ((y - H / 2) / (H / 2)) ** 2)
    return np.clip(1.0 - 0.28 * np.clip(r - 0.55, 0, 1) ** 1.5, 0, 1)[:, :, None]


VIGNETTE = _vignette()


# ---- putting it together

def score(shots, clips, log, out_dir):
    """The soundtrack (score.wav in out_dir): the music, and each sound the world made where its frame is seen."""
    seconds = sum(c["cut"] for c in clips) / FPS
    seen = {}
    t = 0
    for c in clips:
        shot = shots[c["id"]]
        last = None
        for j in range(c["cut"]):
            if c["freeze"] is not None and j >= c["freeze"]:
                break
            f0 = int(c["from"] + j * c["speed"])
            f1 = int(c["from"] + (j + 1) * c["speed"])
            for f in range(f0, max(f0 + 1, f1)):
                if f != last:
                    seen.setdefault((shot["index"], f), []).append(t + j)
                    last = f
        t += c["cut"]
    track = soundtrack.build(seconds, [(0.0,) + MUSIC], log, lambda shot, frame: seen.get((shot, frame), []), out_dir / "mix.txt")
    wav = out_dir / "score.wav"
    music.write(str(wav), track)
    return wav


def remix(video, log, out_dir, scenes=SCENES):
    """The trailer already cut, with a new soundtrack."""
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    wav = score(*parse(scenes), log, out_dir)
    return subprocess.call([os.environ.get("FFMPEG", "ffmpeg"), "-y", "-loglevel", "error", "-i", str(video), "-i", str(wav),
                            "-map", "0:v", "-map", "1:a", "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-shortest",
                            "-movflags", "+faststart", str(out_dir / "the_astral_end_trailer.mp4")])


def render(frames_dir, out_dir, scenes=SCENES):
    out_dir = Path(out_dir)
    (out_dir / "stills").mkdir(parents=True, exist_ok=True)
    shots, clips = parse(scenes)
    footage = Footage(frames_dir)
    total = sum(c["cut"] for c in clips)
    seconds = total / FPS
    print(f"opening: {len(clips)} clips, {total} frames ({seconds:.1f} s)")
    wav = score(shots, clips, Path(frames_dir) / "sounds.txt", out_dir)

    mp4 = out_dir / "the_astral_end_trailer.mp4"
    enc = subprocess.Popen([os.environ.get("FFMPEG", "ffmpeg"), "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24",
                            "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-", "-i", str(wav), "-c:v", "libx264", "-preset", "slow",
                            "-crf", "18", "-maxrate", "12M", "-bufsize", "24M",   # (under GitHub's 100 MB a file)
                            "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "192k", "-shortest", "-movflags", "+faststart", str(mp4)],
                           stdin=subprocess.PIPE)
    prev = None
    t = 0
    for ci, c in enumerate(clips):
        shot = shots[c["id"]]
        rnd = random.Random(ci)
        for j in range(c["cut"]):
            held = c["freeze"] is not None and j >= c["freeze"]
            src = c["from"] + (c["freeze"] if held else j) * c["speed"]
            img = footage.get(shot, src)
            # the camera
            p = punch_amount(j, c["punch"] + ([c["freeze"]] if c["freeze"] is not None else []))
            scale = 1.0 + 0.09 * p
            if "zoom" in c["fx"]:
                scale += 0.07 * j / max(1, c["cut"])
            if held:
                scale += 0.0025 * (j - c["freeze"])
            dx, dy = (rnd.uniform(-1, 1) * 26 * p, rnd.uniform(-1, 1) * 18 * p) if p > 0 else (0, 0)
            img = frame_with_camera(img, scale, dx, dy, rnd.uniform(-1, 1) * 1.2 * p)
            arr = np.asarray(img, np.float32).copy()
            arr = split_colour(arr, p)
            if held and "card" in c["fx"]:
                character_card(arr, c["text"], j - c["freeze"], c["tint"], ci * 101)
            elif c["text"] and "end" not in c["fx"] and "title" not in c["fx"]:
                caption(arr, c["text"], j, c["cut"], c["tint"])
            if "title" in c["fx"]:
                title_card(arr, j)
            if "end" in c["fx"]:
                end_card(arr, j)
            # the cuts: a slash in from the last clip, a white flash in, a white flash out
            if "slash" in c["fx"] and prev is not None and j < 6:
                edge = (j + 1) / 6 * (W + H * 0.6)
                xs = np.arange(W)[None, :] + (np.arange(H)[:, None] * 0.6)
                mask = (xs < edge)[:, :, None]
                arr = np.where(mask, arr, prev)
                stripe = (np.abs(xs - edge) < 14)[:, :, None]
                arr = np.where(stripe, 255.0, arr)
            if "flash" in c["fx"] and j < 4:
                flash(arr, [0.95, 0.6, 0.3, 0.1][j])
            if held and j - c["freeze"] < 3:
                flash(arr, [0.9, 0.45, 0.15][j - c["freeze"]])
            if "out" in c["fx"] and j >= c["cut"] - 3:
                flash(arr, [0.3, 0.65, 1.0][j - (c["cut"] - 3)])
            out = np.clip(grade(arr), 0, 255).astype(np.uint8)
            enc.stdin.write(out.tobytes())
            if j == c["cut"] // 2 or (held and j == c["freeze"] + 6):
                Image.fromarray(out).save(out_dir / "stills" / f"{ci:02d}_{c['id']}_{j:03d}.png")
            if t + j == int(FPS * 2.6):
                Image.fromarray(out).save(out_dir / "poster.png")
        prev = arr
        t += c["cut"]
    enc.stdin.close()
    enc.wait()
    print(f"wrote {mp4} ({total} frames, {seconds:.1f} s); {footage.missing} filmed frames were missing")
    return enc.returncode


if __name__ == "__main__":
    if sys.argv[1] == "--remix":
        sys.exit(remix(*sys.argv[2:5]))
    sys.exit(render(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else SCENES))
