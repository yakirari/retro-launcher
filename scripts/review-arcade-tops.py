"""Generate enlarged source/cutout comparisons for reviewing cabinet roof contours."""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "artwork/arcade-presentations"
OUTPUT = ROOT / "artwork/arcade-presentations-review/top-review"


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    records = json.loads((ART / "front-transparent/manifest.json").read_text())["cutouts"]
    for start in range(0, len(records), 24):
        sheet = Image.new("RGB", (1840, 1056), "#536370")
        draw = ImageDraw.Draw(sheet)
        for i, record in enumerate(records[start:start + 24]):
            x, y = i % 4 * 460, i // 4 * 176
            for j, folder in enumerate(["front", "front-transparent"]):
                image = Image.open(ART / folder / record["asset"]).convert("RGBA")
                image = image.resize((460, 1000), Image.Resampling.LANCZOS).crop((0, 0, 460, 300))
                image.thumbnail((230, 150), Image.Resampling.LANCZOS)
                sheet.paste(image, (x + j * 230, y), image)
            draw.text((x + 4, y + 154), record["asset"], fill="white")
        sheet.save(OUTPUT / f"tops-{start // 24 + 1:02}.jpg", quality=95)


if __name__ == "__main__":
    main()