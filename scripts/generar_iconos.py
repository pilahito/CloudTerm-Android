#!/usr/bin/env python3
"""Genera los iconos de launcher de CloudTerm Android desde el icono maestro de escritorio."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
MASTER = Path(r"C:\Users\David\src\cloudterm\docs\brand\icon-1024.png")
RES = ROOT / "app" / "src" / "main" / "res"

DENSITIES = {
    "mdpi": 1,
    "hdpi": 1.5,
    "xhdpi": 2,
    "xxhdpi": 3,
    "xxxhdpi": 4,
}


def round_clip(img: Image.Image, radius_ratio: float = 0.22) -> Image.Image:
    img = img.convert("RGBA")
    w, h = img.size
    r = int(min(w, h) * radius_ratio)
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=r, fill=255)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.paste(img, (0, 0))
    out.putalpha(mask)
    return out


def save_png(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG")
    print(f"  {path.relative_to(ROOT)} {img.size[0]}x{img.size[1]}")


def main() -> None:
    master = Image.open(MASTER).convert("RGBA")
    print(f"maestro {MASTER} {master.size}")

    for name, scale in DENSITIES.items():
        size = int(48 * scale)
        launcher = master.resize((size, size), Image.Resampling.LANCZOS)
        folder = RES / f"mipmap-{name}"
        save_png(launcher, folder / "ic_launcher.png")
        save_png(round_clip(launcher), folder / "ic_launcher_round.png")

        fg = int(108 * scale)
        pad = int(fg * 0.18)
        canvas = Image.new("RGBA", (fg, fg), (0, 0, 0, 0))
        inner = master.resize((fg - 2 * pad, fg - 2 * pad), Image.Resampling.LANCZOS)
        canvas.paste(inner, (pad, pad), inner)
        save_png(canvas, RES / f"mipmap-{name}" / "ic_launcher_foreground.png")

        notif = int(24 * scale)
        mono = master.resize((notif, notif), Image.Resampling.LANCZOS)
        save_png(mono, RES / f"drawable-{name}" / "ic_notification.png")

    play = master.resize((512, 512), Image.Resampling.LANCZOS)
    save_png(play, ROOT / "store" / "icon-512.png")

    feature = Image.new("RGBA", (1024, 500), (7, 11, 17, 255))
    scaled = master.resize((360, 360), Image.Resampling.LANCZOS)
    feature.paste(scaled, (80, 70), scaled)
    draw = ImageDraw.Draw(feature)
    try:
        from PIL import ImageFont
        font = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 64)
        small = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 28)
    except OSError:
        font = ImageFont.load_default()
        small = font
    draw.text((480, 160), "CloudTerm", fill=(34, 211, 238), font=font)
    draw.text((480, 240), "SSH · SFTP · Pixel Agents", fill=(226, 236, 248), font=small)
    save_png(feature, ROOT / "store" / "feature-graphic.png")


if __name__ == "__main__":
    main()
