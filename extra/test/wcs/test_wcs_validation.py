#!/usr/bin/env python3

"""Check that the validators reject broken results, not just accept good ones."""

import contextlib
import io
import math
import tempfile
import time
import unittest
from pathlib import Path
from unittest.mock import patch

import numpy as np

import run_jhv_wcs_hpc_validation_suite as suite
import validate_jhv_wcs_against_astropy as cpu
import validate_jhv_wcs_with_electron as gpu
from test_electron_wcs_reference import reference_header
from test_wcs_rendering import (
    image_header,
    orthographic_geometry,
    verify_colors,
    verify_coordinates,
    verify_footprint,
)


class ValidationTest(unittest.TestCase):
    def setUp(self):
        self.output = tempfile.TemporaryDirectory()
        self.addCleanup(self.output.cleanup)
        self.root = Path(self.output.name)
        self.file = Path("synthetic.fits")
        self.header = reference_header("TAN")
        self.meta = cpu.build_jhv_meta(self.header)
        self.wcs = cpu.build_astropy_pixel_wcs(self.header)
        self.projection = cpu.build_projection_only_wcs(self.header)
        self.image = np.ones((512, 512))
        self.job = gpu.make_mode_job(
            "ortho", self.file, 8, self.root, True, self.meta, "default"
        )
        self.result = {
            "renderer": "test",
            "clearError": 0,
            "drawError": 0,
            "readError": 0,
            "errors": {},
        }
        self.pixels = np.zeros((8, 8, 4))
        texture = gpu.synthetic_texture(
            self.job["textureWidth"], self.job["textureHeight"]
        )
        for y in range(8):
            for x in range(8):
                uv = gpu.cpu_texcoord_for_mode(
                    "ortho", (x + 0.5) / 8, (y + 0.5) / 8, self.meta, self.image, 1, 0
                )
                if gpu.is_finite_texcoord(uv):
                    self.pixels[y, x] = (*uv, cpu.sample_texture_linear(texture, uv), 1)
        self.quiet = contextlib.redirect_stdout(io.StringIO())
        self.quiet.__enter__()
        self.addCleanup(self.quiet.__exit__, None, None, None)

    def inverse(self):
        return cpu.run_inverse_validation(
            self.file,
            self.projection,
            self.wcs,
            self.meta,
            16,
            0,
            0,
            True,
            False,
            False,
            False,
            False,
            False,
        )

    def forward(self):
        return cpu.run_forward_validation(
            self.file, self.projection, self.wcs, self.meta, 16, 0, 0, False, 0.01
        )

    def evaluate(self, reference=None):
        return gpu.evaluate_shader_to_cpu(
            "ortho",
            self.file,
            8,
            0.01,
            0.001,
            True,
            self.image,
            self.meta,
            self.projection,
            reference or self.wcs,
            self.job,
            self.result,
            self.pixels,
        )

    def test_baselines(self):
        self.assertEqual(self.inverse(), 0)
        self.assertEqual(self.forward(), 0)
        self.assertEqual(self.evaluate(), 0)

    def test_wrong_inverse(self):
        with patch.object(
            cpu, "project_plane_internal_to_world", return_value=(0.5, 0.25)
        ):
            self.assertNotEqual(self.inverse(), 0)

    def test_nonfinite_inverse(self):
        with patch.object(
            cpu, "project_plane_internal_to_world", return_value=(math.nan, math.nan)
        ):
            self.assertNotEqual(self.inverse(), 0)

    def test_nonfinite_forward(self):
        with patch.object(
            cpu, "mirrored_world_to_pixel_center", return_value=(math.nan, math.nan)
        ):
            self.assertNotEqual(self.forward(), 0)

    def test_missing_hpc_pixels(self):
        original = cpu.renderHpcTexcoords

        def missing_right_half(screen, *args):
            result = original(screen, *args)
            return ((math.nan, math.nan), *result[1:]) if screen[0] > 0.5 else result

        with patch.object(cpu, "save_png"):
            args = (self.file, self.root, 8, self.meta, self.image, self.wcs, 0.01)
            self.assertEqual(cpu.run_hpc_render_compare(*args), 0)
            with patch.object(
                cpu, "renderHpcTexcoords", side_effect=missing_right_half
            ):
                self.assertNotEqual(cpu.run_hpc_render_compare(*args), 0)

    def test_nonfinite_gpu_intensity(self):
        self.pixels[self.pixels[:, :, 3] > 0, 2] = math.nan
        with self.assertRaises(ValueError):
            self.evaluate()

    def test_gpu_errors(self):
        for key in ("clearError", "drawError", "readError"):
            with self.subTest(key=key), self.assertRaises(RuntimeError):
                gpu.check_gl_errors({**self.result, key: 1282})
        self.result["errors"] = {"ubo": 1280}
        with self.assertRaises(RuntimeError):
            self.evaluate()

    def test_missing_astropy_reference(self):
        class InvalidReference:
            def wcs_world2pix(self, points, origin):
                return np.full((len(points), 2), np.nan)

        self.assertNotEqual(self.evaluate(InvalidReference()), 0)

    def test_partial_astropy_reference(self):
        original = self.wcs.wcs_world2pix

        def partial(points, origin):
            result = original(points, origin)
            result[np.asarray(points)[:, 0] > 0] = np.nan
            return result

        with patch.object(self.wcs, "wcs_world2pix", side_effect=partial):
            self.assertNotEqual(self.evaluate(), 0)

    def test_missing_surface_map_coordinates(self):
        header = image_header("CAR")
        meta, wcs = cpu.build_jhv_meta(header), cpu.build_astropy_pixel_wcs(header)
        original = cpu.mirrored_world_array_to_pixel_center

        def missing(world, metadata):
            result = original(world, metadata)
            result[0, 0] = np.nan
            return result

        args = (self.file, self.root, 1, meta, np.ones((128, 128)), wcs, 0.01)
        with patch.object(cpu, "save_png"):
            self.assertEqual(cpu.run_surface_map_render_compare(*args), 0)
            with patch.object(
                cpu, "mirrored_world_array_to_pixel_center", side_effect=missing
            ):
                self.assertNotEqual(cpu.run_surface_map_render_compare(*args), 0)

    def test_missing_diff_coverage(self):
        pixels = np.zeros((8, 8, 4))
        pixels[0, 0] = (0.5, 0.5, 0.5, 0.5)
        self.assertNotEqual(
            gpu.evaluate_hpc_diff_selfcheck(
                self.file, 8, 0.01, 0.001, self.meta, self.job, self.result, pixels
            ),
            0,
        )

    def test_parallel_failure_is_preserved(self):
        runs = [
            suite.ValidationRun("slow_pass", ()),
            suite.ValidationRun("fast_failure", ()),
        ]

        def run_case(run):
            if run.name == "slow_pass":
                time.sleep(0.1)
            return suite.ValidationResult(run, int(run.name == "fast_failure"), "", "")

        with patch.object(suite, "run_case", side_effect=run_case):
            results = suite.run_parallel(runs, keep_going=False, jobs=2)
        self.assertTrue(any(result.returncode != 0 for result in results))

    def test_diagnostics_are_identified(self):
        self.assertTrue(
            suite.ValidationRun("preview", ("--orthographic-render",)).diagnostic
        )
        self.assertFalse(suite.ValidationRun("inverse", ("--inverse-zpn",)).diagnostic)

    def test_sphere_and_plane_geometry(self):
        world, valid, disk = orthographic_geometry(
            np.array([0.6, 1.2]), np.zeros(2), np.eye(3)
        )
        np.testing.assert_allclose(world, [[0.6, 0, 0.8], [1.2, 0, 0]], atol=1e-14)
        np.testing.assert_array_equal(valid, [True, True])
        np.testing.assert_array_equal(disk, [True, False])

    def test_rotated_plane_and_back_side(self):
        rotation = np.array(
            [[-0.5, 0, math.sqrt(0.75)], [0, 1, 0], [-math.sqrt(0.75), 0, -0.5]]
        )
        world, valid, _ = orthographic_geometry(
            np.array([0.0, 1.2]), np.zeros(2), rotation
        )
        np.testing.assert_array_equal(valid, [False, True])
        self.assertAlmostEqual(world[1, 2], 0)
        _, surface_valid, _ = orthographic_geometry(
            np.array([0.0, 1.2]), np.zeros(2), rotation, True
        )
        np.testing.assert_array_equal(surface_valid, [True, False])

    def test_bad_production_colors(self):
        lut = np.column_stack(
            (np.arange(256), 255 - np.arange(256), np.full(256, 80), np.full(256, 255))
        )
        black = np.zeros((2, 2, 4))
        black[..., 3] = 1
        with self.assertRaises(AssertionError):
            verify_colors(
                black,
                np.full((2, 2), 0.6),
                np.ones((2, 2), dtype=bool),
                lut,
                np.ones(4),
            )

    def test_bad_production_coordinates(self):
        good = np.full((2, 2, 4), 0.5)
        good[..., 3] = 1
        uv, valid = good[..., :2].copy(), np.ones((2, 2), dtype=bool)
        verify_coordinates(good, uv, valid)
        good[0, 0, 0] += 0.01
        with self.assertRaises(AssertionError):
            verify_coordinates(good, uv, valid)

    def test_observer_image_does_not_wrap(self):
        pixels = np.array([[[1, 0.5, 0, 1]]])
        uv, valid = np.array([[[0, 0.5]]]), np.ones((1, 1), dtype=bool)
        with self.assertRaises(AssertionError):
            verify_coordinates(pixels, uv, valid)
        verify_coordinates(pixels, uv, valid, wrap_longitude=True)

    def test_footprint_limits(self):
        valid = np.ones((40, 40), dtype=bool)
        actual = valid.copy()
        boundary = np.zeros_like(valid)
        boundary[0] = True
        actual[0, 0] = False
        verify_footprint(actual, valid, boundary)
        actual[0, 1] = False  # Too many boundary losses.
        with self.assertRaises(AssertionError):
            verify_footprint(actual, valid, boundary)
        actual[:] = valid
        actual[20, 20] = False
        with self.assertRaises(AssertionError):
            verify_footprint(actual, valid, boundary)

    def test_boundary_tolerance_does_not_hide_interior_loss(self):
        self.assertTrue(gpu.near_image_edge([0.1, 30], 128, 128, 0.5))
        self.assertFalse(gpu.near_image_edge([2, 30], 128, 128, 0.5))
        self.assertFalse(gpu.near_image_edge([0.1, 180], 128, 128, 0.5))


if __name__ == "__main__":
    unittest.main()
