---
title: |
   | SWHV CCN4
   | Zenithal WCS Validation Test Notes
subtitle: SWHV-ROB-TN-001-CCN4 v0
subject: SWHV CCN4 
date: SWHV-ROB-TN-001-CCN4 - Version 0 - 2026-03-xx
lof: true
lot: false
---

`id: \exec{git hash-object \file}`

# Introduction

This note describes the validation work carried out for JHV's zenithal WCS and
`HPC` sampling path.

A dedicated validator was built in:

- [extra/test/validate_jhv_wcs_against_astropy.py](extra/test/validate_jhv_wcs_against_astropy.py)

It reproduces the parts of the JHV WCS/HPC mapping path that are covered by
this validation, outside the renderer, and compares them directly against
Astropy wherever the result is fully defined by WCS.

Within that scope, `TAN` was reworked from the earlier simplified treatment,
and `TAN`, `AZP`, and six-term `ZPN` were implemented according to the
theoretical foundation of Greisen and Calabretta, "Representations of celestial
coordinates in FITS" ([A&A 395, 1077-1122,
2002](https://www.aanda.org/articles/aa/abs/2002/45/aah3859/aah3859.html)).

A native JHV `HPC` projection was also implemented and validated as part of the
same work; its solar-coordinate interpretation follows Thompson, "Coordinate
systems for solar image data"
([A&A 449, 791-803,
2006](https://www.aanda.org/articles/aa/abs/2006/14/aa4262-05/aa4262-05.html)).

The validator was applied to a variety of real instrument FITS files and
metadata, including COR2, HI1, HI2, SDO/AIA, Solar Orbiter/EUI, and PSP/WISPR
examples.

The work reported here includes:

- validating the forward `TAN`, `AZP`, and six-term `ZPN` WCS math against Astropy
- validating the inverse `TAN`, non-slanted `AZP`, and primary-branch six-term `ZPN` mappings against Astropy
- validating the bounded native `HPC` sampling path against Astropy

Main conclusions:

- forward and inverse `TAN`, `AZP`, and primary-branch six-term `ZPN` are
  validated against Astropy within the documented scope
- bounded native JHV `HPC` rendering is validated against Astropy within the
  documented scope
- direct `Orthographic`-vs-`HPC` screen comparison shows that the two display
  modes are not identical even with the same observer viewpoint and
  `dragRotation = 0`
- the measured `Orthographic`-vs-`HPC` discrepancies are consistent with the
  theoretical geometric discrepancy derived in Appendix A
- this note is therefore a validation/testing note, not a design note for
  ortho embedding, playback policy, or annotation behavior

# The validator

## JHV behavior modeled by the validator

For the validation modes in this note, the script models the Java/GLSL
reprojection and sampling path, not the full interactive renderer.

Included:

- FITS metadata parsing into the same effective quantities JHV uses:
  - `CRPIX`
  - `CRVAL`
  - `CDELT`
  - `PC`/`CROTA`
  - `DSUN_OBS`
  - `PV2_0..PV2_5`
- the current shared WCS projection math for:
  - `TAN`
  - non-slanted `AZP`
  - six-term primary-branch `ZPN`
- the current `HPC` image sampling path:
  - screen `HPC` coordinate -> helioprojective -> WCS plane -> source pixel
- the bounded `HPC` display domain used by the current validation path
- direct `Orthographic` vs `HPC` sampling comparison for the specific
  no-rotation comparison mode

Excluded:

- `dragRotation`
- `cameraDiff`
- differential diff-image alignment between two arbitrary live layers beyond the
  specific per-image `deltaT` reprojection modeled in the shader path
- `deltaCROTA`, `deltaCRVAL1`, `deltaCRVAL2`
- annotation picking/drawing
- playback/view policy effects such as dynamic refitting of the visible `HPC`
  box
- full OpenGL rasterization state, blending, and UI behavior

The validator therefore establishes:

- whether the current reprojection/sampling math matches Astropy
- whether the bounded `HPC` sampling map matches Astropy
- whether `Orthographic` and `HPC` choose the same source pixels at the same
  displayed on-disk screen radius

It does not address:

- whether transient user-driven viewing state produces the desired playback
  behavior
- whether annotation tools behave correctly

## Astropy-based validation scope

Astropy can validate all parts of the mapping that are fully defined by WCS.

In this validator, Astropy is used as the external reference for:

- forward sample validation
  - JHV-style world/helioprojective -> WCS plane -> pixel
  - compared against `astropy.wcs.WCS.wcs_world2pix(...)`
- all-pixels validation
  - every source-image pixel center is mapped through the JHV path and compared
    against Astropy pixel-center results
- inverse `TAN`
  - JHV plane -> helioprojective inversion is checked by round-trip comparison
    against Astropy's forward WCS
- inverse `AZP`
  - JHV plane -> helioprojective inversion is checked by round-trip comparison
    against Astropy's forward WCS
- inverse primary-branch `ZPN`
  - same round-trip strategy on the supported primary monotonic branch
- bounded `HPC` render comparison
  - the same `HPC` screen grid is run through:
    - the JHV sampling path
    - Astropy `world -> pixel`
  - the script compares the resulting source pixel centers and also writes diff
    images

The following check is not compared against Astropy:

- direct `Orthographic` vs `HPC` screen comparison
  - this is an internal JHV-to-JHV comparison only

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
  --render-size 512
```

This mode uses the bounded native `HPC` display domain and renders the same
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

### Internal JHV comparison mode

1. Direct `Orthographic` vs `HPC` screen comparison

```text
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/sample.171.fits \
  --ortho-vs-hpc-screen-compare \
  --render-size 2048
```

This mode compares:

- `Orthographic`: screen point -> sphere point -> helioprojective -> source pixel
- `HPC`: same screen point -> linear helioprojective angle -> source pixel

and writes:

- `*_ortho_screen.png`
- `*_hpc_screen.png`
- `*_ortho_vs_hpc_diff.png`

Interpretation:

- this is not an Astropy comparison
- it measures whether `Orthographic` and `HPC` are the same on-screen geometry

# Results

The primary discrepancy unit reported in this note is the angular sky error
derived from `CDELT` and expressed in milliarcseconds. Pixel errors are kept as
secondary units because they are the quantities reported directly by the
validator.

1. Forward `TAN` is correct.

- JHV `world -> helioprojective -> TAN plane -> pixel`
- matches Astropy to numerical precision

2. Inverse `TAN` direction recovery is correct.

- `TAN plane -> helioprojective angles`
- matches Astropy to numerical precision

3. Forward `AZP` is correct for the current HI files.

- JHV `world -> helioprojective -> AZP plane -> pixel`
- matches Astropy to numerical precision
- current HI files are non-slanted: `PV2_2` is absent, so `gamma = 0`

4. Inverse `AZP` direction recovery is correct for the current HI files.

- `AZP plane -> helioprojective angles`
- matches Astropy to numerical precision

5. Forward six-term `ZPN` is correct for the current PSP/WISPR files.

- JHV `world -> helioprojective -> ZPN plane -> pixel`
- currently implemented with `PV2_0..PV2_5`
- matches Astropy to numerical precision on:
  - `extra/test/data/psp_L3_wispr_20231227T150508_V1_1211.fits`
  - `extra/test/data/psp_L3_wispr_20231227T150704_V1_2222.fits`

6. Inverse primary-branch six-term `ZPN` direction recovery is correct for the
   current PSP/WISPR files.

- `ZPN plane -> helioprojective angles`
- validated against Astropy on the same PSP/WISPR files
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

For testing purposes, the validator models the bounded native `HPC` sampling
map and compares it directly against Astropy.

Direct `HPC` validation results:

- COR2 (`TAN`): `1.05e-7 mas` max (`7.16e-12 px`)
- HI1 (`AZP`): `3.55e-7 mas` max (`4.95e-12 px`)
- HI2 (`AZP`): `8.38 mas` max (`3.23e-5 px`) over the finite valid rendered
  domain
- PSP/WISPR 1211 (`ZPN`): `6.65e-6 mas` max (`4.37e-11 px`)
- PSP/WISPR 2222 (`ZPN`): `1.94e-6 mas` max (`9.55e-12 px`)

For HI2, the `8.38 mas` / `3.23e-5 px` maximum occurs near the extreme finite
valid `AZP` edge, where the projection magnification becomes very large. At
those edge samples, the projection maps the point to a source-pixel location
about 15 million pixels from the image reference point, so the absolute
difference corresponds to a relative disagreement of about `1e-12`.

## Comparison of JHV `Orthographic` and `HPC`

Direct `Orthographic`-vs-`HPC` screen comparison result:

- this is a different test from the Astropy validation above
- it compares what source pixel `Orthographic` and `HPC` choose at the same
  displayed on-disk screen radius
- for the current AIA 171 sample, the mismatch is real and visible:
  - max: `2.20 arcsec` (`3.67 px`)
  - RMS: `1.63 arcsec` (`2.71 px`)
- for the current Solo/EUI 174 sample, the mismatch is also real:
  - max: `11.2 arcsec` (`2.53 px`)
  - RMS: `8.28 arcsec` (`1.87 px`)

These measured maxima are consistent with the theoretical geometric discrepancy
derived in Appendix A:

- `sample.171.fits`, at about `1.009 AU`:
  - measured max: `2.20 arcsec`
  - theoretical max: `2.19 arcsec`
- `solo_L2_eui-fsi174-image_20251002T150055171_V00.fits`, at about `0.448 AU`:
  - measured max: `11.2 arcsec`
  - theoretical max: `11.05 arcsec`

These results support the following conclusions:

- both modes are WCS-correct in their own sampling logic
- they are not the same display geometry
- the small on-disk “bulging” when switching between them is expected from that
  geometry difference, not from an Astropy/WCS mismatch

# Appendix A: Theoretical Orthographic vs HPC discrepancy

This appendix records a purely theoretical geometric discrepancy between:

- a straight-on orthographic projection of the visible solar surface
- a linear `HPC` display of the same imaged sphere

with both images scaled so that the apparent solar limb radius is the same.

This result is not derived from JHV. It follows directly from the ideal
orthographic and helioprojective (`HPC`) projection formulas for a unit solar
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
