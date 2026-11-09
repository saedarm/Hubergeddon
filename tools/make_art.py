"""Generates every image in assets/img. Run from the project root: python3 tools/make_art.py
Needs Pillow. Human portraits come from tools/photos/ (crop boxes in PHOTOS below)."""
import math, os, random
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = "assets/img"
FONT = "assets/fonts/Poppins-Bold.ttf"
os.makedirs(OUT, exist_ok=True)
S = 4  # supersampling factor


def font(size):
    return ImageFont.truetype(FONT, size)


def hexc(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


def save(img, name, size=None):
    if size:
        img = img.resize(size, Image.LANCZOS)
    img.save(os.path.join(OUT, name))


# ---------------------------------------------------------------- portraits

# name: (file, centre x, centre y, square size) in the original photo's pixels
PHOTOS = {
    "sam": ("tools/photos/sam.jpg", 840, 1150, 1450),
    "jack": ("tools/photos/jack.png", 262, 178, 270),
    "rob": ("tools/photos/rob.jpg", 592, 1240, 760),
}
COLORS = {"sam": 0x3B82F6, "jack": 0xB45309, "rob": 0xC8102E, "glorp": 0x22C55E,
          "zib": 0x94A3B8, "unit": 0xB91C1C, "moth": 0xA78BFA}


def ring(face, color):
    """Round portrait with a coloured ring, 256x256."""
    n = 256 * S
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    d.ellipse((0, 0, n - 1, n - 1), fill=hexc(color))
    inner = n - 22 * S
    face = face.resize((inner, inner), Image.LANCZOS).convert("RGBA")
    mask = Image.new("L", (inner, inner), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, inner - 1, inner - 1), fill=255)
    out.paste(face, (11 * S, 11 * S), mask)
    return out.resize((256, 256), Image.LANCZOS)


def photo(key):
    f, cx, cy, s = PHOTOS[key]
    im = Image.open(f).convert("RGB")
    return im.crop((cx - s // 2, cy - s // 2, cx + s // 2, cy + s // 2))


def alien_canvas(bg):
    n = 512 * S
    im = Image.new("RGBA", (n, n), hexc(bg))
    d = ImageDraw.Draw(im)
    # a little Huber backdrop: brick-coloured ground and a sky band
    d.rectangle((0, int(n * 0.72), n, n), fill=(120, 54, 40, 255))
    for y in range(int(n * 0.72), n, 18 * S):
        off = 0 if (y // (18 * S)) % 2 == 0 else 20 * S
        for x in range(-40 * S + off, n, 40 * S):
            d.rectangle((x, y, x + 38 * S, y + 16 * S), fill=(150, 66, 48, 255))
    return im, d, n


def eye(d, x, y, r, look=(0, 0), white=(255, 255, 255, 255)):
    d.ellipse((x - r, y - r, x + r, y + r), fill=white, outline=(20, 20, 30, 255), width=3 * S)
    pr = r * 0.48
    px, py = x + look[0] * r * 0.35, y + look[1] * r * 0.35
    d.ellipse((px - pr, py - pr, px + pr, py + pr), fill=(20, 20, 30, 255))
    d.ellipse((px - pr * 0.35 + pr * 0.3, py - pr * 0.35 - pr * 0.3, px + pr * 0.15 + pr * 0.3, py + pr * 0.15 - pr * 0.3), fill=(255, 255, 255, 255))


def glorp():
    im, d, n = alien_canvas(0xBBF7D0)
    u = n / 512
    g, dark = (34, 197, 94, 255), (21, 128, 61, 255)
    # blob body
    d.ellipse((90 * u, 150 * u, 422 * u, 470 * u), fill=g)
    d.ellipse((60 * u, 330 * u, 200 * u, 470 * u), fill=g)
    d.ellipse((320 * u, 320 * u, 460 * u, 470 * u), fill=g)
    # drips
    for x, h in [(150, 60), (260, 85), (360, 50)]:
        d.rounded_rectangle(((x - 14) * u, 430 * u, (x + 14) * u, (440 + h) * u), radius=14 * u, fill=g)
    # eye stalks
    for x, top in [(170, 70), (256, 40), (342, 80)]:
        d.line((x * u, 200 * u, x * u, (top + 40) * u), fill=dark, width=int(16 * u))
        eye(d, x * u, (top + 30) * u, 34 * u, look=(0.3, 0.4))
    # mouth with a leaf
    d.chord((180 * u, 270 * u, 340 * u, 390 * u), 0, 180, fill=(20, 60, 30, 255))
    d.ellipse((280 * u, 300 * u, 360 * u, 340 * u), fill=(132, 204, 22, 255))
    d.line((300 * u, 320 * u, 350 * u, 318 * u), fill=dark, width=int(4 * u))
    # shine
    d.ellipse((140 * u, 190 * u, 190 * u, 230 * u), fill=(187, 247, 208, 200))
    return im


def zib():
    im, d, n = alien_canvas(0xE2E8F0)
    u = n / 512
    skin, dark = (176, 186, 199, 255), (71, 85, 105, 255)
    # neck and uniform
    d.rectangle((215 * u, 360 * u, 297 * u, 420 * u), fill=skin)
    d.pieslice((110 * u, 390 * u, 402 * u, 640 * u), 180, 360, fill=(30, 58, 138, 255))
    for i in range(3):
        d.ellipse((248 * u, (430 + i * 26) * u, 264 * u, (446 + i * 26) * u), fill=(250, 204, 21, 255))
    # big head
    d.ellipse((96 * u, 40 * u, 416 * u, 330 * u), fill=skin)
    d.polygon([(150 * u, 250 * u), (362 * u, 250 * u), (256 * u, 390 * u)], fill=skin)
    # almond eyes
    for sx in (-1, 1):
        cx = 256 + sx * 72
        pts = []
        for k in range(40):
            t = k / 39 * 2 * math.pi
            x = cx + 62 * math.cos(t)
            y = 230 + 30 * math.sin(t) - sx * 14 * math.cos(t)
            pts.append((x * u, y * u))
        d.polygon(pts, fill=(15, 23, 42, 255))
        d.ellipse(((cx - 24 * sx - 10) * u, 212 * u, (cx - 24 * sx + 6) * u, 226 * u), fill=(255, 255, 255, 220))
    # unimpressed mouth
    d.line((236 * u, 330 * u, 276 * u, 326 * u), fill=dark, width=int(6 * u))
    # officer cap
    d.pieslice((120 * u, 10 * u, 392 * u, 170 * u), 180, 360, fill=(30, 58, 138, 255))
    d.rectangle((110 * u, 86 * u, 402 * u, 112 * u), fill=(15, 23, 42, 255))
    d.ellipse((236 * u, 40 * u, 276 * u, 80 * u), fill=(250, 204, 21, 255))
    # clipboard
    d.rounded_rectangle((350 * u, 330 * u, 450 * u, 470 * u), radius=8 * u, fill=(180, 120, 60, 255))
    d.rectangle((362 * u, 350 * u, 438 * u, 460 * u), fill=(255, 255, 255, 255))
    for i in range(5):
        d.line((370 * u, (370 + i * 18) * u, 430 * u, (370 + i * 18) * u), fill=(148, 163, 184, 255), width=int(3 * u))
    d.rectangle((380 * u, 322 * u, 420 * u, 342 * u), fill=(148, 163, 184, 255))
    return im


def unit():
    im, d, n = alien_canvas(0xFDE68A)
    u = n / 512
    brick, mortar = (178, 64, 44, 255), (214, 160, 130, 255)
    # house body
    d.rectangle((70 * u, 200 * u, 442 * u, 440 * u), fill=mortar)
    for row, y in enumerate(range(200, 440, 22)):
        off = 0 if row % 2 == 0 else 24
        for x in range(70 - off, 442, 48):
            x0, x1 = max(x, 70), min(x + 44, 442)
            if x1 > x0:
                d.rectangle((x0 * u, y * u, x1 * u, (y + 18) * u), fill=brick)
    # roof (hair) and chimney
    d.rectangle((330 * u, 70 * u, 380 * u, 170 * u), fill=brick)
    d.polygon([(40 * u, 210 * u), (256 * u, 90 * u), (472 * u, 210 * u)], fill=(55, 48, 45, 255))
    for i in range(6):
        d.line(((70 + i * 40) * u, (200 - i * 3) * u, (110 + i * 40) * u, (190 - i * 6) * u), fill=(85, 75, 70, 255), width=int(4 * u))
    # window eyes with shutters
    for cx in (160, 352):
        d.rectangle(((cx - 60) * u, 240 * u, (cx - 44) * u, 330 * u), fill=(30, 64, 120, 255))
        d.rectangle(((cx + 44) * u, 240 * u, (cx + 60) * u, 330 * u), fill=(30, 64, 120, 255))
        d.rectangle(((cx - 42) * u, 240 * u, (cx + 42) * u, 330 * u), fill=(255, 255, 255, 255))
        eye(d, cx * u, 285 * u, 32 * u, look=(-0.3 if cx > 256 else 0.3, 0.2), white=(254, 249, 195, 255))
        d.line(((cx - 42) * u, 285 * u, (cx - 32) * u, 285 * u), fill=(255, 255, 255, 255), width=int(4 * u))
    # angry brows (gutters)
    d.line((100 * u, 222 * u, 210 * u, 236 * u), fill=(40, 40, 40, 255), width=int(10 * u))
    d.line((302 * u, 236 * u, 412 * u, 222 * u), fill=(40, 40, 40, 255), width=int(10 * u))
    # door mouth
    d.rounded_rectangle((210 * u, 340 * u, 302 * u, 440 * u), radius=10 * u, fill=(120, 53, 15, 255))
    d.rectangle((222 * u, 352 * u, 290 * u, 380 * u), fill=(60, 20, 10, 255))
    for x in range(228, 290, 14):
        d.rectangle((x * u, 352 * u, (x + 8) * u, 362 * u), fill=(255, 255, 255, 255))
    d.ellipse((284 * u, 392 * u, 294 * u, 402 * u), fill=(250, 204, 21, 255))
    # address
    d.text((380 * u, 360 * u), "10,708", font=font(int(18 * u)), fill=(255, 255, 255, 255), anchor="mm")
    return im


def moth():
    im, d, n = alien_canvas(0x1E1B4B)
    u = n / 512
    # porch light glow
    glow = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((330 * u, 10 * u, 520 * u, 200 * u), fill=(253, 224, 71, 160))
    glow = glow.filter(ImageFilter.GaussianBlur(30 * u))
    im.alpha_composite(glow)
    d = ImageDraw.Draw(im)
    d.ellipse((400 * u, 70 * u, 460 * u, 130 * u), fill=(254, 249, 195, 255))
    wing, wing2 = (167, 139, 250, 255), (124, 58, 237, 255)
    # wings
    d.ellipse((10 * u, 120 * u, 250 * u, 330 * u), fill=wing)
    d.ellipse((262 * u, 120 * u, 502 * u, 330 * u), fill=wing)
    d.ellipse((40 * u, 280 * u, 230 * u, 440 * u), fill=wing2)
    d.ellipse((282 * u, 280 * u, 472 * u, 440 * u), fill=wing2)
    for cx in (110, 402):
        d.ellipse(((cx - 40) * u, 180 * u, (cx + 40) * u, 260 * u), fill=(237, 233, 254, 255))
        d.ellipse(((cx - 18) * u, 202 * u, (cx + 18) * u, 238 * u), fill=wing2)
    # fuzzy body
    d.ellipse((186 * u, 130 * u, 326 * u, 470 * u), fill=(230, 220, 200, 255))
    for i in range(60):
        a = random.uniform(0, 2 * math.pi)
        r = random.uniform(0.8, 1.0)
        x = 256 + 70 * r * math.cos(a)
        y = 300 + 170 * r * math.sin(a)
        d.ellipse(((x - 6) * u, (y - 6) * u, (x + 6) * u, (y + 6) * u), fill=(245, 238, 225, 255))
    # head
    d.ellipse((176 * u, 80 * u, 336 * u, 230 * u), fill=(240, 232, 215, 255))
    # antennae
    d.line((220 * u, 95 * u, 170 * u, 20 * u), fill=(60, 50, 40, 255), width=int(7 * u))
    d.line((292 * u, 95 * u, 342 * u, 20 * u), fill=(60, 50, 40, 255), width=int(7 * u))
    for x in (170, 342):
        d.ellipse(((x - 14) * u, 6 * u, (x + 14) * u, 34 * u), fill=(60, 50, 40, 255))
    # dad glasses
    for cx in (220, 292):
        d.rounded_rectangle(((cx - 34) * u, 130 * u, (cx + 34) * u, 176 * u), radius=10 * u, fill=(255, 255, 255, 140), outline=(30, 30, 30, 255), width=int(6 * u))
        eye(d, cx * u, 153 * u, 13 * u, look=(0.5, -0.4))
    d.line((254 * u, 150 * u, 258 * u, 150 * u), fill=(30, 30, 30, 255), width=int(6 * u))
    # mustache and grin
    d.chord((210 * u, 186 * u, 302 * u, 228 * u), 0, 180, fill=(255, 255, 255, 255), outline=(60, 40, 30, 255), width=int(3 * u))
    d.ellipse((206 * u, 176 * u, 258 * u, 200 * u), fill=(90, 60, 40, 255))
    d.ellipse((254 * u, 176 * u, 306 * u, 200 * u), fill=(90, 60, 40, 255))
    return im


def portraits():
    for key in ("sam", "jack", "rob"):
        save(ring(photo(key), COLORS[key]), f"portrait_{key}.png")
    random.seed(4)
    for key, fn in (("glorp", glorp), ("zib", zib), ("unit", unit), ("moth", moth)):
        save(ring(fn(), COLORS[key]), f"portrait_{key}.png")


# ---------------------------------------------------------------- dice and icons

def heart(d, cx, cy, r, fill):
    d.ellipse((cx - r, cy - r * 0.9, cx, cy + r * 0.1), fill=fill)
    d.ellipse((cx, cy - r * 0.9, cx + r, cy + r * 0.1), fill=fill)
    d.polygon([(cx - r * 0.97, cy - r * 0.25), (cx + r * 0.97, cy - r * 0.25), (cx, cy + r)], fill=fill)


def bolt(d, cx, cy, r, fill, outline=None):
    pts = [(0.15, -1), (-0.55, 0.12), (-0.05, 0.12), (-0.25, 1), (0.6, -0.2), (0.08, -0.2), (0.35, -1)]
    d.polygon([(cx + x * r, cy + y * r) for x, y in pts], fill=fill, outline=outline)


def battery(d, cx, cy, r, fill, level=(34, 197, 94, 255)):
    w, h = r * 0.95, r * 1.7
    d.rounded_rectangle((cx - w / 2, cy - h / 2 + r * 0.15, cx + w / 2, cy + h / 2), radius=r * 0.18, fill=fill)
    d.rectangle((cx - w * 0.22, cy - h / 2 - r * 0.02, cx + w * 0.22, cy - h / 2 + r * 0.18), fill=fill)
    inset = r * 0.14
    d.rounded_rectangle((cx - w / 2 + inset, cy - h * 0.05, cx + w / 2 - inset, cy + h / 2 - inset), radius=r * 0.08, fill=level)
    d.line((cx - r * 0.18, cy + h * 0.2, cx + r * 0.18, cy + h * 0.2), fill=(255, 255, 255, 255), width=int(r * 0.1))
    d.line((cx, cy + h * 0.2 - r * 0.18, cx, cy + h * 0.2 + r * 0.18), fill=(255, 255, 255, 255), width=int(r * 0.1))


def star(d, cx, cy, r, fill):
    pts = []
    for k in range(10):
        a = -math.pi / 2 + k * math.pi / 5
        rr = r if k % 2 == 0 else r * 0.45
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    d.polygon(pts, fill=fill)


def dice():
    n = 128 * S
    for f in range(6):
        im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        d.rounded_rectangle((4 * S, 8 * S, n - 4 * S, n - 2 * S), radius=24 * S, fill=(150, 140, 125, 255))
        d.rounded_rectangle((4 * S, 2 * S, n - 4 * S, n - 8 * S), radius=24 * S, fill=(253, 250, 240, 255))
        c = n / 2
        cy = c - 3 * S
        if f < 3:
            d.text((c, cy), str(f + 1), font=font(78 * S), fill=(40, 40, 50, 255), anchor="mm")
        elif f == 3:
            bolt(d, c, cy, 44 * S, (250, 204, 21, 255))
            bolt(d, c - 3 * S, cy - 3 * S, 40 * S, (234, 88, 12, 255))
        elif f == 4:
            heart(d, c, cy + 4 * S, 40 * S, (220, 38, 38, 255))
        else:
            battery(d, c, cy, 34 * S, (40, 40, 50, 255))
        save(im, f"die_{f}.png", (128, 128))


def icons():
    n = 64 * S
    def new():
        im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
        return im, ImageDraw.Draw(im)
    im, d = new(); heart(d, n / 2, n / 2 + 3 * S, 28 * S, (239, 68, 68, 255)); save(im, "icon_heart.png", (64, 64))
    im, d = new(); star(d, n / 2, n / 2 + 2 * S, 30 * S, (250, 204, 21, 255)); save(im, "icon_star.png", (64, 64))
    im, d = new(); battery(d, n / 2, n / 2 + 2 * S, 26 * S, (60, 60, 70, 255)); save(im, "icon_battery.png", (64, 64))
    im, d = new(); bolt(d, n / 2, n / 2, 28 * S, (250, 204, 21, 255)); save(im, "icon_zap.png", (64, 64))
    # ghost
    im, d = new()
    d.ellipse((12 * S, 6 * S, 52 * S, 46 * S), fill=(241, 245, 249, 235))
    d.rectangle((12 * S, 26 * S, 52 * S, 52 * S), fill=(241, 245, 249, 235))
    for k in range(4):
        d.ellipse(((12 + k * 10) * S, 46 * S, (22 + k * 10) * S, 58 * S), fill=(241, 245, 249, 235))
    d.ellipse((22 * S, 20 * S, 29 * S, 30 * S), fill=(30, 30, 40, 255))
    d.ellipse((35 * S, 20 * S, 42 * S, 30 * S), fill=(30, 30, 40, 255))
    d.ellipse((28 * S, 34 * S, 36 * S, 42 * S), fill=(30, 30, 40, 255))
    save(im, "icon_ghost.png", (64, 64))
    # brick (Huber marker)
    im, d = new()
    d.rounded_rectangle((6 * S, 18 * S, 58 * S, 46 * S), radius=4 * S, fill=(178, 64, 44, 255))
    d.line((6 * S, 32 * S, 58 * S, 32 * S), fill=(214, 160, 130, 255), width=3 * S)
    d.line((32 * S, 18 * S, 32 * S, 32 * S), fill=(214, 160, 130, 255), width=3 * S)
    d.line((19 * S, 32 * S, 19 * S, 46 * S), fill=(214, 160, 130, 255), width=3 * S)
    d.line((45 * S, 32 * S, 45 * S, 46 * S), fill=(214, 160, 130, 255), width=3 * S)
    save(im, "icon_brick.png", (64, 64))
    # UI shapes
    im = Image.new("RGBA", (64 * S, 64 * S), (0, 0, 0, 0))
    ImageDraw.Draw(im).rounded_rectangle((0, 0, 64 * S - 1, 64 * S - 1), radius=16 * S, fill=(255, 255, 255, 255))
    save(im, "round.png", (64, 64))
    im = Image.new("RGBA", (64 * S, 64 * S), (0, 0, 0, 0))
    ImageDraw.Draw(im).ellipse((0, 0, 64 * S - 1, 64 * S - 1), fill=(255, 255, 255, 255))
    save(im, "circle.png", (64, 64))
    sh = Image.new("RGBA", (96, 96), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((16, 16, 79, 79), radius=16, fill=(0, 0, 0, 150))
    save(sh.filter(ImageFilter.GaussianBlur(7)), "shadow.png")
    Image.new("RGBA", (4, 4), (255, 255, 255, 255)).save(os.path.join(OUT, "white.png"))


# ---------------------------------------------------------------- the Huber Heights backdrop

def ranch(d, x, y, w, scale, brick, roof, lit):
    h = 70 * scale
    d.rectangle((x, y - h, x + w, y), fill=brick)
    for yy in range(int(y - h), int(y), max(2, int(8 * scale))):
        d.line((x, yy, x + w, yy), fill=tuple(max(0, c - 18) for c in brick[:3]) + (255,), width=1)
    d.polygon([(x - 10 * scale, y - h), (x + w + 10 * scale, y - h), (x + w - 30 * scale, y - h - 34 * scale), (x + 30 * scale, y - h - 34 * scale)], fill=roof)
    # windows and door, all identical (that's the joke)
    win = (255, 214, 120, 255) if lit else (40, 52, 80, 255)
    for wx in (0.12, 0.62):
        d.rectangle((x + w * wx, y - h * 0.68, x + w * wx + 34 * scale, y - h * 0.32), fill=win)
        d.line((x + w * wx + 17 * scale, y - h * 0.68, x + w * wx + 17 * scale, y - h * 0.32), fill=(255, 255, 255, 255), width=max(1, int(2 * scale)))
    d.rectangle((x + w * 0.42, y - h * 0.62, x + w * 0.42 + 24 * scale, y), fill=(92, 45, 20, 255))


def backdrop():
    W, H = 1280 * 2, 720 * 2
    im = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(im)
    # dusk sky
    top, bot = (24, 22, 58), (240, 128, 90)
    for y in range(H):
        t = min(1, y / (H * 0.62))
        d.line((0, y, W, y), fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)) + (255,))
    rnd = random.Random(11)
    for _ in range(140):
        x, y = rnd.uniform(0, W), rnd.uniform(0, H * 0.35)
        r = rnd.uniform(1, 3)
        d.ellipse((x - r, y - r, x + r, y + r), fill=(255, 255, 255, rnd.randint(120, 255)))
    # UFOs
    for (ux, uy, us) in ((300, 160, 1.0), (2050, 230, 0.7), (1500, 110, 0.45)):
        beam = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(beam).polygon([(ux - 40 * us, uy + 20 * us), (ux + 40 * us, uy + 20 * us), (ux + 170 * us, H * 0.8), (ux - 170 * us, H * 0.8)], fill=(180, 255, 200, 40))
        im.alpha_composite(beam)
        d = ImageDraw.Draw(im)
        d.ellipse((ux - 110 * us, uy - 10 * us, ux + 110 * us, uy + 34 * us), fill=(148, 163, 184, 255))
        d.ellipse((ux - 50 * us, uy - 50 * us, ux + 50 * us, uy + 14 * us), fill=(125, 211, 252, 220))
        for k in range(5):
            lx = ux - 80 * us + k * 40 * us
            d.ellipse((lx - 7 * us, uy + 8 * us, lx + 7 * us, uy + 22 * us), fill=(250, 204, 21, 255))
    # rows of identical brick ranches, back to front
    rows = [(int(H * 0.60), 0.55, (150, 70, 60, 255), (70, 60, 60, 255)),
            (int(H * 0.70), 0.75, (165, 72, 55, 255), (60, 52, 50, 255)),
            (int(H * 0.82), 1.0, (178, 76, 52, 255), (50, 44, 42, 255))]
    for y, sc, brick, roof in rows:
        d.rectangle((0, y, W, H), fill=(58, 92, 48, 255) if sc < 1 else (70, 110, 55, 255))
        w = 190 * sc
        x = -rnd.uniform(0, w)
        while x < W:
            ranch(d, x, y, w, sc, brick, roof, rnd.random() < 0.5)
            x += w + 50 * sc
    # the street
    d.rectangle((0, int(H * 0.86), W, H), fill=(52, 54, 60, 255))
    for x in range(0, W, 140):
        d.rectangle((x, int(H * 0.925), x + 70, int(H * 0.935)), fill=(250, 204, 21, 255))
    # welcome sign
    sx, sy = W - 560, int(H * 0.50)
    d.rectangle((sx + 40, sy + 120, sx + 56, sy + 260), fill=(90, 70, 50, 255))
    d.rectangle((sx + 420, sy + 120, sx + 436, sy + 260), fill=(90, 70, 50, 255))
    d.rounded_rectangle((sx, sy, sx + 480, sy + 140), radius=16, fill=(22, 101, 52, 255), outline=(255, 255, 255, 255), width=6)
    d.text((sx + 240, sy + 50), "WELCOME TO HUBER HEIGHTS", font=font(29), fill=(255, 255, 255, 255), anchor="mm")
    d.text((sx + 240, sy + 100), "Come Grow With Us!", font=font(30), fill=(253, 230, 138, 255), anchor="mm")
    # darken a touch so the UI reads on top
    shade = Image.new("RGBA", (W, H), (10, 10, 25, 70))
    im.alpha_composite(shade)
    save(im.convert("RGB"), "backdrop.png", (1280, 720))


def title_card():
    """Logo used on the title screen."""
    W, H = 1100, 260
    im = Image.new("RGBA", (W * 2, H * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    f = font(190)
    for dx, dy in ((10, 12), (6, 8)):
        d.text((W + dx, 150 + dy), "HUBERGEDDON", font=f, fill=(60, 15, 10, 255), anchor="mm")
    d.text((W, 150), "HUBERGEDDON", font=f, fill=(250, 204, 21, 255), anchor="mm", stroke_width=10, stroke_fill=(120, 30, 20, 255))
    d.text((W, 380), "COME GROW WITH US", font=font(74), fill=(255, 255, 255, 255), anchor="mm", stroke_width=6, stroke_fill=(30, 30, 40, 255))
    save(im, "logo.png", (W, H))


if __name__ == "__main__":
    portraits()
    dice()
    icons()
    backdrop()
    title_card()
    print("wrote", len(os.listdir(OUT)), "images to", OUT)
