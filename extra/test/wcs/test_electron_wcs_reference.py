#!/usr/bin/env python3

import math
import unittest

import numpy as np
from astropy.io import fits

from validate_jhv_wcs_against_astropy import (
    build_astropy_pixel_wcs,
    build_jhv_meta,
    build_projection_only_wcs,
)
from validate_jhv_wcs_with_electron import expected_world_deg_for_mode


def reference_header(projection, observer_distance=10.0):
    return fits.Header(
        {
            "NAXIS": 2,
            "NAXIS1": 512,
            "NAXIS2": 512,
            "CTYPE1": f"HPLN-{projection}",
            "CTYPE2": f"HPLT-{projection}",
            "CUNIT1": "deg",
            "CUNIT2": "deg",
            "CRPIX1": 257.25,
            "CRPIX2": 254.75,
            "CRVAL1": 0.08,
            "CRVAL2": -0.06,
            "CDELT1": 0.03,
            "CDELT2": 0.04,
            "PC1_1": 0.96,
            "PC1_2": -0.28,
            "PC2_1": 0.28,
            "PC2_2": 0.96,
            "DSUN_OBS": observer_distance * 695700000.0,
            "PV2_1": 1.0,
            "PV2_2": 0.0,
            "PV2_3": 0.7 if projection == "ZPN" else 0.0,
        }
    )


class ElectronWcsReferenceTest(unittest.TestCase):
    def test_formal_ortho_uses_sphere_viewing_angles(self):
        # This point lies on the visible unit sphere. Its viewing angles must
        # not depend on CRVAL, PV, or an inverse projection of a guessed plane.
        x, y, z = 0.48, -0.36, 0.8
        for projection in ("ARC", "AZP", "ZPN"):
            for distance in (10.0, 215.0):
                with self.subTest(projection=projection, distance=distance):
                    meta = build_jhv_meta(reference_header(projection, distance))
                    expected = (
                        math.degrees(math.atan2(x, distance - z)),
                        math.degrees(
                            math.asin(
                                y / math.sqrt(x * x + y * y + (distance - z) ** 2)
                            )
                        ),
                    )
                    actual = expected_world_deg_for_mode(
                        "ortho", (x + 1.0) / 2.0, (y + 1.0) / 2.0, meta, {}, None
                    )
                    np.testing.assert_allclose(actual, expected, rtol=0, atol=1e-12)

    def test_tan_ortho_keeps_planar_shortcut(self):
        header = reference_header("TAN")
        meta = build_jhv_meta(header)
        projection_wcs = build_projection_only_wcs(header)
        pixel_wcs = build_astropy_pixel_wcs(header)
        world = expected_world_deg_for_mode(
            "ortho", 0.74, 0.32, meta, {}, projection_wcs
        )
        actual = pixel_wcs.wcs_world2pix([world], 1)[0]
        plane = np.degrees(np.array([0.48, -0.36]) / meta.plane_units_per_rad)
        plane -= [header["CRVAL1"], header["CRVAL2"]]
        expected = np.linalg.solve(pixel_wcs.pixel_scale_matrix, plane)
        expected += [header["CRPIX1"], header["CRPIX2"]]
        np.testing.assert_allclose(actual, expected, rtol=0, atol=1e-9)

    def test_disk_diagnostic_excludes_off_limb(self):
        # This diagnostic restricts itself to the disk. Full-fragment sphere/plane
        # coverage is checked separately by test_wcs_rendering.py.
        meta = build_jhv_meta(reference_header("ZPN"))
        actual = expected_world_deg_for_mode("ortho", 0.99, 0.99, meta, {}, None)
        self.assertTrue(all(math.isnan(value) for value in actual))


if __name__ == "__main__":
    unittest.main()
