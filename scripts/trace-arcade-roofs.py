"""Propose source-derived roof edges for visual review (never changes original images)."""
import json
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "artwork/arcade-presentations"


def trace(image):
    rgb = np.asarray(image.convert("RGB").resize((460, 1000), Image.Resampling.LANCZOS), dtype=float)[:300]
    # Roof trim is a sustained transition from studio background into the cabinet.
    # Earliest local contrast avoids following the brighter printed marquee below it.
    smooth = ndimage.gaussian_filter(rgb, sigma=(0.65, 0.65, 0))
    contrast = np.linalg.norm(smooth[2:] - smooth[:-2], axis=2)
    brightness = smooth[1:-1].max(axis=2)
    above = ndimage.uniform_filter1d(smooth.max(axis=2), 9, axis=0, origin=4)[1:-1]
    score = np.minimum(contrast, 50) + np.clip(brightness - above, 0, 35) * .4
    score -= np.arange(score.shape[0])[:, None] * .12
    # Trace a continuous roof from the left side to the right side. A fixed
    # roof height/angle is deliberately not imposed on different source images.
    cost = -score
    back = np.zeros(cost.shape, dtype=np.int16)
    total = cost[:, 0].copy()
    for x in range(1, 460):
        choices = np.full((7, len(total)), np.inf)
        for j, shift in enumerate(range(-3, 4)):
            shifted = np.roll(total, shift) + abs(shift) * 1.5
            if shift > 0:
                shifted[:shift] = np.inf
            elif shift < 0:
                shifted[shift:] = np.inf
            choices[j] = shifted
        pick = choices.argmin(axis=0)
        back[:, x] = pick - 3
        total = choices[pick, np.arange(len(total))] + cost[:, x]
    ys = np.empty(460, dtype=int)
    ys[-1] = total.argmin()
    for x in range(459, 0, -1):
        ys[x - 1] = ys[x] - back[ys[x], x]
    # Most roofs are straight between their raised side rails. Fit that span
    # robustly, so printing and compression noise cannot make a jagged roof.
    xs = np.arange(120, 410)
    best = None
    for slope in np.arange(-.15, .201, .005):
        intercepts = ys[xs] - slope * xs
        for intercept in np.arange(0, 180, 1):
            residual = np.abs(intercepts - intercept)
            support = np.count_nonzero(residual < 3)
            candidate = (support, -float(np.minimum(residual, 12).sum()), slope, intercept)
            if best is None or candidate > best:
                best = candidate
    _, _, slope, intercept = best
    center = np.rint(slope * np.arange(460) + intercept).astype(int)
    # Keep the raised side rails and sloping cabinet side from the measured trace.
    peak = 25 + np.argmin(ys[25:140])
    ys[peak + 12:420] = center[peak + 12:420]
    ys = ndimage.median_filter(ys, size=5)
    return np.maximum(ys - 1, 0)


def main():
    from PIL import ImageDraw
    folder = ROOT / "artwork/arcade-presentations-review/roof-proposals"
    folder.mkdir(exist_ok=True)
    rows = json.loads((ART / "front-transparent/manifest.json").read_text())["cutouts"]
    traces = {}
    roofs = {}
    # These sources have inset/offset, reversed, high-resolution, or unusually
    # shaped roofs. Do not apply a left-facing upright assumption to them.
    excluded = set("batman simpsn2p btoads captcomm offroad zookeep imgfight__conversion s1945iii outrunb dkongo uccops__conversion varthu__conversion mk2 mk3 mainev2p__conversion narc rtype2 galmedes__conversion macross2__conversion mhavoc".split())
    for start in range(0, len(rows), 24):
        sheet = Image.new("RGB", (1840, 1056), "#536370")
        draw = ImageDraw.Draw(sheet)
        for i, row in enumerate(rows[start:start + 24]):
            image = Image.open(ART / "front" / row["asset"]).convert("RGB").resize((460, 1000))
            ys = trace(image)
            traces[row["asset"]] = ys.tolist()
            if row["asset"].removesuffix(".png") not in excluded:
                # Only the central roof span is approved here. Outside it the
                # source-specific side rails/outline are kept unchanged.
                roofs[row["asset"]] = {"left": [120, int(ys[120])], "right": [410, int(ys[410])]}
            marked = image.crop((0, 0, 460, 300))
            ImageDraw.Draw(marked).line(list(enumerate(ys)), fill="#00ff80", width=2)
            x, y = i % 4 * 460, i // 4 * 176
            sheet.paste(marked.resize((230, 150)), (x, y))
            cutout = Image.open(ART / "front-transparent" / row["asset"]).convert("RGBA").resize((460, 1000)).crop((0, 0, 460, 300)).resize((230, 150))
            sheet.paste(cutout, (x + 230, y), cutout)
            draw.text((x + 4, y + 154), row["asset"], fill="white")
        sheet.save(folder / f"roofs-{start // 24 + 1:02}.jpg", quality=95)
    (folder / "traces.json").write_text(json.dumps(traces))
    import hashlib
    approved = dict(version=1, sourceManifestSha256=hashlib.sha256(
        (ROOT / "artwork/arcade-presentations-review/front-import.json").read_bytes()).hexdigest(),
        scope="Central roof only; original side rails and all pixels below y=220 retained", cabinets=roofs)
    (ROOT / "artwork/arcade-presentations-review/cutout-roofs.json").write_text(json.dumps(approved, indent=2) + "\n")


if __name__ == "__main__":
    main()