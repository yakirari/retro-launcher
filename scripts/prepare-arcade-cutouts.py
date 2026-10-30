"""Offline cabinet segmentation. Originals/RGB/dimensions remain unchanged; only alpha is derived.

Requires rembg[cpu]==2.0.86, Pillow, numpy, scipy. Models run locally, never upload images.
Outputs are review copies only; this script never publishes artwork into the app.
"""
import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
REVIEW = ROOT / "artwork/arcade-presentations-review"
ART = ROOT / "artwork/arcade-presentations"


def sha(file):
    return hashlib.sha256(file.read_bytes()).hexdigest()


def silhouette(mask):
    """Keep the principal connected cabinet and fill its interior, including dark CRT/coin door.

    Segmentation confidence is not material transparency. A solid machine must not become
    translucent just because its printing/black plastic resembles the background.
    """
    binary = np.asarray(mask) >= 128
    labels, count = ndimage.label(binary)
    if not count:
        raise ValueError("Empty foreground")
    sizes = np.bincount(labels.ravel())
    sizes[0] = 0
    binary = ndimage.binary_fill_holes(labels == sizes.argmax())
    # Smooth only within the original 1-pixel contour; opaque interior stays byte-exact.
    inside = ndimage.distance_transform_edt(binary)
    outside = ndimage.distance_transform_edt(~binary)
    alpha = np.clip(128 + (inside - outside) * 127, 0, 255).astype(np.uint8)
    return alpha


def reviewed_silhouette(size, review):
    """Rasterize a source-specific, manually reviewed outline with antialiased edges."""
    scale = 4
    mask = Image.new("L", (size[0] * scale, size[1] * scale))
    points = [(round(x * size[0] * scale / 460), round(y * size[1] * scale / 1000))
              for x, y in review["outline"]]
    ImageDraw.Draw(mask).polygon(points, fill=255)
    return np.asarray(mask.resize(size, Image.Resampling.LANCZOS))


def corrected_roof(alpha, roof):
    """Remove connected studio strips above measured roof; preserve side rails/body."""
    result = alpha.copy()
    height, width = result.shape
    left, right = roof["left"], roof["right"]
    x0, x1 = round(left[0] * width / 460), round(right[0] * width / 460)
    for x in range(x0, x1 + 1):
        y = (left[1] + (right[1] - left[1]) * (x - x0) / (x1 - x0)) * height / 1000
        edge = max(0, round(y))
        # The marquee is solid from its roof to the retained body. A narrow
        # restored strip alone could leave an interior gap or detached roof.
        result[edge:round(220 * height / 1000), x] = 255
        result[:edge, x] = 0
    solid = result >= 128
    labels, _ = ndimage.label(solid)
    sizes = np.bincount(labels.ravel())
    sizes[0] = 0
    main = labels == sizes.argmax()
    top = np.arange(height)[:, None] < round(220 * height / 1000)
    result[top & ~main] = 0
    result[top & ndimage.binary_fill_holes(main)] = 255
    return result


def sheets(records, folder, *, output_folder=None):
    output_folder = output_folder or folder
    output_folder.mkdir(parents=True, exist_ok=True)
    for page in range(0, len(records), 24):
        subset = records[page:page + 24]
        sheet = Image.new("RGB", (1440, ((len(subset) + 7) // 8) * 400), "#303845")
        draw = ImageDraw.Draw(sheet)
        for i, record in enumerate(subset):
            x, y = i % 8 * 180, i // 8 * 400
            for yy in range(y, y + 374, 12):
                for xx in range(x, x + 180, 12):
                    draw.rectangle((xx, yy, xx + 11, yy + 11), fill="#536370" if ((xx-x)//12+(yy-y)//12)%2 else "#a6b4bd")
            image = Image.open(folder / record["asset"]).convert("RGBA")
            image.thumbnail((174, 370), Image.Resampling.LANCZOS)
            sheet.paste(image, (x + (180-image.width)//2, y + (374-image.height)//2), image)
            draw.text((x+4, y+380), record["asset"][:26], fill="white")
        sheet.save(output_folder / f"review-{page//24+1:02}.jpg", quality=92)


def main():
    parser = argparse.ArgumentParser(__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--model", default="isnet-general-use", choices=["isnet-general-use", "birefnet-general-lite", "u2net"])
    parser.add_argument("--only", nargs="*")
    parser.add_argument("--verify", action="store_true")
    parser.add_argument("--cached-masks", action="store_true", help="Rebuild without loading/downloading a model")
    args = parser.parse_args()
    imported = json.loads((REVIEW / "front-import.json").read_text())
    review_file = REVIEW / "cutout-outlines.json"
    review_data = json.loads(review_file.read_text()) if review_file.exists() else {}
    reviews = review_data.get("cabinets", {})
    roof_file = REVIEW / "cutout-roofs.json"
    roof_data = json.loads(roof_file.read_text()) if roof_file.exists() else {}
    roofs = roof_data.get("cabinets", {})
    if roofs:
        assert roof_data["sourceManifestSha256"] == sha(REVIEW / "front-import.json"), "Stale roof source collection"
    if reviews:
        assert review_data["sourceManifestSha256"] == sha(REVIEW / "front-import.json"), "Stale reviewed source collection"
    records = imported["fronts"]
    if args.only:
        names = set(args.only)
        records = [r for r in records if r["asset"] in names]
        assert {r["asset"] for r in records} == names
    args.output.mkdir(parents=True, exist_ok=True)
    session = None
    if not args.verify and not args.cached_masks:
        from rembg import new_session
        import onnxruntime as ort
        options = ort.SessionOptions()
        options.intra_op_num_threads = 4
        options.inter_op_num_threads = 1
        session = new_session(args.model, sess_opts=options, providers=["CPUExecutionProvider"])
    results = []
    for number, record in enumerate(records):
        source = ART / "front" / record["asset"]
        assert sha(source) == record["sha256"], f"Original changed: {source}"
        image = Image.open(source).convert("RGBA")
        output = args.output / record["asset"]
        mask_file = args.output / (record["asset"] + ".mask.png")
        if not args.verify:
            review = reviews.get(record["asset"])
            if review:
                alpha = reviewed_silhouette(image.size, review)
            else:
                if not mask_file.exists():
                    if session is None:
                        raise ValueError(f"Missing cached mask: {mask_file}")
                    mask = session.predict(image)[0]
                    mask.save(mask_file)
                alpha = silhouette(Image.open(mask_file).convert("L"))
            if record["asset"] in roofs:
                alpha = corrected_roof(alpha, roofs[record["asset"]])
            pixels = np.asarray(image).copy()
            pixels[:, :, 3] = np.minimum(pixels[:, :, 3], alpha)
            Image.fromarray(pixels).save(output)
        result = Image.open(output).convert("RGBA")
        assert result.size == image.size
        original, actual = np.asarray(image), np.asarray(result)
        assert np.array_equal(original[:, :, :3], actual[:, :, :3]), f"RGB altered: {source}"
        alpha = actual[:, :, 3]
        if record["asset"] in reviews:
            expected = reviewed_silhouette(image.size, reviews[record["asset"]])
            if record["asset"] in roofs:
                expected = corrected_roof(expected, roofs[record["asset"]])
            expected = np.minimum(original[:, :, 3], expected)
            assert np.array_equal(alpha, expected), f"Reviewed outline mismatch: {source}"
        assert np.any(alpha == 0) and np.any(alpha == 255), f"Missing transparency/solid foreground: {source}"
        ys, xs = np.where(alpha > 0)
        coverage = float(np.count_nonzero(alpha) / alpha.size)
        assert 0.15 < coverage < 0.99, f"Suspicious coverage {coverage}: {source}"
        results.append(dict(asset=record["asset"], sourceSha256=record["sha256"], sha256=sha(output),
                            width=image.width, height=image.height, bounds=[int(xs.min()), int(ys.min()), int(xs.max()+1), int(ys.max()+1)],
                            foregroundFraction=round(coverage, 6)))
        print(f"{number+1}/{len(records)} {record['asset']} {coverage:.3f}", flush=True)
    manifest = dict(version=1, model=args.model, processing="Local segmentation; largest connected silhouette, interior holes filled. Source RGB and canvas dimensions preserved; only alpha changed.",
                    source=imported["source"], cutouts=results)
    if reviews:
        manifest["processing"] += " Source-bound manually reviewed outlines override failed segmentation masks."
        manifest["reviewedOutlinesSha256"] = sha(review_file)
        manifest["reviewedOutlines"] = sorted(r["asset"] for r in records if r["asset"] in reviews)
    if roofs:
        manifest["roofCorrectionsSha256"] = sha(roof_file)
        manifest["roofCorrections"] = sorted(r["asset"] for r in records if r["asset"] in roofs)
    target = args.output / "manifest.json"
    if args.verify:
        assert json.loads(target.read_text()) == manifest, "Cutout manifest changed"
    else:
        target.write_text(json.dumps(manifest, indent=2) + "\n")
        sheets(results, args.output)
        if reviews:
            sheets([r for r in results if r["asset"] in reviews], args.output,
                   output_folder=REVIEW / "cutout-repairs")
    print(f"PASS: {len(results)} cutouts; source RGB/dimensions/hashes intact", flush=True)


if __name__ == "__main__":
    main()