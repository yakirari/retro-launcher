"""Offline regression tests for reviewed cabinet outlines; no segmentation model required."""
import importlib.util
import json
from pathlib import Path
import unittest

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("cutouts", ROOT / "scripts/prepare-arcade-cutouts.py")
cutouts = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(cutouts)


class ReviewedCutoutTest(unittest.TestCase):
    def test_reviewed_outputs_are_solid_and_match_their_outlines(self):
        reviews = json.loads((cutouts.REVIEW / "cutout-outlines.json").read_text())
        self.assertEqual(reviews["sourceManifestSha256"], cutouts.sha(cutouts.REVIEW / "front-import.json"))
        for name, review in reviews["cabinets"].items():
            with self.subTest(cabinet=name):
                source = np.array(Image.open(cutouts.ART / "front" / name).convert("RGBA"))
                output = np.array(Image.open(cutouts.ART / "front-transparent" / name).convert("RGBA"))
                np.testing.assert_array_equal(source[:, :, :3], output[:, :, :3])
                expected = cutouts.reviewed_silhouette((source.shape[1], source.shape[0]), review)
                roofs = json.loads((cutouts.REVIEW / "cutout-roofs.json").read_text())["cabinets"]
                if name in roofs:
                    expected = cutouts.corrected_roof(expected, roofs[name])
                np.testing.assert_array_equal(output[:, :, 3], expected)
                solid = output[:, :, 3] >= 128
                self.assertEqual(ndimage.label(solid)[1], 1)
                np.testing.assert_array_equal(ndimage.binary_fill_holes(solid), solid)

    def test_previously_deleted_cabinet_details_are_opaque(self):
        points = {
            "19xx.png": [(250, 900), (45, 350)],
            "aburner2.png": [(250, 850)],
            "buckrog.png": [(80, 340)],
            "cbuster__conversion.png": [(250, 100)],
            "ecofghtr.png": [(250, 600)],
            "kodu__conversion.png": [(250, 100)],
            "mario.png": [(200, 600)],
            "mk2.png": [(250, 100)],
            "nitrobal__conversion.png": [(250, 400)],
            "xevious.png": [(250, 870)],
            "xmultipl__conversion.png": [(250, 100)],
        }
        for name, positions in points.items():
            with self.subTest(cabinet=name):
                alpha = Image.open(cutouts.ART / "front-transparent" / name).getchannel("A")
                for point in positions:
                    self.assertEqual(alpha.getpixel(point), 255, (name, point))


if __name__ == "__main__":
    unittest.main()