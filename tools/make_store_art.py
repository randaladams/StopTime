"""Generates the Google Play icon (512x512) and feature graphic (1024x500). Run: python3 tools/make_store_art.py"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "store")
GREEN, GREEN_D, RED, WHITE = (46, 125, 50), (27, 94, 32), (198, 40, 40), (255, 255, 255)
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
MONO = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
S = 4  # supersampling

def gradient(w, h, top, bottom):
    img = Image.new("RGB", (w, h), top)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(h - 1, 1)
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img

def stopwatch(d, cx, cy, r, face=WHITE, ring=WHITE, hand=RED, text=None):
    """Draws a stopwatch centred at (cx, cy) with face radius r."""
    w = r * 0.16
    # crown + side button
    d.rounded_rectangle([cx - r*0.22, cy - r*1.38, cx + r*0.22, cy - r*1.16], radius=r*0.05, fill=ring)
    d.rectangle([cx - r*0.09, cy - r*1.18, cx + r*0.09, cy - r*0.95], fill=ring)
    # outer ring
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=ring)
    inner = r - w
    d.ellipse([cx - inner, cy - inner, cx + inner, cy + inner], fill=face)
    # tick marks
    import math
    for i in range(12):
        a = math.radians(i * 30)
        r1, r2 = inner * 0.80, inner * (0.92 if i % 3 else 0.97)
        d.line([(cx + r1*math.sin(a), cy - r1*math.cos(a)), (cx + r2*math.sin(a), cy - r2*math.cos(a))],
               fill=GREEN_D, width=int(r * (0.05 if i % 3 == 0 else 0.03)))
    # hand pointing straight up (= exactly one revolution)
    d.line([(cx, cy), (cx, cy - inner * 0.74)], fill=hand, width=int(r * 0.08))
    d.ellipse([cx - r*0.1, cy - r*0.1, cx + r*0.1, cy + r*0.1], fill=hand)
    if text:
        f = ImageFont.truetype(MONO, int(r * 0.30))
        d.text((cx, cy + inner * 0.45), text, font=f, fill=GREEN_D, anchor="mm")

# ---------- 512x512 app icon (full square; Google Play rounds the corners) ----------
n = 512 * S
icon = gradient(n, n, (67, 160, 71), GREEN_D)
d = ImageDraw.Draw(icon)
stopwatch(d, n/2, n*0.56, n*0.33, text="1.00")
icon.resize((512, 512), Image.LANCZOS).save(os.path.join(OUT, "icon-512.png"))

# ---------- 1024x500 feature graphic ----------
W, H = 1024 * S, 500 * S
fg = gradient(W, H, (56, 142, 60), (20, 70, 24))
d = ImageDraw.Draw(fg)
# soft glow behind the watch
glow = Image.new("L", (W, H), 0)
ImageDraw.Draw(glow).ellipse([W*0.66 - H*0.42, H*0.53 - H*0.42, W*0.66 + H*0.42, H*0.53 + H*0.42], fill=90)
glow = glow.filter(ImageFilter.GaussianBlur(H * 0.08))
fg.paste(Image.new("RGB", (W, H), (129, 199, 132)), (0, 0), glow)
d = ImageDraw.Draw(fg)
stopwatch(d, W*0.78, H*0.56, H*0.30, text="1.00")
d.text((W*0.07, H*0.36), "StopTime", font=ImageFont.truetype(BOLD, int(H*0.20)), fill=WHITE, anchor="lm")
d.text((W*0.07, H*0.56), "Stop the clock at", font=ImageFont.truetype(BOLD, int(H*0.075)), fill=(220, 237, 200), anchor="lm")
d.text((W*0.07, H*0.66), "exactly 1.00 second.", font=ImageFont.truetype(BOLD, int(H*0.075)), fill=(220, 237, 200), anchor="lm")
d.text((W*0.07, H*0.82), "Can you?", font=ImageFont.truetype(BOLD, int(H*0.06)), fill=(255, 213, 79), anchor="lm")
fg.resize((1024, 500), Image.LANCZOS).save(os.path.join(OUT, "feature-graphic-1024x500.png"))
print("written to", os.path.abspath(OUT))
