"""
Generates StopTime's sound effects (no recordings needed).
Run:  python3 tools/make_sounds.py   -> writes app/src/main/res/raw/*.ogg
"""
import numpy as np, subprocess, wave, os, tempfile

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")

def t(sec): return np.arange(int(SR * sec)) / SR

def env(n, attack=0.003, decay=0.08):
    x = np.arange(n) / SR
    a = np.clip(x / attack, 0, 1)
    return a * np.exp(-x / decay)

def tone(freq, sec, decay, harmonics=((1, 1.0),), attack=0.003):
    x = t(sec)
    s = sum(amp * np.sin(2 * np.pi * freq * h * x) for h, amp in harmonics)
    return s * env(len(x), attack, decay)

def place(track, sound, at):
    i = int(at * SR)
    track[i:i + len(sound)] += sound[: len(track) - i]

def save(name, s, peak=0.8):
    s = s / (np.max(np.abs(s)) + 1e-9) * peak
    pcm = (s * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
        tmp = f.name
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis",
                    "-q:a", "5", os.path.join(OUT, name + ".ogg")], check=True)
    os.remove(tmp)

# 1. START: short rising "bip" (two quick notes up)
s = np.zeros(int(SR * 0.16))
place(s, tone(880, 0.07, 0.025, ((1, 1), (2, .25))), 0.0)
place(s, tone(1320, 0.09, 0.03, ((1, 1), (2, .25))), 0.055)
save("snd_start", s)

# 2. STOP: solid lower "thock" (mechanical stopwatch feel)
x = t(0.12)
thock = np.sin(2 * np.pi * (520 * np.exp(-x * 18)) * x) * env(len(x), 0.001, 0.035)
click = np.random.default_rng(1).normal(0, 1, len(x)) * env(len(x), 0.0005, 0.004) * 0.4
save("snd_stop", thock + click)

# 3. UI CLICK: soft, tiny tick for menu buttons
x = t(0.05)
tick = tone(2200, 0.05, 0.008, ((1, 1), (1.5, .3))) + \
       np.random.default_rng(2).normal(0, 1, len(x)) * env(len(x), 0.0003, 0.003) * 0.3
save("snd_click", tick, peak=0.55)

# 4. PERFECT 1.00: bright ascending chime arpeggio (C-E-G-C) with shimmer
s = np.zeros(int(SR * 1.2))
bell = ((1, 1), (2, .5), (3, .22), (4.2, .1))
for i, f in enumerate([1046.5, 1318.5, 1568.0, 2093.0]):
    place(s, tone(f, 0.9, 0.35, bell), i * 0.085)
save("snd_perfect", s)

# 5. ACHIEVEMENT: two-note "ta-daa" fanfare, warmer (square-ish) tone
s = np.zeros(int(SR * 1.0))
brass = ((1, 1), (3, .33), (5, .2), (7, .14))
place(s, tone(784.0, 0.16, 0.09, brass, attack=0.01), 0.0)                    # G
place(s, tone(1046.5, 0.8, 0.30, brass, attack=0.012) * 1.1, 0.14)           # C (held)
place(s, tone(1318.5, 0.8, 0.30, ((1, 1), (2, .3))) * 0.5, 0.14)             # E (harmony)
save("snd_achievement", s)

print("Sounds written to", os.path.abspath(OUT))
