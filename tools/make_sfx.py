"""Synthesizes every sound in assets/sfx as 16-bit mono WAV. Run: python3 tools/make_sfx.py (needs numpy).
Placeholders: drop a recorded .wav with the same name into assets/sfx to replace one."""
import os, wave
import numpy as np

SR = 44100
OUT = "assets/sfx"
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(3)


def t(d): return np.arange(int(SR * d)) / SR


def env(n, a=0.005, r=0.1):
    e = np.ones(n)
    ai, ri = int(SR * a), int(SR * r)
    if ai: e[:ai] = np.linspace(0, 1, ai)
    if ri and ri < n: e[-ri:] = np.linspace(1, 0, ri) ** 2
    return e


def tone(f, d, kind="sine", a=0.005, r=0.1, vol=0.5):
    x = t(d)
    ph = 2 * np.pi * np.cumsum(np.full(len(x), f) if np.isscalar(f) else f) / SR
    if kind == "sine": w = np.sin(ph)
    elif kind == "square": w = np.sign(np.sin(ph)) * 0.6
    elif kind == "saw": w = 2 * ((ph / (2 * np.pi)) % 1) - 1
    else: w = np.sin(ph) + 0.3 * np.sin(2 * ph) + 0.15 * np.sin(3 * ph)
    return w * env(len(x), a, r) * vol


def noise(d, vol=0.5, a=0.002, r=0.05, lp=None):
    n = rng.standard_normal(int(SR * d))
    if lp:
        k = np.ones(lp) / lp
        n = np.convolve(n, k, mode="same")
        n /= np.max(np.abs(n)) + 1e-9
    return n * env(len(n), a, r) * vol


def seq(*parts, gap=0.0):
    out = []
    for p in parts:
        out.append(p)
        if gap: out.append(np.zeros(int(SR * gap)))
    return np.concatenate(out)


def mix(*xs):
    n = max(len(x) for x in xs)
    o = np.zeros(n)
    for x in xs: o[:len(x)] += x
    return o


def write(name, x):
    x = x / max(1.0, np.max(np.abs(x)) / 0.9)
    with wave.open(os.path.join(OUT, name + ".wav"), "w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())


# dice clatter: a few wooden knocks
clat = np.zeros(int(SR * 0.45))
for i in range(9):
    s = int(SR * (0.02 + i * 0.045 + rng.uniform(0, 0.02)))
    k = mix(noise(0.03, 0.6, r=0.025, lp=6), tone(rng.uniform(500, 900), 0.04, r=0.035, vol=0.35))
    clat[s:s + len(k)] += k * (1 - i * 0.07)
write("roll", clat)
write("click", mix(tone(1400, 0.03, r=0.025, vol=0.4), noise(0.01, 0.2)))
f = np.linspace(1200, 150, int(SR * 0.28))
write("zap", mix(tone(f, 0.28, "saw", r=0.1, vol=0.5), noise(0.12, 0.3)))
write("hit", mix(tone(np.linspace(160, 60, int(SR * 0.25)), 0.25, "sine", r=0.15, vol=0.9), noise(0.08, 0.5, lp=4)))
write("heal", seq(tone(523, 0.08, r=0.06), tone(659, 0.08, r=0.06), tone(784, 0.16, r=0.12)))
write("battery", mix(tone(1568, 0.12, "rich", r=0.1, vol=0.35), tone(2093, 0.18, r=0.16, vol=0.25)))
write("star", seq(tone(880, 0.06, "square", r=0.04, vol=0.25), tone(1320, 0.14, "square", r=0.12, vol=0.25)))
write("enter", seq(tone(392, 0.1, "rich"), tone(523, 0.1, "rich"), tone(659, 0.1, "rich"), tone(784, 0.3, "rich", r=0.25)))
write("leave", tone(np.linspace(1400, 300, int(SR * 0.5)), 0.5, "sine", r=0.1, vol=0.5))
write("ko", seq(tone(392, 0.28, "saw", r=0.08, vol=0.35), tone(370, 0.28, "saw", r=0.08, vol=0.35),
                tone(349, 0.28, "saw", r=0.08, vol=0.35), tone(np.linspace(330, 300, int(SR * 0.8)), 0.8, "saw", r=0.4, vol=0.35)))
write("win", seq(*[tone(fq, d, "rich", r=0.05) for fq, d in ((523, .12), (523, .12), (523, .12), (659, .3), (587, .12), (659, .12), (784, .6))]))
write("buy", seq(mix(noise(0.05, 0.4, lp=3), tone(2600, 0.05, r=0.04, vol=0.2)), tone(1760, 0.08, "rich", r=0.06, vol=0.3), tone(2637, 0.3, "rich", r=0.25, vol=0.3)))
write("mishap", seq(tone(311, 0.2, "saw", r=0.05, vol=0.3), tone(294, 0.2, "saw", r=0.05, vol=0.3), tone(277, 0.5, "saw", r=0.3, vol=0.3)))
write("sweep", noise(0.35, 0.35, a=0.1, r=0.2, lp=20))
write("turn", tone(988, 0.15, "rich", r=0.12, vol=0.3))
vib = 520 + 60 * np.sin(2 * np.pi * 5 * t(0.9))
write("ghost", tone(vib * np.linspace(1, 0.7, len(vib)), 0.9, "sine", a=0.15, r=0.4, vol=0.4))
write("power", seq(*[tone(fq, 0.07, "square", r=0.05, vol=0.22) for fq in (523, 659, 784, 1047, 1319)]))
print("wrote", len(os.listdir(OUT)), "sounds")
