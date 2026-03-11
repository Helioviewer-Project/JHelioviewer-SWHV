Zenithal Embedding Notes for JHV
================================

Status
------

This note reflects the current state of the work after:

- validating the forward `TAN` and `AZP` WCS math against Astropy
- validating the non-slanted `AZP` inverse mapping against Astropy
- trying and rejecting multiple 3D embedding experiments in `solarOrtho.frag`

Current conclusion:

- the WCS math is correct
- the remaining HI rendering problem is not a WCS problem
- it is a 3D embedding problem
- FITS/WCS does not uniquely determine that embedding


Primary design criterion
------------------------

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


What is already proven
----------------------

These parts are already on solid ground.

1. Forward `TAN` is correct.

- JHV `world -> helioprojective -> TAN plane -> pixel`
- matches Astropy to numerical precision

2. Forward `AZP` is correct for the current HI files.

- JHV `world -> helioprojective -> AZP plane -> pixel`
- matches Astropy to numerical precision
- current HI files are non-slanted: `PV2_2` is absent, so `gamma = 0`

3. Inverse `AZP` direction recovery is correct for the current HI files.

- `AZP plane -> helioprojective angles`
- implemented and validated in
  [extra/test/validate_jhv_wcs_against_astropy.py](extra/test/validate_jhv_wcs_against_astropy.py)
- matches Astropy to numerical precision

So the unresolved part is not the projection formulas. It is what 3D surface
JHV should render for HI/AZP/ZPN in orthographic mode.


What FITS/WCS actually gives
----------------------------

For zenithal projections, FITS/WCS gives:

- a mapping between angular coordinates and image-plane coordinates
- `CRVAL` center direction
- `CROTA` or equivalent `PC`
- projection parameters such as `PV2_1 = mu` and `PV2_2 = gamma`

It does not give:

- a unique physical distance for a rendered image sheet in the Sun-centered 3D scene
- a unique rule for where along a recovered observer ray a rendered point should sit

That missing degree of freedom is the core reason the remaining problem exists.


What Thompson 2006 clarifies
----------------------------

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

So Thompson reinforces the current state rather than changing it:

- forward/inverse WCS is constrained and testable
- orthographic 3D embedding remains a viewer convention


Current HI header facts
-----------------------

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


Projection formulas in current JHV conventions
----------------------------------------------

In the current GLSL/validator conventions:

- `phi, theta` are helioprojective longitude/latitude
- `phi0, theta0` come from `CRVAL`
- `mu = PV2_1`
- `gamma = PV2_2` in degrees in FITS, converted to radians for trig

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


Projection-agnostic decomposition
---------------------------------

The clean way to think about zenithal embedding is to separate two problems.

Part 1: recover an observer-frame ray direction

- start from a point in native WCS plane coordinates
- invert the radial law:
  - `TAN`: `eta = atan(R)`
  - `AZP`: `eta = inverseAzpRadius(...)`
  - `ZPN`: `eta = inverseZpnRadius(...)`
- invert the native spherical rotation around `CRVAL`
- convert the resulting helioprojective angles to an observer-frame ray direction `d`

Part 2: choose a viewer placement rule

- choose where along that ray the rendered point sits
- mathematically: choose `lambda(d)` and place `p_obs = lambda(d) * d`

Part 1 is constrained by WCS.
Part 2 is not.


What failed and why
-------------------

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

So the constant-plane family is not a viable full-image embedding for HI2.


Embedding constraints
---------------------

Any acceptable HI/AZP/ZPN orthographic embedding must satisfy:

1. `CRVAL` shifts the image center in 3D.
2. `CROTA` rotates around the image center, not around the Sun.
3. The visible domain stays a single non-self-intersecting sheet.
4. The validated WCS path remains authoritative for sampling.
5. The structure generalizes to `ZPN`.
6. HI wide-field appearance should remain barrel-like rather than
   pincushion-like.


Admissible model families
-------------------------

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
- place all points on a shell of chosen radius `S` around the observer

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


Most plausible next implementation direction
--------------------------------------------

The best next candidate is now the observer shell model, not the observer plane.

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

At this point there is no evidence that `S` can be derived from current FITS/WCS
for HI. It must come from:

- external instrument geometry
- an existing JHV convention
- or a new explicit viewer convention


Architectural consequence
-------------------------

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


What Astropy can test
---------------------

Astropy can test all WCS-defined parts.

Yes, Astropy can validate:

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

That is exactly what the current validator already does for forward `TAN`/`AZP`
and inverse non-slanted `AZP`.


What Astropy cannot test
------------------------

Astropy cannot test the unresolved orthographic HI problem directly.

Astropy does not define:

- where the image sheet should sit in the Sun-centered 3D scene
- what `lambda(d)` should be along an observer ray
- which JHV visualization convention should be used for side-on viewing

This remains true even when Thompson 2006 is taken into account: the paper
constrains the solar-image coordinate formalism, not the 3D viewer placement
rule.

So the answer is:

- WCS part: yes, fully testable against Astropy
- 3D embedding part: no, not from Astropy alone


Practical testing strategy
--------------------------

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


Bottom line
-----------

The unresolved problem is not “how to implement `AZP`”.

It is:

- choose and document a JHV 3D viewer convention for zenithal wide-field images
- then implement that convention in a geometry path that starts from inverse WCS,
  not from a guessed scene-space hit point
