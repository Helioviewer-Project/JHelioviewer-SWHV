---
title: |
   | SWHV CCN4
   |
   | Zenithal WCS Validation and Display Notes for JHV
subtitle: SWHV-ROB-TN-001-CCN4 v0
subject: SWHV CCN4 
date: SWHV-ROB-TN-001-CCN4 - Version 0 - 2026-03-xx
lof: false
lot: false
---

`id: \exec{git hash-object \file}`

# Status

This note reflects the current state of the work after:

- validating the forward `TAN`, `AZP`, and six-term `ZPN` WCS math against Astropy
- validating the `TAN` inverse mapping against Astropy
- validating the non-slanted `AZP` inverse mapping against Astropy
- validating the primary-branch six-term `ZPN` inverse mapping against Astropy
- correcting the local embedding prototypes to use the actual JHV observer
  frame
- trying and rejecting multiple 3D embedding experiments in `solarOrtho.frag`

Current conclusion:

- the WCS math is correct
- a bounded native JHV `HPC` mode was implemented and its render mapping is
  validated against Astropy for the current test files
- direct `Orthographic`-vs-`HPC` screen comparison shows they are not identical
  even with the same observer viewpoint and `dragRotation = 0`
- the remaining HI rendering problem is not a WCS problem
- it is a 3D embedding problem
- FITS/WCS does not uniquely determine that embedding


# Primary design criterion

The main criterion for orthographic embedding should be:

- when the image is viewed from its own observer viewpoint
- and `dragRotation = 0`
- the rendering should look as close as possible to the image's native
  helioprojective appearance

This criterion applies to both `TAN` and `AZP`.

For `TAN`, the current interpretation is consistent with that criterion:

- `TAN` is the natural flat-plane helioprojective image model
- in the native observer view, it is the right baseline for preserving the
  original image appearance

For wide-field `AZP`/later `ZPN`, this criterion is the main test for whether a
viewer embedding convention is acceptable.

There is also a qualitative constraint for HI in ortho:

- HI1 and HI2 should appear barrel-distorted rather than pincushion-distorted

This does not determine the embedding by itself, but it is an additional sign
check on whether a candidate viewer convention is visually plausible.


# What is already proven

These parts are already on solid ground.

1. Forward `TAN` is correct.

- JHV `world -> helioprojective -> TAN plane -> pixel`
- matches Astropy to numerical precision

2. Inverse `TAN` direction recovery is correct.

- `TAN plane -> helioprojective angles`
- implemented and validated in
  [extra/test/validate_jhv_wcs_against_astropy.py](extra/test/validate_jhv_wcs_against_astropy.py)
- matches Astropy to numerical precision

3. Forward `AZP` is correct for the current HI files.

- JHV `world -> helioprojective -> AZP plane -> pixel`
- matches Astropy to numerical precision
- current HI files are non-slanted: `PV2_2` is absent, so `gamma = 0`

4. Inverse `AZP` direction recovery is correct for the current HI files.

- `AZP plane -> helioprojective angles`
- implemented and validated in
  [extra/test/validate_jhv_wcs_against_astropy.py](extra/test/validate_jhv_wcs_against_astropy.py)
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

The unresolved part is therefore not the projection formulas. It is what 3D surface
JHV should render for HI/AZP/ZPN in orthographic mode.


# What FITS/WCS actually gives

For zenithal projections, FITS/WCS gives:

- a mapping between angular coordinates and image-plane coordinates
- `CRVAL` center direction
- `CROTA` or equivalent `PC`
- projection parameters such as `PV2_1 = mu` and `PV2_2 = gamma`

It does not give:

- a unique physical distance for a rendered image sheet in the Sun-centered 3D scene
- a unique rule for where along a recovered observer ray a rendered point should sit

That missing degree of freedom is the core reason the remaining problem exists.


# HPC intermediate representation

The cleanest way to factor this work is to introduce an explicit intermediate
`HPC` representation between WCS and any final JHV display mode.

For zenithal image data, the deterministic part is:

1. image pixel / WCS plane coordinate
2. inverse WCS (`TAN`, `AZP`, later `ZPN`)
3. helioprojective angles
4. observer-frame `HPC` ray

This intermediate `HPC` ray field is fully determined by FITS/WCS and observer
geometry. No viewer convention enters yet.

That gives a cleaner separation:

- zenithal WCS chooses how to recover the `HPC` ray
- a native JHV `HPC` mode can render that result directly on an `HPC` image plane
- orthographic mode can then apply a separate viewer embedding to the same `HPC`
  ray field

The unresolved question is therefore no longer “how should `AZP` be embedded?” but:

- “given an `HPC` ray field, how should ortho place it in 3D?”

That is a much better long-term split because a future pure JHV `HPC`
projection should then be deterministic, while ortho remains a viewer
convention layered on top.

Current prototype result:

- the deterministic `HPC` image plane is easy to define and validate
- it preserves native observer-view angular appearance by construction
- for HI1 it remains well-behaved over the full image
- for HI2 it becomes extremely extended near the horizon, because the pure `HPC`
  plane itself has a native singular blow-up there

`HPC` is therefore a good intermediate representation and is also the basis of a
native bounded JHV display mode, but it does not by itself solve the ortho
embedding problem for very wide fields.

Current native `HPC` status:

- a bounded `HPC` display mode with symmetric extent on both axes was implemented in JHV
- the display scale is derived from the enabled image footprints
- the visible screen domain stays centered on `(Tx, Ty) = (0, 0)`, so the Sun
  center remains at the grid center
- aspect-ratio padding is applied around `(0, 0)` to preserve isotropic angular
  scale on screen
- during playback, the fitted `HPC` scale is sticky for the enabled layer set
  instead of being recomputed from every frame
- the `HPC` render sampling map has been validated directly against Astropy on
  the current test files

Current direct `HPC` validation results:

- COR2 (`TAN`): pixel-center error at machine precision
- HI1 (`AZP`): pixel-center error at machine precision
- HI2 (`AZP`): pixel-center error still extremely small (`~3e-5 px` max) over
  the finite valid rendered domain
- PSP/WISPR 1211 (`ZPN`): pixel-center error at machine precision
- PSP/WISPR 2222 (`ZPN`): pixel-center error at machine precision over the
  finite primary-branch rendered domain

Current direct `Orthographic`-vs-`HPC` screen comparison result:

- this is a different test from the Astropy validation above
- it compares what source pixel `Orthographic` and `HPC` choose at the same
  displayed on-disk screen radius
- for the retained AIA 171 sample, the mismatch is real and visible:
  - `pixel_center_max_error_px ~ 3.67`
  - `pixel_center_rms_error_px ~ 2.71`

The current evidence supports the following conclusions:

- both modes are WCS-correct in their own sampling logic
- they are not the same display geometry
- the small on-disk “bulging” when switching between them is expected from that
  geometry difference, not from an Astropy/WCS mismatch


# What Thompson 2006 clarifies

Thompson (2006) helps, but only on the WCS side of the boundary.

It supports:

- treating solar image data in helioprojective coordinates
- using standard FITS/WCS zenithal projections such as `TAN` and `AZP` for the
  2D angular-to-plane mapping
- the general solar-image coordinate formalism already used in the current
  validator and shader math

It does not provide:

- a unique 3D placement rule for a rendered image sheet in JHV's Sun-centered
  orthographic scene
- a unique line-of-sight distance for off-limb image samples

Thompson therefore reinforces the current state rather than changing it:

- forward/inverse WCS is constrained and testable
- orthographic 3D embedding remains a viewer convention


# Current HI header facts

For the current STEREO-A HI test files:

- `CRVAL1/2 == XCEN/YCEN`
- `DSUN_OBS` is present and already used as observer-Sun distance
- `PV2_1` is present
- `PV2_2` is absent, so `gamma = 0`

What is not present:

- no detector-plane distance
- no explicit embedding distance
- no metadata field that answers “where should the image surface be placed in 3D?”

Current JHV metadata agrees with this:

- [src/org/helioviewer/jhv/metadata/HelioviewerMetaData.java](src/org/helioviewer/jhv/metadata/HelioviewerMetaData.java) reads `DSUN_OBS`, `CRVAL*`, `CROTA`/`PC`, and `PV2_*`
- it does not define an extra image-surface placement scalar


# Projection formulas in current JHV conventions

In the current GLSL/validator conventions:

- `phi, theta` are helioprojective longitude/latitude
- `phi0, theta0` come from `CRVAL`
- `mu = PV2_1`
- `gamma = PV2_2` in degrees in FITS, converted to radians for trig
- current `ZPN` support uses `PV2_0..PV2_5`

Forward `TAN` uses:

- `a = cos(theta) * sin(phi - phi0)`
- `b = cos(theta0) * sin(theta) - sin(theta0) * cos(theta) * cos(phi - phi0)`
- `cos(eta) = sin(theta0) * sin(theta) + cos(theta0) * cos(theta) * cos(phi - phi0)`
- `R = tan(eta)`
- `x = R * sin(alpha) = a / cos(eta)`
- `y = R * cos(alpha) = b / cos(eta)`

The inverse `TAN` radial law therefore uses:

- `eta = atan(R)`

followed by inversion of the same native spherical rotation about `CRVAL`.

Forward `AZP` uses:

- `a = cos(theta) * sin(phi - phi0)`
- `b = cos(theta0) * sin(theta) - sin(theta0) * cos(theta) * cos(phi - phi0)`
- `cos(eta) = sin(theta0) * sin(theta) + cos(theta0) * cos(theta) * cos(phi - phi0)`

For `gamma = 0`:

- `R = (mu + 1) * sin(eta) / (mu + cos(eta))`
- `x = R * sin(alpha)`
- `y = R * cos(alpha)`

The inverse non-slanted `AZP` radial law is therefore solved first, then the
native spherical rotation about `CRVAL` is inverted.

Current `ZPN` uses:

- `eta = acos(sin(theta0) * sin(theta) + cos(theta0) * cos(theta) * cos(phi - phi0))`
- `R(eta) = sum(PV2_m * eta^m)` for `m = 0..5`
- `eta` is in radians
- `R` is therefore also in radians before the usual `planeUnitsPerRad` scaling

For inversion, the current JHV/validator implementation:

- keeps the primary forward branch only
- defines that branch by `dR/deta > 0`
- inverts `R(eta)` by bisection on that branch


# Projection-agnostic decomposition

A useful formulation of zenithal embedding is to separate two problems.

Part 1: recover an observer-frame ray direction

- start from a point in native WCS plane coordinates
- invert the radial law:
  - `TAN`: `eta = atan(R)`
  - `AZP`: `eta = inverseAzpRadius(...)`
  - `ZPN`: `eta = inverseZpnRadius(...)`
- invert the native spherical rotation around `CRVAL`
- convert the resulting helioprojective angles to an observer-frame ray direction `d`

In JHV's current helioprojective convention, this observer frame is:

- Sun center at `(0, 0, 0)`
- image observer at `(0, 0, D)`
- center line of sight toward `-z`

The recovered direction is therefore an observer-to-scene direction, not an
origin-centered direction.

Part 2: choose a viewer placement rule

- choose where along that ray the rendered point sits
- mathematically: choose `lambda(d)` and place
  `p_scene = p_observer + lambda(d) * d`

Part 1 is constrained by WCS.
Part 2 is not.


# What failed and why

1. Sun-centered plane

Historical ortho behavior:

- intersect the screen ray with a plane through Sun center
- rotate into image frame
- feed the result through correct WCS

Why it is acceptable for narrow `TAN`:

- simple
- preserves `CRVAL` and `CROTA`
- narrow fields hide the arbitrariness

Why it is weak for HI:

- not implied by `AZP`
- side-on appearance is dominated by arbitrary plane placement

2. Direct projection-plane sampling

Rejected idea:

- use `rotatedHitPoint.xy` directly as WCS plane coordinates

Why it failed:

- discarded the `CRVAL` offset in geometry
- recentered HI on the Sun

3. Closest-approach sphere

Rejected idea:

- recover a ray and place the point on a closest-approach sphere to the Sun

Why it failed:

- introduced a Sun-centric physical rule not present in WCS
- created a cusp/pinch and visible singular wedge

All failed attempts share the same pattern:

- WCS was right
- the chosen 3D distance rule was unjustified

4. Constant observer plane

Prototyped in the local validator with:

- plane normal = center ray
- plane distance `L = k * observerDistance`

Result for HI2:

- changing `k` does not remove the excluded image domain
- the same subset of pixels remains outside the forward-plane hemisphere

The constant-plane family is therefore not a viable full-image embedding for HI2.


# Embedding constraints

Any acceptable HI/AZP/ZPN orthographic embedding must satisfy:

1. `CRVAL` shifts the image center in 3D.
2. `CROTA` rotates around the image center, not around the Sun.
3. The visible domain stays a single non-self-intersecting sheet.
4. The validated WCS path remains authoritative for sampling.
5. The structure generalizes to `ZPN`.
6. HI wide-field appearance should remain barrel-like rather than
   pincushion-like.


# Admissible model families

Only a few model families are defensible.

1. Observer image plane

- recover an observer-frame ray direction from inverse WCS
- intersect that ray with a plane orthogonal to the image center ray
- plane offset is a chosen viewer parameter `L`

Pros:

- explicit
- preserves image center and orientation naturally
- generalizes to `ZPN`

Cons:

- `L` is not fixed by FITS/WCS
- side-on appearance depends on `L`
- for HI2, the constant-plane version has now failed structurally as a
  full-domain model

2. Observer angular shell

- recover only directions
- place all points on a shell of chosen radius `S` around the observer:
  `p_scene = p_observer + S * d`

Pros:

- pure angular visualization
- projection-agnostic after inverse WCS

Cons:

- no detector-plane interpretation
- less natural if the intended UI metaphor is an image plane

3. Explicit visualization surface

- any other chosen surface whose contract is purely visual

Pros:

- flexible

Cons:

- must be documented as a JHV viewer convention, not as WCS


# Most plausible next implementation direction

The preferred next candidate is the observer shell model, not the observer plane.

Reason:

- the constant-plane family has already been falsified for HI2 as a full-image
  model
- the shell family remains projection-agnostic after inverse WCS
- it has no forward-plane cutoff
- it still separates inverse WCS from the viewer placement rule

It is also more compatible with the current primary design criterion for HI:

- native observer view should stay as close as possible to the original angular
  appearance
- side-view behavior may then be treated as a secondary viewer convention

But this still has one unavoidable free parameter:

- shell radius `S`

Current prototype result:

- for HI2, the shell family is the only currently tested full-domain viable
  family
- it preserves native observer-view angular appearance
- its sampled edge-curvature diagnostic bows outward rather than inward
- these results hold after correcting the prototype to use the actual JHV
  observer frame `(0, 0, D)` with line of sight toward `-z`

The shell family is therefore the practical baseline for ortho HI rendering work.

At this point there is no evidence that `S` can be derived from current FITS/WCS
for HI. It must come from:

- external instrument geometry
- an existing JHV convention
- or a new explicit viewer convention

The first explicit candidate convention should be:

- `S = observerDistance`

This should be treated as a JHV viewer convention, not as a FITS/WCS-derived
physical truth.


# Exact visibility criterion for an observer-centered shell

For the shell model, after inverse WCS the embedded point is

- `p_scene = p_observer + S * d`

with:

- `p_observer` the image observer position in scene coordinates
- `S > 0` the chosen shell radius
- `d` the unit observer-ray direction recovered from inverse WCS

Now consider the current rendering camera in orthographic mode.
Let:

- `p_eye` be the shell point transformed into current eye space
- `o_eye` be the shell center (`p_observer`) transformed into current eye space
- `n_eye = normalize(p_eye - o_eye)` be the shell normal in eye space

OpenGL eye space looks along `-z`, so the current-camera viewing direction into
the scene is:

- `u_eye = (0, 0, -1)`

For a convex sphere, the exact visible hemisphere is the front-facing one:

- `dot(n_eye, u_eye) < 0`

Equivalently, because `n_eye` is proportional to `p_eye - o_eye`:

- `p_eye.z > o_eye.z`

The silhouette is the equality case:

- `p_eye.z = o_eye.z`

Consequences:

- the back hemisphere `p_eye.z < o_eye.z` must not be rendered
- any triangle that crosses `p_eye.z = o_eye.z` must be clipped or split at the
  silhouette
- simple mesh rendering without this test produces the kind of overlapping
  hourglass geometry seen in the failed HI2 shell experiment

So for a shell-based ortho renderer, current-camera visibility is not a
heuristic. It is exactly the front hemisphere of the observer-centered sphere in
current eye space.


# Architectural consequence

The current fragment-only full-screen-quad ortho path is a poor fit for this.

Why:

- the fragment shader currently starts from a guessed scene-space point and
  projects it into the image
- the projection-agnostic zenithal model wants the opposite flow:
  image-plane coordinate -> inverse WCS -> observer ray -> scene point

That strongly suggests a mesh/tessellated image-sheet path is the coherent long-term
solution for HI/AZP/ZPN:

- generate a sheet in observer-plane coordinates
- convert vertices with inverse WCS
- let the fragment shader do sampling and clipping only


# How to use the validator

The main script is:

- [extra/test/validate_jhv_wcs_against_astropy.py](extra/test/validate_jhv_wcs_against_astropy.py)

Run it with:

```bash
python3 extra/test/validate_jhv_wcs_against_astropy.py <fits-file> [mode]
```

Validator modes:

1. Forward WCS random-sample validation

```bash
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/20241224_194245_d4c2A.fts
```

This reports:

- `projection_max_error_internal`
- `pixel_center_max_error_px`

2. Full pixel-center validation

```bash
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/20250622_000831_s4h1A.fts \
  --all-pixels
```

This checks the full image grid against Astropy and reports the worst pixel
center error.

3. Inverse `TAN`

```bash
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/sample.171.fits \
  --hdu 1 \
  --inverse-tan
```

This validates:

- `TAN plane -> helioprojective`
- round-trip error

4. Inverse non-slanted `AZP`

```bash
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/20250622_000831_s4h1A.fts \
  --inverse-azp
```

This validates:

- `AZP plane -> helioprojective`
- round-trip error

5. Inverse primary-branch `ZPN`

```bash
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/psp_L3_wispr_20231227T150704_V1_2222.fits \
  --inverse-zpn
```

This validates:

- `ZPN plane -> helioprojective`
- round-trip error on the primary monotonic branch

6. `HPC` render comparison

```bash
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/20241224_194245_d4c2A.fts \
  --hpc-render-compare \
  --render-size 512
```

This uses the bounded native `HPC` display domain, renders the same screen grid
through:

- the JHV `HPC` sampling path
- Astropy `world -> pixel`

and writes:

- `*_hpc_jhv.png`
- `*_hpc_astropy.png`
- `*_hpc_diff.png`

under:

- [extra/test/out](extra/test/out)

The script also reports:

- `bounds_deg`
- `pixel_center_max_error_px`
- `pixel_center_rms_error_px`

Interpretation:

- near-black diff image means the JHV `HPC` render mapping matches Astropy
- bright areas indicate a mapping mismatch or a domain/singularity issue

Interpretation of the reported max error:

- the reported `pixel_center_max_error_px` is an absolute source-pixel difference
- for most current `TAN`, `AZP`, and `ZPN` test files, that absolute error stays
  near machine precision
- for STEREO-A HI2 (`AZP`), the reported max can be noticeably larger in
  absolute pixels (around `3e-5 px`) while still representing essentially the
  same numerical agreement

Why HI2 is different:

- HI2 reaches much closer to the finite valid edge of the `AZP` domain
- near that edge, the `AZP` radial magnification becomes extremely large
- the validator therefore compares JHV and Astropy at source-pixel coordinates
  of order `1e7 px`
- tiny floating-point differences in the angular/plane conversion then appear as
  larger absolute pixel-coordinate differences

For HI2, a max error around `3e-5 px` still corresponds to a relative error
of order `1e-12`, which is consistent with the machine-precision-level agreement
seen in the other tests.

7. Direct `Orthographic` vs `HPC` screen comparison

```bash
python3 extra/test/validate_jhv_wcs_against_astropy.py \
  extra/test/data/sample.171.fits \
  --ortho-vs-hpc-screen-compare \
  --render-size 512
```

This compares:

- `Orthographic`: screen point -> sphere point -> helioprojective -> source pixel
- `HPC`: same screen point -> linear helioprojective angle -> source pixel

and writes:

- `*_ortho_screen.png`
- `*_hpc_screen.png`
- `*_ortho_vs_hpc_diff.png`

Interpretation:

- this is not an Astropy comparison
- it measures whether `Orthographic` and `HPC` are the same on-screen geometry
- for the current AIA 171 sample they are not

8. Embedding prototypes

There are also experimental geometry modes:

- `--observer-hpc-prototype`
- `--observer-plane-prototype`
- `--observer-shell-prototype`

These are for reasoning about ortho embedding, not for validating the production
JHV render path.

## What the validator models from current JHV

For the production validation modes above, the script models the stable
Java/GLSL reprojection and sampling path, not the full interactive renderer.

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
- the bounded `HPC` display domain used for validation
- direct `Orthographic` vs `HPC` on-screen pixel-choice comparison for the
  specific no-rotation comparison mode

Excluded:

- `dragRotation`
- `cameraDiff`
- differential diff-image alignment between two arbitrary live layers beyond the
  specific per-image `deltaT` reprojection modeled in the shader path
- `deltaCROTA`, `deltaCRVAL1`, `deltaCRVAL2`
- annotation picking/drawing
- playback/view policy effects such as dynamic refitting of the visible `HPC`
  box
- ortho off-limb embedding experiments except in the explicit prototype modes
- full OpenGL rasterization state, blending, and UI behavior

The validator therefore answers:

- whether the current reprojection/sampling math matches Astropy
- whether the bounded `HPC` sampling map matches Astropy
- whether `Orthographic` and `HPC` choose the same source pixels at the same
  displayed on-disk screen radius

It does not answer:

- whether transient user-driven viewing state produces the desired playback
  behavior
- whether annotation tools behave correctly
- whether an ortho embedding convention is visually acceptable

## What is compared against Astropy

The validator uses Astropy as the external reference for these checks:

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

Not compared against Astropy:

- direct `Orthographic` vs `HPC` screen comparison
  - this is an internal JHV-to-JHV comparison only
- observer plane/shell/HPC embedding prototypes
  - these are geometry experiments, not Astropy validations


# What Astropy can test

Astropy can validate all parts that are fully defined by WCS.

Specifically, Astropy can validate:

1. Forward projection

- helioprojective angles -> projected plane coordinates

2. Inverse projection

- projected plane coordinates -> helioprojective angles

3. Round-trip behavior

- world -> plane -> world
- plane -> world -> plane

4. Valid-domain / branch behavior

- singularities
- asymptotic limits
- fold-back rejection where applicable

5. Full pixel-center mapping

- WCS plane plus linear terms (`CRVAL`, `PC`/`CROTA`, `CDELT`, `CRPIX`)

That is exactly what the current validator already does for forward
`TAN`/`AZP`/six-term `ZPN`, inverse `TAN`, inverse non-slanted `AZP`, and
inverse primary-branch six-term `ZPN`.


# What Astropy cannot test

Astropy cannot test the unresolved orthographic HI problem directly.

Astropy does not define:

- where the image sheet should sit in the Sun-centered 3D scene
- what `lambda(d)` should be along an observer ray
- which JHV visualization convention should be used for side-on viewing

This remains true even when Thompson 2006 is taken into account: the paper
constrains the solar-image coordinate formalism, not the 3D viewer placement
rule.

The conclusion is:

- WCS part: yes, fully testable against Astropy
- 3D embedding part: no, not from Astropy alone


# Practical testing strategy

The right testing split is:

1. Keep validating all WCS pieces against Astropy.
2. For embedding, use derived invariants instead of Astropy:
   - image center follows `CRVAL`
   - rotation follows `CROTA`
   - sheet stays single-valued
   - no self-intersection within valid image domain
   - side-on extent changes only with the chosen viewer parameter (`L` or `S`)

If a new embedding model is implemented, it should first be tested in the local
validator as a pure geometry model before going back into GLSL.


# Bottom line

The unresolved problem is not “how to implement `AZP`”.

It is:

- choose and document a JHV 3D viewer convention for zenithal wide-field images
- then implement that convention in a geometry path that starts from inverse WCS,
  not from a guessed scene-space hit point
