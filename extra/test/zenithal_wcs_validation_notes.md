---
title: |
   | SWHV CCN4
   | Zenithal WCS and HPC Validation Notes
subtitle: SWHV-ROB-TN-001-CCN4 v0.9
subject: SWHV CCN4 
date: SWHV-ROB-TN-001-CCN4 - Version 0.9 - 2026-03-xx
lof: true
lot: false
---

`id: \exec{git hash-object \file}`

# Summary

This note describes the zenithal WCS and `HPC` work implemented in JHV, and
the corresponding validation against Astropy and direct internal comparison
tests.

In this work, a new `TAN` path was implemented together with `AZP` and
six-term `ZPN`, in line with the theoretical foundation of
Greisen and Calabretta, "Representations of celestial coordinates in FITS"
([A&A 395, 1077-1122, 2002](https://www.aanda.org/articles/aa/abs/2002/45/aah3859/aah3859.html)).
Hereafter, `formal-TAN` denotes this Greisen & Calabretta `TAN` formulation in
JHV.

The JHV `TAN` implementation of previous versions is hereafter denoted as
`simple-TAN`. It treats the orthographic surface point's planar `(x,y)`
coordinates as a small-angle approximation to helioprojective coordinates. It
then feeds those coordinates directly into the linear image transform, instead of
first converting the 3D point to helioprojective angles and then applying the
`formal-TAN` world-to-plane projection.

An `HPC` projection mode was also implemented in JHV and validated as part of
the same work; its solar-coordinate interpretation follows Thompson, "Coordinate
systems for solar image data"
([A&A 449, 791-803,
2006](https://www.aanda.org/articles/aa/abs/2006/14/aa4262-05/aa4262-05.html)).
Because `HPC` is fundamentally tied to a specific observer viewpoint, it is
not a natural fit for the multi-viewpoint aspect of JHV.

Heliospheric imager datasets often use `AZP` or `ZPN` projections. For these
datasets, the validation in this note supports the correctness of the `HPC`
WCS and sampling path. That conclusion does not extend automatically to
`Orthographic` mode, where wide-angle heliospheric images may still not display
satisfactorily because their integration into the JHV 3D viewing model remains
a separate problem.

A dedicated validator was built in:

- [extra/test/validate_jhv_wcs_against_astropy.py](extra/test/validate_jhv_wcs_against_astropy.py)

Outside the renderer, it reproduces the parts of the JHV WCS/HPC mapping path
covered by this note and compares them directly against Astropy wherever
the result is fully defined by WCS.

The validator was applied to a variety of real instrument FITS files and
metadata, including COR2, HI1, HI2, SDO/AIA, Solar Orbiter/EUI, and PSP/WISPR
examples.

The work reported here includes:

- validating `formal-TAN`, non-slanted `AZP`, and six-term `ZPN` (primary
  branch only) against Astropy, including round-trip checks
- validating the `HPC` sampling path against Astropy

Main conclusions:

- `formal-TAN`, `AZP`, and `ZPN` are validated against Astropy, including the
  tested inverse mappings
- JHV `HPC` rendering is validated against Astropy for the WCS and sampling
  path covered by this note
- direct comparison between the `formal-TAN` path in `Orthographic` mode and
  `HPC` shows that the two display geometries are not identical even with the
  same observer viewpoint and `dragRotation = 0`
- the measured discrepancies between `formal-TAN` in `Orthographic` mode and
  `HPC` are consistent with the theoretical geometric discrepancy derived in
  Appendix A
- on the sample TAN files, `simple-TAN` remains very close to the `HPC`
  display geometry, which suggests it may be a better choice than
  `formal-TAN` for JHV `Orthographic` mode when visual consistency with `HPC`
  is preferred
- this note is therefore a validation/testing note, not a design note for
  `Orthographic` embedding, playback policy, mouse-position reporting, or
  synthetic overdrawing

Bottom line:

- `simple-TAN` will be kept in JHV `Orthographic` mode for data designated as
  `TAN` in the metadata
- `HPC` mode was added (with the noted caveat about the viewpoint)
- support for `AZP` and `ZPN` data was added (with the noted caveat about the heliospheric imagers)

# The validator

## JHV behavior modeled by the validator

For the validation modes in this note, the script provides a Python/CPU model
of the relevant JHV reprojection and sampling logic. It does not execute the
actual JHV renderer. In particular, major parts of the corresponding code in
JHV run in GLSL on the GPU, whereas the validator re-expresses that logic in
Python for comparison and testing. It therefore models the reprojection and
sampling path, not the full interactive renderer.

For each screen pixel, JHV first determines the corresponding point on the
display geometry. In `HPC` mode, that geometry is the `HPC` display plane; in
`Orthographic` mode, it is the 3D solar scene used by the ortho renderer. From that
display point, JHV derives the helioprojective coordinates needed to evaluate
the image WCS. For zenithal FITS data in the formal WCS paths, this means using the inverse form of
the relevant projection model, such as `TAN`, `AZP`, or `ZPN`, and then
applying the linear WCS terms (`CRVAL`, `CRPIX`, `CDELT`, and `PC`/`CROTA`) to
obtain the source-image coordinates. The screen pixel is then produced by
sampling the source image at those coordinates with interpolation. In this sense, the WCS projection
determines how image coordinates relate to observer geometry, while the JHV
rendering mode determines which display geometry is sampled before that WCS
mapping is evaluated.

The sampling pipeline described above, and the point at which `simple-TAN`
departs from the formal WCS path, can be summarized as follows:

```text
formal WCS:
screen pixel
  -> display geometry point
  -> helioprojective coordinates
  -> WCS world-to-plane step
  -> linear WCS terms
  -> source-image coordinates
  -> interpolated image sample

simple-TAN:
screen pixel
  -> display geometry point
  -> (x,y) coordinates in the image-view plane
  -> linear WCS terms
  -> source-image coordinates
  -> interpolated image sample
```

In `simple-TAN`, the `(x,y)` pair refers to the display point's coordinates in
the image-view plane, i.e. the plane perpendicular to the viewing direction in
the image observer frame. Those coordinates are used directly as an
approximation to the helioprojective viewing angles, instead of first
converting the 3D point to helioprojective coordinates and then applying the
`formal-TAN` projection step.

\newpage

Included in the validator:

- FITS metadata parsing into the same effective quantities JHV uses:
  - `CRPIX`
  - `CRVAL`
  - `CDELT`
  - `PC`/`CROTA`
  - `DSUN_OBS`
  - `PV2_0..PV2_5`
- the shared WCS projection math for:
  - `TAN`
  - non-slanted `AZP`
  - six-term `ZPN` (primary branch only)
- the `HPC` image sampling path:
  - screen `HPC` coordinate -> helioprojective -> WCS plane -> source pixel
- the `HPC` display domain used in the validation runs reported here
- direct comparison between the `formal-TAN` path in `Orthographic` mode and
  `HPC` for the specific no-rotation comparison mode

Excluded:

- `dragRotation`
- `cameraDiff`
- differential diff-image alignment between two arbitrary live layers beyond the
  specific per-image `deltaT` reprojection modeled in the shader path
- `deltaCROTA`, `deltaCRVAL1`, `deltaCRVAL2`
- mouse-position reporting or synthetic overdrawing such as annotations, grid
  overlays, and similar on-screen constructs
- playback/view policy effects such as dynamic refitting of the visible `HPC`
  box or any other transient user-driven viewing state
- full OpenGL rasterization state, blending, and UI behavior

The validator therefore establishes:

- whether the implemented reprojection and sampling math matches Astropy
- whether the implemented `HPC` sampling map matches Astropy
- whether the `formal-TAN` path in `Orthographic` mode and `HPC` sample the
  same source pixels at the same displayed on-disk screen radius

## Astropy-based validation scope

Astropy can validate the parts of the mapping that are fully defined by WCS.

In this validator, Astropy is the external reference for:

- forward samples
  - JHV-style world/helioprojective -> WCS plane -> pixel
  - checked against `astropy.wcs.WCS.wcs_world2pix(...)`
- full pixel-center validation
  - every source-image pixel center is mapped through the JHV path and checked
    against Astropy
- inverse `TAN`
  - plane -> helioprojective, checked by round-trip against Astropy's forward
    WCS
- inverse `AZP`
  - plane -> helioprojective, checked by round-trip against Astropy's forward
    WCS
- inverse `ZPN` (primary branch only)
  - plane -> helioprojective, checked by the same round-trip strategy on the
  supported primary monotonic branch
- bounded `HPC` render comparison
  - the same `HPC` screen grid is run through the JHV sampling path and through
    Astropy `world -> pixel`
  - the script compares the resulting source-pixel centers and also writes diff
    images

Not compared against Astropy:

- direct comparison between the `formal-TAN` path in `Orthographic` mode and
  `HPC`
  - this is an internal JHV-to-JHV comparison

## How to use the validator

The main script is:

- [extra/test/validate_jhv_wcs_against_astropy.py](extra/test/validate_jhv_wcs_against_astropy.py)

Run it with:

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py <fits-file> [mode]
```

### Astropy-based validation modes

1. Forward WCS random-sample validation

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/20241224_194245_d4c2A.fts
```

This mode reports:

- `projection_max_error_internal`
- `pixel_center_max_error_px`

2. Full pixel-center validation

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/20250622_000831_s4h1A.fts \
  --all-pixels
```

This mode checks the full image grid against Astropy and reports the worst
pixel-center error.

3. Inverse `TAN`

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/sample.171.fits \
  --hdu 1 \
  --inverse-tan
```

This mode validates:

- `TAN plane -> helioprojective`
- round-trip error

4. Inverse non-slanted `AZP`

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/20250622_000831_s4h1A.fts \
  --inverse-azp
```

This mode validates:

- `AZP plane -> helioprojective`
- round-trip error

5. Inverse primary-branch `ZPN`

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/psp_L3_wispr_20231227T150704_V1_2222.fits \
  --inverse-zpn
```

This mode validates:

- `ZPN plane -> helioprojective`
- round-trip error on the primary monotonic branch

6. `HPC` render comparison

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/20241224_194245_d4c2A.fts \
  --hpc-render-compare \
  --render-size 2048
```

This mode uses the `HPC` display domain and runs the same
screen grid through:

- the JHV `HPC` sampling path
- Astropy `world -> pixel`

and writes:

- `*_hpc_jhv.png`
- `*_hpc_astropy.png`
- `*_hpc_diff.png`

under:

- [extra/test/out](extra/test/out)

It also reports:

- `bounds_deg`
- `pixel_center_max_error_px`
- `pixel_center_rms_error_px`

Interpretation:

- the script reports absolute pixel errors directly
- HI2 can show a slightly larger absolute `px` error because `AZP`
  magnification near the finite valid edge becomes extremely large

### Internal JHV comparison modes

1. `formal-TAN` vs JHV `HPC`

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/sample.171.fits \
  --hdu 1 \
  --ortho-vs-hpc-screen-compare \
  --render-size 4096
```

This mode compares, for TAN data:

- the `formal-TAN` path in `Orthographic` mode
- the JHV `HPC` display sampling path

over the on-disk orthographic domain, using a comparison grid at the native
image resolution of the tested file.

It writes:

- `*_ortho_screen.png`
- `*_hpc_screen.png`
- `*_ortho_vs_hpc_diff.png`

Interpretation:

- this is not an Astropy comparison
- it measures whether the `formal-TAN` path in `Orthographic` mode and `HPC`
  define the same on-screen geometry

2. `simple-TAN` vs JHV `HPC`

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/sample.171.fits \
  --hdu 1 \
  --compare-initial-tan-vs-hpc
```

This mode compares, for TAN data:

- `simple-TAN`
- the `HPC` display sampling path

over the on-disk orthographic domain, using a comparison grid at the native
image resolution of the tested file.

It writes:

- `*_initial_tan_screen.png`
- `*_hpc_screen_from_initial_tan_compare.png`
- `*_initial_tan_vs_hpc_diff.png`

It also reports:

- `initial_tan_vs_hpc_max_px`
- `initial_tan_vs_hpc_rms_px`

Interpretation:

- this is an internal JHV comparison for `TAN`
- it measures how closely `simple-TAN` in `Orthographic` mode matches the
  `HPC` display geometry

3. `simple-TAN` vs `formal-TAN`

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/sample.171.fits \
  --hdu 1 \
  --compare-initial-tan
```

This mode compares:

- `simple-TAN`
- `formal-TAN`
- Astropy as the WCS reference

over the on-disk orthographic domain, using a comparison grid at the native
image resolution of the tested file.

It writes:

- `*_initial_tan.png`
- `*_formal_tan.png`
- `*_initial_vs_formal_tan_diff.png`

Interpretation:

- this is an implementation-comparison mode for `TAN` only
- it quantifies how far `simple-TAN` and `formal-TAN` each are from Astropy,
  and how far `simple-TAN` is from `formal-TAN`

# Results

The primary discrepancy units reported in this note are angular sky errors
derived from `CDELT`, expressed in milliarcseconds or arcseconds as
appropriate. Pixel errors are kept as secondary units because they are the
quantities reported directly by the validator.

## FITS projections

1. `TAN` is correct.

- JHV `world -> helioprojective -> TAN plane -> pixel`
- `TAN plane -> helioprojective angles`
- matches Astropy to numerical precision

2. `formal-TAN` is substantially more accurate than `simple-TAN`.

- native-resolution comparison against Astropy on the TAN samples gives:
  - `sample.171.fits` (`1.009 AU`):
    - `simple-TAN`: `2.20 arcsec` max, `1.63 arcsec` RMS
    - `formal-TAN`: `9.40e-11 arcsec` max, `3.04e-11 arcsec` RMS
  - `solo_L2_eui-fsi174-image_20251002T150055171_V00.fits` (`0.448 AU`):
    - `simple-TAN`: `11.42 arcsec` max, `8.36 arcsec` RMS
    - `formal-TAN`: `8.99e-11 arcsec` max, `2.88e-11 arcsec` RMS
  - `20241224_194245_d4c2A.fts` (`0.967 AU`):
    - `simple-TAN`: `2.40 arcsec` max, `1.77 arcsec` RMS
    - `formal-TAN`: `1.02e-10 arcsec` max, `3.40e-11 arcsec` RMS
- in all three cases, `formal-TAN` matches Astropy to numerical precision,
  while `simple-TAN` shows a visible geometric error

3. `AZP` is correct for the current HI files.

- JHV `world -> helioprojective -> AZP plane -> pixel`
- `AZP plane -> helioprojective angles`
- matches Astropy to numerical precision
- current HI files are non-slanted: `PV2_2` is absent, so `gamma = 0`

4. Six-term `ZPN` is correct for the tested PSP/WISPR files.

- JHV `world -> helioprojective -> ZPN plane -> pixel`
- `ZPN plane -> helioprojective angles`
- currently implemented with `PV2_0..PV2_5`
- matches Astropy to numerical precision on:
  - `extra/test/data/psp_L3_wispr_20231227T150508_V1_1211.fits`
  - `extra/test/data/psp_L3_wispr_20231227T150704_V1_2222.fits`
- the current implementation keeps only the primary monotonic branch of the
  radial polynomial

## Astropy validation of JHV HPC rendering

For zenithal image data, the deterministic part of the mapping is:

1. image pixel or WCS-plane coordinate
2. inverse WCS (`TAN`, `AZP`, `ZPN`)
3. helioprojective angles
4. observer-frame `HPC` ray

That intermediate `HPC` ray field is fully determined by FITS/WCS and the
observer geometry.

For testing purposes, the validator models this `HPC` sampling map and compares
it directly against Astropy.

Measured `HPC` validation results:

- COR2 (`TAN`): `3.97e-4 mas` max (`7.50e-12 px`)
- HI1 (`AZP`): `3.59e-7 mas` max (`5.00e-12 px`)
- HI2 (`AZP`): `80.3 mas` max (`3.09e-4 px`) over the finite valid rendered
  domain
- PSP/WISPR 1211 (`ZPN`): `4.42e-5 mas` max (`2.90e-10 px`)
- PSP/WISPR 2222 (`ZPN`): `1.21e-5 mas` max (`5.93e-11 px`)

For HI2, the `80.3 mas` / `3.09e-4 px` maximum occurs near the extreme finite
valid `AZP` edge, where the projection magnification becomes very large. At
those edge samples, the projection maps the point to a source-pixel location
about 88 million pixels from the image reference point, so the absolute
difference corresponds to a relative disagreement of about `1e-12`.

## `Formal-TAN` versus JHV `HPC`

Measured comparison result:

- this is a different test from the Astropy validation above
- it compares the source pixels sampled by `formal-TAN` in `Orthographic`
  mode and by `HPC` at the same displayed on-disk screen radius
- native-resolution comparison between `formal-TAN` in `Orthographic` mode and
  `HPC` on the TAN samples gives:
  - `sample.171.fits` (`1.009 AU`):
    - `2.20 arcsec` max, `1.62 arcsec` RMS
    - theoretical max from Appendix A: `2.19 arcsec`
  - `solo_L2_eui-fsi174-image_20251002T150055171_V00.fits` (`0.448 AU`):
    - `11.2 arcsec` max, `8.28 arcsec` RMS
    - theoretical max from Appendix A: `11.05 arcsec`
  - `20241224_194245_d4c2A.fts` (`0.967 AU`):
    - `2.40 arcsec` max, `1.77 arcsec` RMS
    - theoretical max from Appendix A: `2.29 arcsec`

These results support the following conclusions:

- the `formal-TAN` path in `Orthographic` mode and `HPC` are not the same
  display geometry
- the small on-disk “bulging” when switching between them is expected from that
  geometry difference, not from an Astropy/WCS mismatch

## `Simple-TAN` versus JHV `HPC`

- it compares the source pixels sampled by `simple-TAN` in `Orthographic`
  mode and by `HPC` at the same displayed on-disk screen radius
- native-resolution comparison between `simple-TAN` and `HPC` on the TAN
  samples gives:
  - `sample.171.fits` (`1.009 AU`):
    - `6.73 mas` max, `3.09 mas` RMS
  - `solo_L2_eui-fsi174-image_20251002T150055171_V00.fits` (`0.448 AU`):
    - `370 mas` max, `116 mas` RMS
  - `20241224_194245_d4c2A.fts` (`0.967 AU`):
    - `9.32 mas` max, `3.65 mas` RMS
- this confirms that `simple-TAN` is much closer to the `HPC` display
  geometry than `formal-TAN`, which supports its use in `Orthographic` mode
  when visual consistency with `HPC` is preferred

# Appendix A: Theoretical Orthographic vs HPC discrepancy

This appendix records a purely theoretical geometric discrepancy between:

- a straight-on orthographic projection of the visible solar surface
- a linear `HPC` display of the same imaged sphere

with both images scaled so that the apparent solar limb radius is the same.

This result is not derived from JHV. It follows directly from the ideal
orthographic and `HPC` projection formulas for a unit solar
sphere observed from distance $D$.

Assumptions:

- solar radius `R = 1`
- observer distance `D` expressed in solar radii
- `HPC` is displayed linearly in helioprojective angle
- the comparison is purely geometric and does not depend on JHV-specific
  rendering choices

Let $\gamma$ be the heliocentric angle on the solar surface, measured from disk
center toward the limb.

The visible limb occurs at:

- $\gamma_{\mathrm{limb}} = \cos^{-1}(1 / D)$

The apparent helioprojective limb angle is:

- $\theta_{\mathrm{limb}} = \sin^{-1}(1 / D)$

The orthographic and `HPC` radial coordinates, after equal-limb normalization,
are:

- $R_{\mathrm{ortho}}(\gamma) = \sin(\gamma) / \sqrt{1 - D^{-2}}$
- $R_{\mathrm{hpc}}(\gamma) = \tan^{-1}(\sin(\gamma) / (D - \cos(\gamma))) / \sin^{-1}(1 / D)$

The normalized discrepancy is therefore:

- $\Delta(\gamma) = R_{\mathrm{hpc}}(\gamma) - R_{\mathrm{ortho}}(\gamma)$

At 1 AU, with $D \approx 215.03215567$, the maximum absolute discrepancy is:

- $\max |\Delta| \approx 0.00232016$
- this occurs at $\gamma \approx 44.84^\circ$
- in angular units, this is about `2.23 arcsec`
- equivalently, about `0.232%` of the apparent solar limb radius

The discrepancy is positive, so the `HPC` image bulges outward slightly relative
to the orthographic image.

The discrepancy is zero at disk center and at the limb, and peaks on a ring at
about $44.8^\circ$ heliocentric angle, corresponding to about $70.7\%$ of the
apparent disk radius.

The following figure shows the same discrepancy at `1 AU` as a radial gradient
over the normalized solar disk. The dashed ring marks the location of the
maximum discrepancy.

![Orthographic vs HPC discrepancy across the projected solar disk at 1 AU](ortho_vs_hpc_discrepancy_gradient_1au.pdf){ width=72% }

\newpage

The following figure shows the maximum discrepancy as a function of observer
distance between `0.2 AU` and `1.1 AU`.

![Maximum Orthographic vs HPC discrepancy versus observer distance](ortho_vs_hpc_discrepancy_vs_distance.pdf){ width=85% }
