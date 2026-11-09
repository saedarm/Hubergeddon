"""Synthesizes the two music loops in assets/music. Run: python3 tools/make_music.py (needs numpy; ffmpeg for .ogg).
lobby.ogg: elevator-music bossa for the menus. game.ogg: a light funk groove for the table.
Replace either with your own .ogg of the same name."""
import os, subprocess, wave
import numpy as np

SR = 32000
OUT = "assets/music"
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(9)


def note(m): return 440.0 * 2 ** ((m - 69) / 12)


def env(n, a, r):
    e = np.ones(n)
    ai, ri = max(1, int(SR * a)), max(1, int(SR * r))
    e[:ai] = np.linspace(0, 1, ai)
    if ri < n: e[-ri:] *= np.linspace(1, 0, ri)
    return e


def epiano(f, d, vol=0.18):
    t = np.arange(int(SR * d)) / SR
    w = np.sin(2 * np.pi * f * t + 0.8 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t * 5))
    return w * np.exp(-t * 1.8) * env(len(t), 0.004, 0.08) * vol


def organ(f, d, vol=0.07):
    t = np.arange(int(SR * d)) / SR
    w = sum(np.sin(2 * np.pi * f * h * t) / h for h in (1, 2, 3, 4))
    return w * env(len(t), 0.01, 0.05) * vol * (1 + 0.15 * np.sin(2 * np.pi * 6 * t))


def vibes(f, d, vol=0.12):
    t = np.arange(int(SR * d)) / SR
    return (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * f * 4 * t)) * np.exp(-t * 3) * (1 + 0.3 * np.sin(2 * np.pi * 5 * t)) * vol


def bass(f, d, vol=0.3):
    t = np.arange(int(SR * d)) / SR
    w = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(4 * np.pi * f * t)
    return w * np.exp(-t * 2.5) * env(len(t), 0.005, 0.05) * vol


def shaker(d=0.06, vol=0.05):
    n = rng.standard_normal(int(SR * d))
    n = np.diff(n, prepend=0)
    return n * np.exp(-np.arange(len(n)) / SR * 60) * vol


def rim(vol=0.12):
    t = np.arange(int(SR * 0.05)) / SR
    return (np.sin(2 * np.pi * 1700 * t) * 0.5 + rng.standard_normal(len(t)) * 0.4) * np.exp(-t * 90) * vol


def kick(vol=0.35):
    t = np.arange(int(SR * 0.18)) / SR
    return np.sin(2 * np.pi * (110 * np.exp(-t * 18) + 40) * t) * np.exp(-t * 14) * vol


def place(buf, x, at):
    i = int(at * SR)
    j = min(len(buf), i + len(x))
    if i < len(buf): buf[i:j] += x[:j - i]


def chord(root, kind):
    iv = {"maj7": [0, 4, 7, 11], "m7": [0, 3, 7, 10], "7": [0, 4, 7, 10], "m9": [0, 3, 7, 10, 14], "6": [0, 4, 7, 9]}[kind]
    return [root + i for i in iv]


def lobby():
    bpm, bars = 104, 16
    beat = 60 / bpm
    prog = [(60, "maj7"), (57, "m7"), (62, "m7"), (55, "7"), (64, "m7"), (57, "7"), (62, "m7"), (55, "7")] * 2
    buf = np.zeros(int(SR * beat * 4 * bars) + SR)
    melody = [76, 74, 72, 71, 72, 74, 76, 79, 77, 76, 74, 72, 74, 72, 71, 67]
    for b, (root, kind) in enumerate(prog):
        t0 = b * 4 * beat
        notes = chord(root, kind)
        for k, at in enumerate((0, 1.5, 2.5)):  # bossa comping
            for n in notes: place(buf, epiano(note(n), beat * 1.2, 0.07), t0 + at * beat)
        place(buf, bass(note(root - 24), beat * 1.4), t0)
        place(buf, bass(note(root - 24 + 7), beat * 1.0), t0 + 1.5 * beat)
        place(buf, bass(note(root - 24), beat * 1.0), t0 + 2 * beat)
        place(buf, bass(note(root - 24 + 7), beat * 1.0), t0 + 3.5 * beat)
        for e in range(8): place(buf, shaker(vol=0.035 if e % 2 else 0.05), t0 + e * beat / 2)
        for at in (0.75, 2, 3.25): place(buf, rim(0.06), t0 + at * beat)
        place(buf, vibes(note(melody[b % len(melody)]), beat * 2), t0 + 0.5 * beat)
        if b % 2: place(buf, vibes(note(melody[(b + 5) % len(melody)]), beat * 1.5), t0 + 2.5 * beat)
    return buf[:int(SR * beat * 4 * bars)]


def game():
    bpm, bars = 112, 16
    beat = 60 / bpm
    prog = [(57, "m9"), (57, "m9"), (62, "7"), (62, "7"), (60, "maj7"), (55, "7"), (57, "m7"), (64, "7")] * 2
    buf = np.zeros(int(SR * beat * 4 * bars) + SR)
    riff = [0, 0, 12, 0, 10, 0, 7, 5]
    for b, (root, kind) in enumerate(prog):
        t0 = b * 4 * beat
        for e, iv in enumerate(riff):
            place(buf, bass(note(root - 24 + iv), beat * 0.45, 0.26), t0 + e * beat / 2)
        for at in (0, 2): place(buf, kick(), t0 + at * beat)
        if b % 2: place(buf, kick(0.25), t0 + 2.75 * beat)
        for at in (1, 3): place(buf, rim(0.16), t0 + at * beat)
        for e in range(8): place(buf, shaker(vol=0.03 if e % 2 else 0.045), t0 + e * beat / 2)
        for at in (0.5, 1.75, 3): 
            for n in chord(root, kind)[:4]: place(buf, organ(note(n), beat * 0.4), t0 + at * beat)
        if b % 4 == 3:
            for k, n in enumerate((69, 72, 74, 76)): place(buf, vibes(note(n), beat * 0.6, 0.1), t0 + (2 + k * 0.5) * beat)
    return buf[:int(SR * beat * 4 * bars)]


def write(name, x):
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.8
    # smooth the loop seam
    f = int(SR * 0.01)
    x[:f] *= np.linspace(0, 1, f); x[-f:] *= np.linspace(1, 0, f)
    wav = os.path.join(OUT, name + ".wav")
    with wave.open(wav, "w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    try:
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", os.path.join(OUT, name + ".ogg")], check=True)
        os.remove(wav)
    except Exception as e:
        print("ffmpeg not available, kept", wav, e)


write("lobby", lobby())
write("game", game())
print("wrote", os.listdir(OUT))
