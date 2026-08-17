# 3D FITS volume data for JHelioviewer

This note defines the FITS profile currently accepted by JHelioviewer (JHV) for scalar 3D volumes. It is intended for
producers that convert simulation output, such as coronal-model results, into a directly displayable JHV product.

This is deliberately a narrow profile. A file can be valid FITS and have a valid WCS without being a supported JHV
volume. Restricting the input to a linear, heliocentric Cartesian grid gives JHV an unambiguous mapping from voxels to
its 3D solar world and a direct mapping from integer FITS samples to a GLES 3D texture.

The implementation is in:

- `src/org/helioviewer/jhv/opengl/volume/FitsVolumeLoader.java`
- `src/org/helioviewer/jhv/opengl/volume/VolumeData.java`
- `src/org/helioviewer/jhv/opengl/GLSLVolume.java`
- `resources/glsl/volume.vert` and `resources/glsl/volume.frag`

Reference files are `extra/test/data/synthetic-corona-8.fits` and
`extra/test/data/synthetic-corona-16.fits`. `extra/test/VolumeLayerTest.java` additionally creates temporary files that
exercise axis permutations, non-zero reference coordinates, and `PCi_j` and `CDi_j` matrices.

## Producer checklist

A JHV volume must satisfy all of the following:

1. Store one scalar on a three-dimensional, rectilinear heliocentric Cartesian grid.
2. Put the grid in a primary image HDU or image extension with `NAXIS=3` and positive `NAXIS1`, `NAXIS2`, and
   `NAXIS3`.
3. Use integer `BITPIX=8` or `BITPIX=16`. Other integer widths and floating-point FITS arrays are not accepted.
4. Quantize the scientific quantity into the available integer range before writing the file, and describe that
   quantization with finite `BSCALE` and `BZERO` values.
5. Use `CTYPEi=SOLX`, `SOLY`, and `SOLZ`, each exactly once.
6. Use `CUNITi=solRad`, `m`, `km`, or `Mm`, with this exact spelling and case.
7. Supply a complete, nonsingular linear WCS using one of the forms described below.
8. Supply the observation time, `DSUN_OBS`, and one complete supported observer-coordinate pair.
9. Write `RSUN_REF=695700000.0` metres. JHV currently uses this fixed radius internally even if another
   `RSUN_REF` value is present.
10. Use `BLANK` when undefined voxels exist. Do not encode undefined samples as a valid low value.
11. Write a valid physical unit in `BUNIT`, or omit `BUNIT` for a dimensionless scalar. `BUNIT='1'` is not valid FITS
    unit syntax.

The keyword-level requirements are:

| Keyword | Status | JHV use |
| --- | --- | --- |
| `BITPIX` | Required | Must be 8 or 16; selects the scalar texture representation. |
| `NAXIS` | Required | Must be 3. |
| `NAXIS1`, `NAXIS2`, `NAXIS3` | Required | Define positive voxel dimensions. |
| `CTYPE1`, `CTYPE2`, `CTYPE3` | Required | Must contain `SOLX`, `SOLY`, and `SOLZ` exactly once. |
| `CUNIT1`, `CUNIT2`, `CUNIT3` | Required | Select one supported length unit for each WCS row. |
| `CRPIX1`, `CRPIX2`, `CRPIX3` | Required | Define the one-based reference pixel. |
| `CRVAL1`, `CRVAL2`, `CRVAL3` | Required | Define the Cartesian coordinate at the reference pixel. |
| `CDELT1`, `CDELT2`, `CDELT3` | Conditional | Required for diagonal and PC forms; unused for CD. |
| `PCi_j` or `CDi_j` | Optional | Define a general linear transform; omit both for diagonal `CDELTi`. |
| `DATE-AVG` or `DATE-OBS` | Required | Defines the time of the observer-dependent Cartesian frame. |
| `DSUN_OBS` | Required | Defines observer distance in metres and must be positive. |
| `CRLN_OBS` and `CRLT_OBS` | Conditional | One supported complete observer-direction pair. |
| `HGLN_OBS` and `HGLT_OBS` | Conditional | Alternative complete observer-direction pair. |
| `RSUN_REF` | Producer-profile requirement | Must be `695700000.0 m` for consistency with JHV's fixed radius. |
| `BSCALE`, `BZERO` | Optional but recommended | Define the physical quantity represented by stored integers. |
| `BLANK` | Optional | Identifies undefined stored integer values and causes JHV to construct a GPU mask. |
| `BUNIT` | Optional | Records a valid FITS physical unit; omit it for dimensionless values. |
| `EXTNAME`, `OBJECT` | Optional | Provide the displayed layer name. |
| `WCSNAME` | Optional | Describes the coordinate system for humans and other software. |

## Data array and axis order

FITS axis 1 is the fastest-varying array dimension. If `x`, `y`, and `z` are zero-based pixel indices associated with
`NAXIS1`, `NAXIS2`, and `NAXIS3`, respectively, JHV reads the sample at:

```text
x + NAXIS1 * (y + NAXIS2 * z)
```

The three pixel axes do not have to correspond to `SOLX`, `SOLY`, and `SOLZ` in that order. The `CTYPEi` values and
the linear WCS matrix define that association. Nevertheless, writing `SOLX`, `SOLY`, and `SOLZ` on axes 1, 2, and 3
is recommended when no permutation is needed, because it is easier to inspect and less error-prone.

JHV loads the complete array into CPU memory and then uploads the complete array as one GLES 3D texture. Every
dimension must therefore fit the device's maximum 3D texture size, and the complete cube must fit both CPU and GPU
memory. FITS image compression can reduce storage and transfer size, but does not reduce the decoded CPU or GPU
footprint.

Approximate GPU storage, excluding driver overhead, is:

| Input | Scalar texture | Optional `BLANK` mask |
| --- | ---: | ---: |
| `BITPIX=8` | 1 byte/voxel (`R8`) | 1 byte/voxel (`R8`) |
| `BITPIX=16` | 2 bytes/voxel (`R16F`) | 1 byte/voxel (`R8`) |

The loader selects the first three-dimensional image HDU in the file. Producers should normally provide exactly one
such HDU rather than relying on this selection rule.

Users load the file with **File > Open Volume Layer...**. The file chooser accepts `.fts`, `.fits`, and `.fits.gz`.
The layer name is taken from `EXTNAME`, then from `OBJECT`, and finally from the file name if neither keyword is
present. `EXTNAME` is therefore recommended for a volume stored in an image extension, while `OBJECT` is appropriate
for the primary-HDU examples in this note.

## Scalar encoding

For a stored integer `s`, the FITS physical value is:

```text
q = BZERO + BSCALE * s
```

`BSCALE` defaults to `1` and `BZERO` defaults to `0`, but producers should write both when the array is a quantized
scientific quantity. `BSCALE` must be non-zero, and both values must be finite.

JHV does not derive a display range from the minimum and maximum values actually present in the cube. It maps the
complete representable stored range monotonically to the texture range `[0, 1]`. A producer should therefore perform
the desired clipping and scaling before writing the FITS file. Otherwise, a narrow useful range inside a much wider
declared integer range will have poor contrast and precision.

Negative `BSCALE` is supported. JHV reverses the normalized texture values so that increasing texture intensity still
corresponds to increasing physical value.

### `BITPIX=8`

FITS stores values from 0 through 255. JHV uploads the defined stored values to an `R8` normalized texture. For a
desired physical interval `[q_min, q_max]` using the complete range:

```text
BSCALE = (q_max - q_min) / 255
BZERO  = q_min
s      = round((q - q_min) / BSCALE)
```

If one code is reserved for `BLANK`, it cannot also represent a defined value. The producer must account for that
when choosing its quantization and clipping policy.

### `BITPIX=16`

FITS stores signed integers from -32768 through 32767. For a desired physical interval `[q_min, q_max]` using the
complete range:

```text
BSCALE = (q_max - q_min) / 65535
BZERO  = q_min + 32768 * BSCALE
s      = round((q - BZERO) / BSCALE)
```

JHV normalizes the stored range to `[0, 1]`, converts it to IEEE binary16 on the CPU, and uploads it as `R16F`.
Consequently, `BITPIX=16` preserves much more input precision than `BITPIX=8`, but the GPU representation is
half-precision floating point rather than a 16-bit normalized integer texture.

The physical endpoint values derived from `BSCALE` and `BZERO` are retained in `VolumeData`. The renderer does not
use them to apply an interactive physical transfer function.

### `BUNIT`

`BUNIT` describes the physical quantity after applying `BSCALE` and `BZERO`. Use a valid FITS unit string appropriate
for the produced quantity. Omit `BUNIT` for dimensionless data; an empty unit is dimensionless in the FITS unit
grammar. In particular:

```fits
BUNIT  = '1'                 / not valid FITS unit syntax
```

must not be used. JHV retains `BUNIT` as metadata but the renderer does not alter rendering based on the unit.

## Undefined voxels and `BLANK`

`BLANK` is the stored integer code for an undefined voxel. It is interpreted before applying `BSCALE` and `BZERO`, as
required by FITS. It must lie inside the stored range:

- 0 through 255 for `BITPIX=8`
- -32768 through 32767 for `BITPIX=16`

JHV rejects a file if `BLANK` is outside the appropriate range or if every voxel is blank.

When `BLANK` is present, JHV constructs a separate `R8` validity texture: 255 for defined voxels and 0 for undefined
voxels. The scalar texture stores zero at undefined locations. During trilinear sampling, the shader divides the
filtered scalar value by the filtered validity value. This prevents undefined neighbors from darkening a defined
sample merely because texture filtering crossed a mask boundary. The remaining fractional validity reduces opacity,
so a partially defined interpolation neighborhood does not behave as fully occupied material.

This mask is derived entirely by JHV. Producers should not add a separate mask HDU for this purpose.

## Heliocentric Cartesian coordinates

The supported world-coordinate axes are those described by Thompson's solar-coordinate convention:

- `SOLX`: heliocentric distance toward solar west in the observer's image plane
- `SOLY`: heliocentric distance toward solar north in the observer's image plane
- `SOLZ`: heliocentric distance from Sun centre toward the observer

The origin is Sun centre. These are observer-aligned Cartesian coordinates, not Carrington Cartesian coordinates and
not a spherical grid of radius, longitude, and latitude. A model sampled on a spherical or irregular grid must be
resampled to a rectilinear `SOLX/SOLY/SOLZ` grid before it is supplied to JHV.

Each of `SOLX`, `SOLY`, and `SOLZ` must occur exactly once among `CTYPE1`, `CTYPE2`, and `CTYPE3`.

JHV accepts these exact coordinate units:

| `CUNITi` | Meaning |
| --- | --- |
| `solRad` | JHV solar radii |
| `m` | metres |
| `km` | kilometres |
| `Mm` | megametres |

JHV currently defines one solar radius as exactly 695700000 metres. `RSUN_REF` is written for a complete,
interoperable solar WCS, but the loader does not currently use its value to change this conversion. Producers must
therefore write:

```fits
RSUN_REF=          695700000.0 / [m] assumed physical solar radius
```

Using another `RSUN_REF` value while expressing coordinates in `solRad`, or expecting it to alter conversions from
metres, will produce a scale disagreement in JHV.

## Reference pixels and coordinates

FITS pixel coordinates are one-based and refer to pixel centres. Integer pixel coordinate 1 is the centre of the
first voxel, not its lower boundary. `CRVALi` is the world-coordinate value at the reference pixel `CRPIXi`.

For a cube with `NAXISi` cells whose geometrical centre represents Sun centre, the recommended values are:

```text
CRPIXi = (NAXISi + 1) / 2
CRVALi = 0
```

Thus, a 256-cell axis centred on the Sun has `CRPIXi=128.5`. For an odd-sized 255-cell axis, it has
`CRPIXi=128.0`.

Neither `CRPIXi` nor `CRVALi` is required to describe Sun centre. A cropped or displaced Cartesian volume can put the
reference pixel at a convenient location and set `CRVALi` to that location's actual `SOLX`, `SOLY`, or `SOLZ`
coordinate. JHV applies both the reference translation and the linear matrix before rotating the volume into its
Carrington 3D world.

JHV treats voxels as cells centred on their WCS pixel coordinates. After finding the world coordinate of the first
voxel centre, it subtracts half of each of the three pixel-step vectors to obtain the outer corner of the rendered
volume. The rendered parallelepiped therefore encloses the complete voxel cells rather than joining only their
centres.

## Linear WCS forms

Let `p` be the one-based FITS pixel-coordinate vector, `r` the `CRPIXi` vector, and `v` the `CRVALi` vector. JHV
supports the linear relation:

```text
world = v + M (p - r)
```

Rows of `M` use the units declared by the corresponding `CUNITi`; columns correspond to FITS pixel axes. One of the
following representations must be used.

### Diagonal `CDELTi`

When neither a `PCi_j` nor a `CDi_j` keyword is present, JHV constructs:

```text
M = diag(CDELT1, CDELT2, CDELT3)
```

This is the simplest and recommended form for an axis-aligned regular cube.

### `CDELTi` and `PCi_j`

If any `PCi_j` keyword is present, `CDELT1`, `CDELT2`, and `CDELT3` are required and:

```text
M_i,j = CDELTi * PCi_j
```

Missing diagonal `PCi_i` elements default to 1; missing off-diagonal elements default to 0. A complete matrix is
still recommended for clarity.

### `CDi_j`

If no `PCi_j` keyword is present and at least one `CDi_j` keyword is present:

```text
M_i,j = CDi_j
```

Missing `CDi_j` elements default to 0. `CDELTi` is not used in this form. Each row of the CD matrix is expressed in
the corresponding `CUNITi` per pixel.

Do not write both PC and CD forms. JHV gives PC precedence, but relying on that implementation rule makes the file
ambiguous to readers and producers.

The resulting transform must be finite and nonsingular. Rotations, reflections, axis permutations, unequal scales,
and linear shear are supported. JHV uses the determinant sign to render mirrored grids with the correct face winding.
Nonlinear spatial axes and projection suffixes are not supported for volumes.

## Time and observer metadata

The `SOLX/SOLY/SOLZ` basis depends on the observer. JHV therefore requires an observation time, observer distance,
and observer direction.

### Time

JHV accepts the first keyword present in this order:

1. `DATE-AVG`
2. `DATE_AVG`
3. `DATE_OBS`
4. `DATE-OBS`

Producers should normally use the standard hyphenated spelling and should use `DATE-AVG` when a volume represents an
average over an interval. An ISO timestamp such as `2026-08-17T00:00:00.000` is recommended. A trailing `Z` is
accepted, and a date without a time is interpreted as midnight.

### Distance and direction

`DSUN_OBS` is required, must be positive, and is expressed in metres:

```fits
DSUN_OBS=       151470458469.0 / [m] distance from centre of Sun to observer
```

Supply exactly one complete observer-coordinate pair:

| Pair | Meaning |
| --- | --- |
| `CRLN_OBS`, `CRLT_OBS` | Carrington heliographic longitude and heliographic latitude, in degrees |
| `HGLN_OBS`, `HGLT_OBS` | Stonyhurst heliographic longitude and latitude, in degrees |

Do not mix one keyword from each pair. If both complete pairs are present, JHV currently gives the Stonyhurst pair
precedence; producers should not rely on that rule.

For an Earth observer, `HGLN_OBS` is normally approximately zero, whereas `CRLN_OBS` varies with time. The latitude
is the observer's heliographic latitude, commonly called B0 for Earth. Writing both Carrington values as zero does not
describe the Earth observer except at a coincidental longitude and latitude.

JHV converts the observer-aligned Cartesian reference point and all three pixel-step vectors into its Carrington 3D
world. The conversion has been checked against the equivalent SunPy Heliocentric-to-Heliographic-Carrington
transformation. JHV stores its Cartesian world components in an internal order, but producers must write the standard
`SOLX/SOLY/SOLZ` axes and should not compensate for JHV's internal representation.

## Complete axis-aligned example

The following excerpt describes a 256 cubed, Sun-centred volume extending from -3 to +3 solar radii along every
observer-aligned axis. The values shown for the observer correspond to the stated timestamp. Standard mandatory FITS
cards such as `SIMPLE`, `NAXIS`, and `END` are included here where relevant but are normally written by a FITS
library.

```fits
SIMPLE  =                    T
BITPIX  =                    8
NAXIS   =                    3
NAXIS1  =                  256
NAXIS2  =                  256
NAXIS3  =                  256
OBJECT  = 'synthetic coronal density'
DATE-OBS= '2026-08-17T00:00:00.000'
BSCALE  =  0.003921568627451 / physical value per stored integer step
BZERO   =                  0.0 / physical value represented by stored zero
BLANK   =                  255 / undefined stored value
WCSNAME = 'Heliocentric Cartesian'
RSUN_REF=          695700000.0 / [m] assumed physical solar radius
DSUN_OBS=       151470458469.0 / [m] distance from centre of Sun to observer
CRLN_OBS=   168.63517776981791 / [deg] Carrington longitude of observer
CRLT_OBS=    6.722914954331527 / [deg] Carrington latitude of observer
CTYPE1  = 'SOLX'
CUNIT1  = 'solRad'
CRPIX1  =                128.5 / [pixel] reference pixel
CRVAL1  =                  0.0 / [solRad] coordinate at reference pixel
CDELT1  =            0.0234375 / [solRad/pixel] coordinate increment
CTYPE2  = 'SOLY'
CUNIT2  = 'solRad'
CRPIX2  =                128.5 / [pixel] reference pixel
CRVAL2  =                  0.0 / [solRad] coordinate at reference pixel
CDELT2  =            0.0234375 / [solRad/pixel] coordinate increment
CTYPE3  = 'SOLZ'
CUNIT3  = 'solRad'
CRPIX3  =                128.5 / [pixel] reference pixel
CRVAL3  =                  0.0 / [solRad] coordinate at reference pixel
CDELT3  =            0.0234375 / [solRad/pixel] coordinate increment
```

`BUNIT` is intentionally absent because this example contains a normalized, dimensionless synthetic scalar.

The first voxel centre is at -2.98828125 solar radii on each axis. Its cell begins at -3, while the last cell ends at
+3. The coordinate increment is `6 / 256 = 0.0234375` solar radii per pixel.

## Non-zero reference coordinates

For a regular subvolume whose geometrical centre is at `(x0, y0, z0)` rather than Sun centre, keep the reference
pixels at the array centre and write the centre coordinates as `CRVALi`:

```fits
CRPIX1  =                128.5
CRPIX2  =                128.5
CRPIX3  =                128.5
CRVAL1  =                   x0 / [CUNIT1]
CRVAL2  =                   y0 / [CUNIT2]
CRVAL3  =                   z0 / [CUNIT3]
```

Those placeholders are explanatory and are not literal valid FITS values. The numeric values must be written in the
units of their corresponding axes. JHV translates the complete volume to that heliocentric position and then applies
the observer-to-Carrington orientation.

For a permuted or matrix-transformed WCS, `CRVALi` belongs to world-coordinate row `i`, while `CRPIXi` belongs to
pixel axis `i`, exactly as in FITS WCS. Producers should use a WCS library rather than manually permuting these values.

## How JHV renders the volume

The loader converts the FITS WCS into four world-space quantities:

- the outer corner of the first voxel
- the full `NAXIS1` edge vector
- the full `NAXIS2` edge vector
- the full `NAXIS3` edge vector

These vectors define a possibly rotated, reflected, or sheared parallelepiped in JHV's Carrington scene. The vertex
shader maps a unit cube to that parallelepiped. The fragment shader transforms the orthographic view ray back into
texture coordinates and ray-marches through the 3D texture with approximately two samples per voxel along the most
rapidly crossed texture axis, capped at 2048 steps.

The current renderer:

- is available only in JHV's orthographic 3D view;
- uses trilinear texture filtering;
- maps normalized values through a user-selectable JHV color table;
- treats the unit solar sphere as opaque and stops integration when a ray reaches it;
- uses premultiplied-alpha accumulation;
- does not write depth for the translucent volume;
- applies small output dithering to reduce visible display banding;
- provides a global layer-opacity control which multiplies the final premultiplied RGBA result;
- has no producer-controlled transfer function, clipping range, or opacity curve yet.

These limitations make producer-side scalar preparation important. A general physical cube with a large dynamic
range will usually need a deliberate transform—linear, logarithmic, or another domain-appropriate mapping—before
quantization. Record the physical meaning and inverse transform in additional metadata or accompanying documentation;
the current JHV renderer does not infer it.

## Unsupported inputs

The current loader rejects or does not interpret:

- `BITPIX=32`, `BITPIX=64`, `BITPIX=-32`, and `BITPIX=-64`;
- four-dimensional cubes or time as a FITS array axis;
- spherical radius/longitude/latitude grids;
- irregular, curvilinear, adaptive, or unstructured grids;
- nonlinear spatial WCS projections;
- alternate-WCS suffixes for selecting the volume coordinate system;
- a separate producer-supplied validity-mask HDU;
- multiple scalar channels in one volume;
- vector fields stored as an additional component axis.

For these inputs, an intermediary producer must select the desired scalar, resample it onto a regular heliocentric
Cartesian grid, apply the desired physical-to-display transform, quantize it to 8 or 16 bits, and write this profile.

## Validation before delivery

At minimum, a producer should verify:

1. FITS structural validity without reading scaled data.
2. `BITPIX`, dimensions, and the stored integer range.
3. The WCS coordinates of the first voxel centre, central voxel or central half-pixel, and last voxel centre.
4. The eight outer cell corners after the half-voxel extension.
5. Observer metadata by constructing the corresponding SunPy frame.
6. Axis order with a deliberately asymmetric test pattern; a spherical test field cannot reveal permutations or
   reflections.
7. `BLANK` handling at isolated voxels and along a boundary.
8. The physical values recovered from representative stored codes using `BZERO + BSCALE * s`.
9. JHV rendering from several viewpoints, including occultation by the solar sphere.

With Astropy and SunPy installed, the following is a useful metadata check:

```python
from astropy.io import fits
from astropy.wcs import WCS
from astropy.wcs.utils import wcs_to_celestial_frame
import sunpy.coordinates  # registers the solar WCS-to-frame mappings

path = "volume.fits"
with fits.open(path, do_not_scale_image_data=True) as hdus:
    hdus.verify("exception")
    header = hdus[0].header
    assert header["BITPIX"] in (8, 16)
    assert header["NAXIS"] == 3
    assert {header[f"CTYPE{i}"] for i in range(1, 4)} == {"SOLX", "SOLY", "SOLZ"}
    assert header["RSUN_REF"] == 695700000.0
    frame = wcs_to_celestial_frame(WCS(header))
    print(frame)
```

This checks FITS/WCS interoperability, but it is not a substitute for loading the file in JHV: JHV intentionally
supports only the restricted profile documented here.

## References

- [FITS Standard 4.0](https://fits.gsfc.nasa.gov/fits_standard.html)
- W. T. Thompson, [Coordinate systems for solar image data](https://doi.org/10.1051/0004-6361:20054262),
  *Astronomy & Astrophysics* 449 (2006), 791-803
- [SunPy coordinates and WCS](https://docs.sunpy.org/en/stable/topic_guide/coordinates/wcs.html)
- [SunPy observer metadata helper](https://docs.sunpy.org/en/stable/generated/api/sunpy.map.header_helper.get_observer_meta.html)
