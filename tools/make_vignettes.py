"""Little illustrations for the Only-in-Huber mishaps and Huber-wide events (assets/img/mishap_N.png, event_N.png).
Run from the project root: python3 tools/make_vignettes.py (needs Pillow). Order matches MISHAPS and EVENTS in Game.java."""
import math, os, random
from PIL import Image, ImageDraw, ImageFont

OUT = "assets/img"
FONT = "assets/fonts/Poppins-Bold.ttf"
W, H = 600, 340  # drawn at 2x, saved at 300x170
rnd = random.Random(5)


def font(s): return ImageFont.truetype(FONT, s)


BRICK, MORTAR, ROOF = (178, 70, 50), (214, 160, 130), (60, 52, 50)
SKY, GRASS, ROAD = (135, 190, 235), (95, 150, 70), (70, 72, 78)


def canvas(sky=SKY, ground=GRASS, gy=230):
    im = Image.new("RGB", (W, H), sky)
    d = ImageDraw.Draw(im)
    d.rectangle((0, gy, W, H), fill=ground)
    return im, d


def house(d, x, y, w=120, lit=False, color=BRICK):
    h = w * 0.45
    d.rectangle((x, y - h, x + w, y), fill=color)
    for yy in range(int(y - h), int(y), 7):
        d.line((x, yy, x + w, yy), fill=tuple(max(0, c - 22) for c in color))
    d.polygon([(x - 8, y - h), (x + w + 8, y - h), (x + w - 22, y - h - 26), (x + 22, y - h - 26)], fill=ROOF)
    win = (255, 214, 120) if lit else (40, 52, 80)
    d.rectangle((x + w * 0.12, y - h * 0.7, x + w * 0.12 + 22, y - h * 0.3), fill=win)
    d.rectangle((x + w * 0.66, y - h * 0.7, x + w * 0.66 + 22, y - h * 0.3), fill=win)
    d.rectangle((x + w * 0.42, y - h * 0.62, x + w * 0.42 + 16, y), fill=(92, 45, 20))


def car(d, x, y, color, w=110, brake=False):
    d.rounded_rectangle((x, y - 34, x + w, y), radius=10, fill=color)
    d.rounded_rectangle((x + w * 0.2, y - 56, x + w * 0.78, y - 30), radius=10, fill=color)
    d.rectangle((x + w * 0.26, y - 52, x + w * 0.48, y - 34), fill=(190, 225, 245))
    d.rectangle((x + w * 0.52, y - 52, x + w * 0.72, y - 34), fill=(190, 225, 245))
    for wx in (x + w * 0.22, x + w * 0.78):
        d.ellipse((wx - 14, y - 14, wx + 14, y + 14), fill=(30, 30, 35))
        d.ellipse((wx - 6, y - 6, wx + 6, y + 6), fill=(150, 150, 160))
    if brake:
        d.rectangle((x - 2, y - 28, x + 6, y - 18), fill=(255, 40, 40))


def cone(d, x, y, s=1.0):
    d.polygon([(x - 18 * s, y), (x + 18 * s, y), (x + 5 * s, y - 60 * s), (x - 5 * s, y - 60 * s)], fill=(249, 115, 22))
    d.rectangle((x - 13 * s, y - 34 * s, x + 13 * s, y - 24 * s), fill=(255, 255, 255))
    d.rectangle((x - 24 * s, y - 6 * s, x + 24 * s, y), fill=(234, 88, 12))


def person(d, x, y, color=(59, 130, 246), s=1.0):
    d.ellipse((x - 9 * s, y - 70 * s, x + 9 * s, y - 52 * s), fill=(240, 200, 170))
    d.line((x, y - 52 * s, x, y - 24 * s), fill=color, width=int(9 * s))
    d.line((x, y - 24 * s, x - 10 * s, y), fill=(40, 40, 60), width=int(6 * s))
    d.line((x, y - 24 * s, x + 12 * s, y - 2 * s), fill=(40, 40, 60), width=int(6 * s))
    d.line((x, y - 46 * s, x - 14 * s, y - 30 * s), fill=color, width=int(5 * s))
    d.line((x, y - 46 * s, x + 14 * s, y - 34 * s), fill=color, width=int(5 * s))


def caption(d, text, y=24, size=34, fill=(255, 255, 255), stroke=(30, 30, 40)):
    d.text((W / 2, y), text, font=font(size), fill=fill, anchor="mm", stroke_width=4, stroke_fill=stroke)


def letter(d, x, y, w, h, title, stamp=None):
    d.rectangle((x + 6, y + 6, x + w + 6, y + h + 6), fill=(0, 0, 0))
    d.rectangle((x, y, x + w, y + h), fill=(252, 250, 240))
    d.text((x + w / 2, y + 30), title, font=font(26), fill=(30, 30, 40), anchor="mm")
    for k in range(5):
        d.line((x + 24, y + 62 + k * 18, x + w - 24 - (k == 4) * 80, y + 62 + k * 18), fill=(170, 170, 180), width=4)
    if stamp:
        d.rounded_rectangle((x + w - 190, y + h - 70, x + w - 14, y + h - 20), radius=8, outline=(220, 38, 38), width=5)
        d.text((x + w - 102, y + h - 45), stamp, font=font(24), fill=(220, 38, 38), anchor="mm")


def save(im, name):
    im = im.resize((W // 2, H // 2), Image.LANCZOS)
    # rounded corners
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), radius=14, fill=255)
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    out.paste(im, (0, 0), m)
    out.save(os.path.join(OUT, name))


# ---------------------------------------------------------------- mishaps

def m0():  # lost among identical ranches
    im, d = canvas(gy=60)
    for r in range(3):
        for c in range(4):
            house(d, 20 + c * 150, 120 + r * 95, 110)
    person(d, 300, 330, s=0.9)
    d.text((330, 230), "?", font=font(70), fill=(255, 255, 255), stroke_width=5, stroke_fill=(30, 30, 40))
    return im


def m1():  # Old Troy Pike traffic
    im, d = canvas(gy=200)
    d.rectangle((0, 200, W, H), fill=ROAD)
    for x in range(0, W, 90): d.rectangle((x, 300, x + 50, 308), fill=(250, 204, 21))
    colors = [(220, 38, 38), (59, 130, 246), (250, 250, 250), (34, 197, 94), (120, 120, 130)]
    for i, x in enumerate(range(-40, W, 125)):
        car(d, x, 280, colors[i % 5], brake=True)
    caption(d, "OLD TROY PIKE  ·  5:04 PM", 60)
    return im


def m2():  # lane closed
    im, d = canvas(gy=200)
    d.rectangle((0, 200, W, H), fill=ROAD)
    for x in (120, 220, 320, 420): cone(d, x, 320)
    d.rectangle((420, 90, 590, 170), fill=(249, 115, 22))
    d.text((505, 130), "LANE\nCLOSED", font=font(26), fill=(20, 20, 20), anchor="mm", align="center")
    d.rectangle((500, 170, 510, 230), fill=(80, 80, 80))
    car(d, 20, 300, (59, 130, 246), brake=True)
    return im


def m3():  # no downtown
    im, d = canvas(sky=(235, 225, 200), ground=(235, 225, 200))
    for k in range(9):
        d.line((rnd.randint(0, W), 0, rnd.randint(0, W), H), fill=(200, 190, 160), width=10)
        d.line((0, rnd.randint(0, H), W, rnd.randint(0, H)), fill=(200, 190, 160), width=10)
    d.ellipse((265, 120, 335, 190), fill=(220, 38, 38))
    d.polygon([(270, 168), (330, 168), (300, 230)], fill=(220, 38, 38))
    d.ellipse((288, 140, 312, 164), fill=(255, 255, 255))
    caption(d, "DOWNTOWN HUBER", 50, 36, fill=(30, 30, 40), stroke=(235, 225, 200))
    caption(d, "404: NOT FOUND", 290, 40, fill=(220, 38, 38), stroke=(255, 255, 255))
    return im


def m4():  # hedge fund landlord
    im, d = canvas(sky=(30, 60, 50), ground=(30, 60, 50))
    letter(d, 120, 60, 360, 230, "RENT IS DUE", "PAY UP")
    d.rectangle((400, 20, 470, 70), fill=(20, 20, 20))
    d.rectangle((380, 66, 490, 78), fill=(20, 20, 20))
    for k in range(6):
        d.text((rnd.randint(20, 100), rnd.randint(40, 300)), "$", font=font(48), fill=(74, 222, 128))
        d.text((rnd.randint(500, 580), rnd.randint(100, 300)), "$", font=font(48), fill=(74, 222, 128))
    return im


def m5():  # Rose parking lot walk
    im, d = canvas(gy=140, ground=(90, 92, 98))
    d.polygon([(380, 140), (590, 140), (570, 60), (400, 60)], fill=(70, 70, 90))
    d.text((485, 98), "THE ROSE", font=font(24), fill=(250, 204, 21), anchor="mm")
    for i in range(10):
        x = 20 + i * 60
        d.line((x, 160, x - 40 + i * 6, H), fill=(240, 240, 240), width=5)
    person(d, 110, 320, s=0.8)
    d.text((150, 270), "1 mile to go", font=font(24), fill=(255, 255, 255))
    return im


def m6():  # stubbed toe on a brick
    im, d = canvas(gy=250)
    d.rounded_rectangle((250, 210, 400, 270), radius=6, fill=BRICK)
    d.line((250, 240, 400, 240), fill=MORTAR, width=5)
    d.line((325, 210, 325, 240), fill=MORTAR, width=5)
    d.ellipse((120, 210, 270, 270), fill=(240, 200, 170))
    for k in range(5): d.ellipse((240 + k * 0, 216 + k * 10, 262, 232 + k * 10), fill=(250, 190, 160))
    for (x, y) in ((280, 120), (350, 90), (430, 140)):
        pts = [(x + 26 * math.cos(a * math.pi / 5) * (1 if a % 2 == 0 else 0.45), y + 26 * math.sin(a * math.pi / 5) * (1 if a % 2 == 0 else 0.45)) for a in range(10)]
        d.polygon(pts, fill=(250, 204, 21))
    caption(d, "OW. OW. OW.", 40)
    return im


def m7():  # roundabout four times
    im, d = canvas(ground=GRASS, gy=0)
    d.ellipse((150, 20, 450, 320), fill=ROAD)
    d.ellipse((230, 100, 370, 240), fill=(110, 170, 80))
    for k in range(4):
        a = k * math.pi / 2
        x, y = 300 + 115 * math.cos(a), 170 + 115 * math.sin(a)
        d.polygon([(x + 14 * math.cos(a + 1.6), y + 14 * math.sin(a + 1.6)), (x - 12 * math.cos(a), y - 12 * math.sin(a)), (x + 12 * math.cos(a), y + 12 * math.sin(a))], fill=(255, 255, 255))
    car(d, 380, 120, (220, 38, 38), w=80)
    caption(d, "LAP 4", 300, 36)
    return im


def m8():  # Come Grow With Us sign waves
    im, d = canvas(gy=260)
    d.rectangle((150, 180, 164, 290), fill=(90, 70, 50))
    d.rectangle((436, 180, 450, 290), fill=(90, 70, 50))
    d.rounded_rectangle((110, 70, 490, 190), radius=14, fill=(22, 101, 52), outline=(255, 255, 255), width=5)
    d.text((300, 110), "COME GROW", font=font(38), fill=(255, 255, 255), anchor="mm")
    d.text((300, 155), "WITH US!", font=font(38), fill=(253, 230, 138), anchor="mm")
    d.ellipse((470, 40, 530, 100), fill=(240, 200, 170))
    for k in range(3): d.arc((440 - k * 14, 20 - k * 14, 560 + k * 14, 140 + k * 14), 300, 350, fill=(255, 255, 255), width=4)
    return im


def m9():  # Brandt Pike plan, again
    im, d = canvas(sky=(30, 64, 120), ground=(30, 64, 120))
    d.rectangle((100, 60, 500, 290), fill=(59, 130, 246))
    for x in range(100, 500, 30): d.line((x, 60, x, 290), fill=(96, 165, 250), width=1)
    for y in range(60, 290, 30): d.line((100, y, 500, y), fill=(96, 165, 250), width=1)
    d.text((300, 120), "BRANDT PIKE", font=font(34), fill=(255, 255, 255), anchor="mm")
    d.text((300, 170), "REVITALIZATION", font=font(30), fill=(255, 255, 255), anchor="mm")
    d.text((300, 225), "PLAN v7 (DRAFT)", font=font(26), fill=(253, 230, 138), anchor="mm")
    for k in range(4): d.line((100, 60 + k * 9, 160 - k * 12, 60), fill=(220, 220, 230), width=2)
    return im


def m10():  # Meijer had everything
    im, d = canvas(sky=(250, 245, 235), ground=(210, 205, 195), gy=290)
    d.polygon([(150, 140), (450, 140), (420, 260), (180, 260)], outline=(120, 120, 130), width=8)
    for x in range(180, 440, 30): d.line((x, 140, x + 5, 260), fill=(160, 160, 170), width=3)
    d.line((450, 140, 500, 100), fill=(120, 120, 130), width=8)
    for wx in (200, 400): d.ellipse((wx - 18, 268, wx + 18, 304), fill=(40, 40, 45))
    items = [(220, 38, 38), (59, 130, 246), (250, 204, 21), (34, 197, 94), (168, 85, 247), (249, 115, 22)]
    for k in range(16):
        x, y = rnd.randint(170, 420), rnd.randint(40, 140)
        d.rounded_rectangle((x, y, x + rnd.randint(30, 60), y + rnd.randint(30, 60)), radius=6, fill=items[k % 6])
    caption(d, "RECEIPT: 4 FEET LONG", 315, 26)
    return im


def m11():  # Wright-Patt flyover
    im, d = canvas(gy=250)
    house(d, 220, 300, 160, lit=True)
    d.polygon([(60, 100), (300, 80), (360, 70), (300, 100), (80, 115)], fill=(120, 130, 140))
    d.polygon([(200, 90), (260, 40), (280, 40), (250, 92)], fill=(100, 110, 120))
    d.polygon([(200, 100), (250, 150), (270, 150), (250, 100)], fill=(100, 110, 120))
    for k in range(4): d.line((20 - k * 10, 95 + k * 7, -120, 95 + k * 7), fill=(255, 255, 255), width=3)
    for k in range(3): d.arc((200 - k * 16, 180 - k * 16, 400 + k * 16, 330 + k * 16), 200, 340, fill=(255, 255, 255), width=3)
    caption(d, "RATTLE RATTLE", 30, 30)
    return im


def m12():  # HOA letter about alien lawn ornaments
    im, d = canvas(gy=250)
    letter(d, 60, 40, 300, 220, "HOA NOTICE", "VIOLATION")
    # little alien lawn gnome
    d.ellipse((440, 150, 520, 230), fill=(34, 197, 94))
    d.ellipse((455, 170, 480, 195), fill=(255, 255, 255)); d.ellipse((485, 170, 510, 195), fill=(255, 255, 255))
    d.ellipse((462, 178, 474, 190), fill=(20, 20, 20)); d.ellipse((492, 178, 504, 190), fill=(20, 20, 20))
    d.rectangle((465, 230, 495, 290), fill=(34, 197, 94))
    d.polygon([(450, 160), (480, 90), (510, 160)], fill=(220, 38, 38))
    return im


def m13():  # 7 a.m. Sunday mower
    im, d = canvas(gy=240)
    d.ellipse((440, 20, 560, 140), fill=(253, 224, 71))
    d.line((470, 80, 495, 80), fill=(120, 80, 0), width=5); d.line((505, 80, 530, 80), fill=(120, 80, 0), width=5)
    d.arc((475, 90, 525, 120), 200, 340, fill=(120, 80, 0), width=5)
    d.rounded_rectangle((120, 220, 280, 270), radius=10, fill=(220, 38, 38))
    d.line((260, 225, 340, 150), fill=(40, 40, 40), width=8)
    for wx in (145, 255): d.ellipse((wx - 18, 255, wx + 18, 291), fill=(30, 30, 35))
    person(d, 360, 300, color=(234, 88, 12))
    caption(d, "BRRRRRRRR", 120, 44, fill=(255, 255, 255))
    caption(d, "SUNDAY 7:00 AM", 300, 26)
    return im


# ---------------------------------------------------------------- events

def e0():  # city council meeting
    im, d = canvas(sky=(120, 90, 70), ground=(90, 60, 45), gy=230)
    d.rectangle((190, 140, 410, 300), fill=(110, 70, 40))
    d.rectangle((170, 130, 430, 150), fill=(140, 90, 50))
    for k, x in enumerate((60, 520)): person(d, x, 300, color=(30, 60, 140), s=1.1)
    d.ellipse((260, 30, 340, 110), fill=(255, 255, 255), outline=(40, 40, 40), width=5)
    d.line((300, 70, 300, 42), fill=(40, 40, 40), width=5); d.line((300, 70, 322, 80), fill=(40, 40, 40), width=5)
    caption(d, "AGENDA ITEM 47 OF 112", 210, 24)
    return im


def e1():  # snow emergency
    im, d = canvas(sky=(170, 190, 210), ground=(245, 248, 252), gy=220)
    house(d, 60, 240, 160, lit=True)
    car(d, 330, 260, (59, 130, 246))
    d.pieslice((300, 170, 480, 320), 180, 360, fill=(245, 248, 252))
    for _ in range(60):
        x, y = rnd.randint(0, W), rnd.randint(0, H)
        d.ellipse((x - 5, y - 5, x + 5, y + 5), fill=(255, 255, 255))
    caption(d, "LEVEL 3", 40, 40, fill=(255, 255, 255), stroke=(30, 64, 175))
    return im


def e2():  # concert night at the Rose
    im, d = canvas(sky=(20, 15, 40), ground=(40, 30, 60), gy=250)
    for k, c in enumerate([(236, 72, 153), (59, 130, 246), (250, 204, 21)]):
        d.polygon([(150 + k * 150, 0), (130 + k * 150, 0), (60 + k * 150, 250), (240 + k * 150, 250)], fill=tuple(int(v * 0.5) for v in c))
    d.rectangle((120, 200, 480, 260), fill=(70, 60, 90))
    for x in (60, 480):
        d.rectangle((x, 120, x + 60, 260), fill=(20, 20, 25))
        d.ellipse((x + 12, 150, x + 48, 186), fill=(80, 80, 90)); d.ellipse((x + 8, 200, x + 52, 244), fill=(80, 80, 90))
    for _ in range(8):
        x, y = rnd.randint(150, 450), rnd.randint(40, 170)
        d.text((x, y), rnd.choice(["♪", "♫"]), font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 40) if os.path.exists("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf") else font(40), fill=(255, 255, 255))
    return im


def e3():  # another car wash
    im, d = canvas(gy=250)
    d.rectangle((120, 80, 480, 260), fill=(56, 189, 248))
    d.rectangle((160, 130, 440, 260), fill=(14, 116, 144))
    car(d, 220, 260, (220, 38, 38), w=160)
    for _ in range(25):
        x, y, r = rnd.randint(140, 460), rnd.randint(110, 250), rnd.randint(8, 22)
        d.ellipse((x - r, y - r, x + r, y + r), outline=(255, 255, 255), width=3)
    d.text((300, 102), "GRAND OPENING", font=font(30), fill=(255, 255, 255), anchor="mm")
    caption(d, "(the 5th one)", 300, 26)
    return im


def e4():  # power outage
    im, d = canvas(sky=(15, 20, 40), ground=(25, 35, 30), gy=240)
    house(d, 80, 270, 200, lit=False)
    d.line((0, 80, W, 110), fill=(30, 30, 30), width=4)
    d.rectangle((560, 70, 572, 260), fill=(70, 50, 35))
    d.ellipse((380, 70, 430, 102), fill=(140, 100, 70))
    d.ellipse((415, 62, 440, 84), fill=(140, 100, 70))
    d.ellipse((350, 60, 395, 98), fill=(160, 120, 85))
    pts = [(300, 120), (320, 150), (305, 150), (330, 190), (300, 155), (315, 155)]
    d.polygon(pts, fill=(250, 204, 21))
    caption(d, "A SQUIRREL. AGAIN.", 300, 28)
    return im


def e5():  # free sample day
    im, d = canvas(sky=(250, 245, 235), ground=(230, 225, 215), gy=200)
    d.ellipse((90, 170, 510, 290), fill=(200, 200, 210))
    d.ellipse((100, 172, 500, 280), fill=(230, 232, 238))
    cols = [(220, 38, 38), (250, 204, 21), (34, 197, 94), (168, 85, 247), (249, 115, 22)]
    for k in range(12):
        x = 140 + (k % 6) * 56
        y = 200 + (k // 6) * 40
        d.polygon([(x, y), (x + 34, y), (x + 28, y + 30), (x + 6, y + 30)], fill=(255, 255, 255), outline=(180, 180, 190))
        d.ellipse((x + 4, y - 4, x + 30, y + 8), fill=cols[k % 5])
    caption(d, "LITTLE CUPS OF EVERYTHING", 70, 30, fill=(30, 30, 40), stroke=(250, 245, 235))
    return im


def e6():  # property tax reassessment
    im, d = canvas(sky=(50, 70, 60), ground=(50, 70, 60))
    letter(d, 140, 50, 320, 240, "TAX BILL", "REASSESSED")
    d.text((300, 185), "$ +++", font=font(30), fill=(220, 38, 38), anchor="mm")
    return im


def e7():  # air show
    im, d = canvas(sky=(110, 180, 240), gy=290)
    for k in range(3):
        y = 80 + k * 55
        x = 420 - k * 60
        d.line((0, y + 10, x, y + 10), fill=(255, 255, 255), width=10)
        d.polygon([(x, y), (x + 90, y + 8), (x + 100, y + 12), (x, y + 22)], fill=(80, 90, 110))
        d.polygon([(x + 20, y + 10), (x + 50, y - 16), (x + 60, y - 16), (x + 50, y + 10)], fill=(60, 70, 90))
    for x in range(30, W, 60): person(d, x, 340, color=rnd.choice([(220, 38, 38), (59, 130, 246), (250, 204, 21)]), s=0.6)
    return im


def e8():  # citywide garage sale
    im, d = canvas(gy=230)
    house(d, 330, 240, 230)
    d.rectangle((60, 200, 320, 216), fill=(160, 110, 60))
    d.line((80, 216, 80, 290), fill=(120, 80, 40), width=8); d.line((300, 216, 300, 290), fill=(120, 80, 40), width=8)
    for k, c in enumerate([(59, 130, 246), (250, 204, 21), (220, 38, 38), (34, 197, 94), (168, 85, 247)]):
        d.rounded_rectangle((80 + k * 46, 160 - (k % 2) * 16, 116 + k * 46, 200), radius=6, fill=c)
    d.rectangle((120, 60, 280, 130), fill=(255, 255, 255), outline=(220, 38, 38), width=6)
    d.text((200, 95), "SALE", font=font(40), fill=(220, 38, 38), anchor="mm")
    d.rectangle((195, 130, 205, 160), fill=(120, 80, 40))
    return im


def e9():  # Old Troy Pike resurfacing
    im, d = canvas(gy=210)
    d.rectangle((0, 210, W, H), fill=(40, 40, 44))
    d.rectangle((0, 210, 260, H), fill=ROAD)
    d.rounded_rectangle((250, 150, 430, 250), radius=12, fill=(250, 204, 21))
    d.rectangle((280, 110, 360, 150), fill=(250, 204, 21))
    d.ellipse((380, 200, 480, 300), fill=(90, 90, 95)); d.ellipse((260, 230, 320, 290), fill=(30, 30, 35))
    for x in (60, 140, 520): cone(d, x, 320, 0.9)
    caption(d, "EST. COMPLETION: 2031", 50, 28)
    return im


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    ms = [m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13]
    es = [e0, e1, e2, e3, e4, e5, e6, e7, e8, e9]
    for i, f in enumerate(ms): save(f(), f"mishap_{i}.png")
    for i, f in enumerate(es): save(f(), f"event_{i}.png")
    print("wrote", len(ms), "mishaps and", len(es), "events")
