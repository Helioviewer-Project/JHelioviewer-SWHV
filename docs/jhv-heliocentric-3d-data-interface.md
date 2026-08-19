\include{./templates/preamble.md}
---
title: JHV Heliocentric 3D data interface
subject: Space Weather HelioViewer
date: 2026-08-19
lof: false
lot: false
---

`id: \exec{git hash-object \file}`

# Introduction

This document is a working interface between scientific-model projects and JHelioviewer (JHV). It describes how a
project can deliver 3D results that JHV can place and render in the solar scene without losing the product's spatial,
temporal, or scientific meaning.

Two complementary product types are covered:

- a FITS scalar volume for a quantity sampled on a regular 3D grid;
- a glTF scene for explicit geometry such as field lines, surfaces, or points.

glTF is the 3D scene standard. It has two file forms: `.gltf` stores the scene description as JSON and may reference
separate or Base64-embedded buffers and images, while GLB stores the same glTF scene and its binary resources in one
`.glb` file. GLB is convenient to distribute and avoids Base64 expansion. For a binary-dominated, self-contained
asset, its binary payload is about 25% smaller than the equivalent Base64 payload because Base64 encodes every three
bytes as four; the total saving depends on the amount of JSON and image data. There is no corresponding size advantage
over a `.gltf` file accompanied by external binary resources.

The profiles below describe the interface supported by JHV today. They are intentionally more specific than the FITS
and glTF standards themselves. Keeping that supported profile explicit gives producing projects a stable target and
gives both sides a concrete basis for discussing future extensions.

The COCONUT conversion near the end is a worked example. Its numerical and scientific choices are not requirements of
the interface.

# Relationship to Qorona's existing JHV exports

Qorona already produces two kinds of output designed specifically for JHV:

- WCS-registered 2D FITS rasters from its squashing-factor and white-light renderers;
- Carrington field-line geometry in SunJSON.

Those products remain the direct choices for their respective purposes. The interfaces in this document add a
regular 3D scalar volume and a general-purpose geometry scene; they do not replace Qorona's existing image or
field-line exports.

## Why SunJSON for JHV-native geometry

SunJSON is JHV's native interchange format for solar geometry. It supports points, polylines, and ellipses, with
colours and a thickness or size appropriate to the primitive. Each coordinate is `[r, lon, lat]`: `r` is the radius
in solar radii, while `lon` and `lat` are Carrington longitude and latitude in degrees. JHV converts these spherical
Carrington coordinates directly into its world Cartesian coordinates. The top-level `time` selects the geometry on
JHV's timeline; it is not used to rotate an observer-relative coordinate frame.

Qorona uses the SunJSON line primitive for its field-line products. It assumes that the solution is
Carrington-aligned and converts each Cartesian trace to radius, Carrington longitude, and latitude. SunJSON also
carries the JHV-specific line thickness and per-vertex RGBA colour. A Qorona field-line product intended primarily
for JHV should therefore normally use SunJSON.

## Why glTF for a scene

glTF is intended for general 3D scenes rather than solar connection data. It is the appropriate route for triangle
meshes, surfaces, textured objects, points, and scenes that should also open in ordinary 3D viewers.

A glTF `POSITION` attribute is a Cartesian three-component vertex position. Solar spherical tuples cannot be stored
there as `[r, lon, lat]` while retaining interoperability: a standard viewer would interpret those
three numbers as Cartesian `x`, `y`, and `z` and display distorted geometry. The values stored in `POSITION` must
therefore be Cartesian coordinates. Producers whose source data uses spherical coordinates must convert it to
Cartesian coordinates before export. JHV-specific solar metadata identifies how those Cartesian axes are placed in
the solar world. Other viewers ignore that metadata but still display the geometry correctly in its local Cartesian
frame.

For field lines alone, glTF gives up useful SunJSON semantics—notably portable JHV line thickness—and introduces
additional coordinate-frame metadata. Its value is that the same scene can represent field lines together with
meshes or other general scene geometry. The GLB fixture later in this document combines field lines, a meshed current
sheet, and point markers at the inner and outer boundary endpoints of open field lines to exercise the general glTF
scene interface; it is not a recommendation to replace Qorona's SunJSON export for field-line-only products.

Qorona's FITS exports are likewise distinct from the volume profile below. They are floating-point 2D helioprojective
rasters with `HPLN-TAN`/`HPLT-TAN` WCS: the squashing-factor export contains the line-of-sight-averaged `log10(Qperp)`,
while the white-light export contains a display image plus raw polarized- and total-brightness extensions. The 3D
profile below instead describes an integer scalar cube with three Cartesian `SOLX`/`SOLY`/`SOLZ` axes.

# The shared contract

Both product types use the same observer-aligned heliocentric Cartesian frame and observation metadata. A matched FITS
volume and glTF scene can therefore be loaded together and occupy the same JHV world.

## Spatial frame

The coordinate axes follow the heliocentric Cartesian convention described by Thompson (2006):

- `SOLX` points toward solar west in the observer's image plane;
- `SOLY` points toward solar north in the observer's image plane;
- `SOLZ` points from Sun centre toward the observer.

The origin is Sun centre. These are observer-aligned Cartesian coordinates, rather than Carrington Cartesian
coordinates or spherical radius/longitude/latitude coordinates.

Each axis has a length unit. JHV currently accepts the following exact values:

| Unit | Interpretation |
| --- | --- |
| `solRad` | solar radii, using 695700000 metres per radius |
| `m` | metres |
| `km` | kilometres |
| `Mm` | megametres |

For consistency between software, products in this profile record:

```text
RSUN_REF = 695700000.0 m
```

JHV currently uses that radius internally rather than deriving its scale from `RSUN_REF`. A different value would
therefore create a scale disagreement.

## Observation metadata

The `SOLX/SOLY/SOLZ` basis depends on the observer, so the product identifies both the observation time and the
observer direction.

The preferred time keywords are:

- `DATE-OBS` for an observation or model state at one time;
- `DATE-AVG` for a product representing an average over an interval.

JHV also accepts `DATE_OBS` and `DATE_AVG`. A FITS date-time such as `2025-10-09T18:19:52.000` is a good interchange
form. JHV tolerates a trailing `Z`, but it is not valid in a FITS date-time value: FITS does not support time-zone
designators. The time scale belongs in `TIMESYS`, whose FITS default is `UTC`.

The observer direction is supplied as one complete pair:

| Keywords | Coordinates |
| --- | --- |
| `CRLN_OBS`, `CRLT_OBS` | Carrington heliographic longitude and latitude, in degrees |
| `HGLN_OBS`, `HGLT_OBS` | Stonyhurst heliographic longitude and latitude, in degrees |

`DSUN_OBS` records the distance from Sun centre to the observer in metres. It is required by the FITS-volume profile
and is included in scene metadata so paired products carry the same complete observer description.

When several time spellings are present, JHV currently checks `DATE-AVG`, `DATE_AVG`, `DATE_OBS`, and `DATE-OBS` in
that order. A product with one time keyword avoids depending on this precedence rule.

For this 3D interface, Carrington observer coordinates are preferred. The volume and scene readers use the complete
Carrington pair when both coordinate pairs are present; Stonyhurst remains a supported alternative. Supplying only
the intended pair keeps the product unambiguous.

`DSUN_OBS` does not currently change glTF placement, but carrying it keeps the scene self-describing and aligned
with a companion FITS product.

For an Earth observer, both Carrington coordinates will generally be non-zero. They should be calculated for the
product time rather than replaced by a nominal `(0, 0)` observer.

## Placement in JHV

Product coordinates are expressed in the declared `SOLX/SOLY/SOLZ` basis. On loading, JHV uses the observation
metadata to rotate them into its Carrington world. The same placement is applied consistently to meshes, lines, and
points before the scene is displayed.

The user's subsequent rotation of the JHV scene is a view operation. It does not change the product's physical
placement.

# FITS scalar-volume profile

The FITS product contains one display-ready scalar on a regular three-dimensional grid with a finite, nonsingular
affine WCS. This allows a direct mapping from FITS voxels to a GLES 3D texture and from the WCS to a parallelepiped in
JHV's solar world.

## Compatibility summary

| Item | Supported profile |
| --- | --- |
| Image | first image HDU with `NAXIS=3` and positive axis lengths |
| Storage | ordinary FITS image or registered tiled-compressed image |
| Samples | integer `BITPIX=8` or `BITPIX=16` |
| Undefined samples | optional FITS `BLANK` stored-value code |
| Axes | `SOLX`, `SOLY`, and `SOLZ`, each exactly once |
| Axis units | `solRad`, `m`, `km`, or `Mm` |
| WCS | diagonal `CDELTi`, `CDELTi` with `PCi_j`, or `CDi_j` |
| Time | `DATE-OBS` or `DATE-AVG` |
| Observer | `DSUN_OBS` and one complete supported direction pair |

The loader currently does not interpret floating-point FITS arrays, additional scalar/component axes, nonlinear
spatial WCS, curvilinear grids, or unstructured grids. Such source data can still be used after a producing project
has selected a scalar and sampled it onto this interchange grid.

JHV selects the first three-dimensional image HDU. A product with one logical 3D image is the least ambiguous form.
The displayed layer name comes from `EXTNAME`, then `OBJECT`, and finally the file name.

## Header fields

| Keyword | Role in the interface |
| --- | --- |
| `BITPIX` | `8` or `16`; selects the scalar texture representation. |
| `NAXIS`, `NAXIS1..3` | Describe a non-empty 3D image. |
| `CTYPE1..3` | Contain `SOLX`, `SOLY`, and `SOLZ` exactly once. |
| `CUNIT1..3` | Give the length unit of each WCS row. |
| `CRPIX1..3` | Give the one-based reference pixel coordinates. |
| `CRVAL1..3` | Give the Cartesian coordinate at the reference pixel. |
| `CDELT1..3` | Supply the diagonal scale, or the row scales of a PC matrix. |
| `PCi_j` or `CDi_j` | Optionally supply a general affine linear part. |
| `DATE-OBS` or `DATE-AVG` | Identify the product time. |
| `TIMESYS` | Optionally state the FITS time scale; the FITS default is `UTC`. |
| `DSUN_OBS` | Give the positive observer distance in metres. |
| observer direction pair | Orient the `SOLX/SOLY/SOLZ` basis. |
| `RSUN_REF` | Record the agreed physical solar radius. |
| `BSCALE`, `BZERO` | Relate stored integers to the represented scalar. |
| `BLANK` | Identify undefined stored integers, when present. |
| `BUNIT` | Record a FITS-valid unit for the represented scalar, when applicable. |
| `EXTNAME`, `OBJECT` | Supply a useful layer name. |
| `WCSNAME`, `BTYPE`, `HISTORY` | Describe the coordinate system, scalar, and provenance. |

FITS defaults for `BSCALE=1` and `BZERO=0` are accepted. Writing the cards explicitly is helpful when the stored
integers are a quantized representation of another quantity.

## Array and axis order

FITS axis 1 is the fastest-varying dimension. With zero-based array indices `x`, `y`, and `z`, the linear sample index
is:

```text
x + NAXIS1 * (y + NAXIS2 * z)
```

The pixel axes may be permuted. `CTYPEi` and the WCS matrix establish which world component each pixel axis affects.
Using `SOLX`, `SOLY`, and `SOLZ` on axes 1, 2, and 3 is nevertheless the easiest arrangement to inspect when no
permutation is needed.

JHV loads and uploads the whole cube. Each dimension must fit the device's maximum 3D texture size, and the decoded
cube must fit in CPU and GPU memory. Approximate GPU storage is:

| FITS samples | Scalar texture | Optional validity mask |
| --- | ---: | ---: |
| `BITPIX=8` | 1 byte/voxel (`R8`) | 1 byte/voxel (`R8`) |
| `BITPIX=16` | 2 bytes/voxel (`R16F`) | 1 byte/voxel (`R8`) |

## Scalar encoding

The FITS image contains the display-ready quantized scalar. JHV does not derive a new range from the voxel values or
apply a logarithmic, asinh, or other scientific transform.

For `BITPIX=8`, JHV uploads the stored byte codes to an `R8` normalized texture. For `BITPIX=16`, it normalizes the
signed 16-bit codes, converts them to IEEE binary16, and uploads them to `R16F`.

JHV does not apply `BZERO + BSCALE * stored_value` to every voxel. It uses `BSCALE` and `BZERO` to retain the physical
range as metadata; the current renderer does not use that range. The sign of `BSCALE` selects whether stored codes
are uploaded in forward or reverse order. `BSCALE` must be finite and non-zero, and `BZERO` must be finite.

`BUNIT` describes the physical value represented by the scaled FITS data. It may be omitted for a dimensionless
scalar. For example, `log10(ne / m^-3)` is dimensionless; its meaning can be recorded in `BTYPE` and `HISTORY` without
assigning the logarithm the density unit `m-3`. `BUNIT='1'` is not valid FITS unit syntax.

## Undefined voxels

`BLANK` is available for both supported integer formats, including `BITPIX=8`. It identifies one stored integer as
undefined. FITS and JHV interpret that code before applying `BSCALE` and `BZERO`.

Use `BLANK` where no scientific sample exists—for example outside the model domain, where source data is missing, or
where interpolation did not produce a finite result. A low but valid value is still data and should remain a defined
sample, even if the chosen colour or opacity mapping makes it invisible.

The blank code must be within the stored range:

- 0 through 255 for `BITPIX=8`;
- -32768 through 32767 for `BITPIX=16`.

When a cube has no undefined voxels, omitting `BLANK` preserves every stored code for data and avoids a full-sized
mask; JHV uses a single all-valid texel instead. When undefined voxels are present, reserve a code during quantization
and write the same integer in the `BLANK` header card. A file in which every voxel is blank is rejected.

When `BLANK` is present, JHV constructs a separate `R8` GPU validity texture. Defined samples receive 255 and blank
samples receive 0. During trilinear sampling, the shader normalizes the filtered scalar by the filtered validity, so
an undefined neighbour does not simply darken a valid sample. Fractional validity still reduces opacity at the edge
of the defined domain.

The mask is derived entirely from the scalar image and its `BLANK` card; no separate mask HDU is part of this
interface.

## Affine WCS

JHV uses the standard linear FITS WCS to place voxel centres in the solar scene. The reference pixel coordinate
`CRPIX` maps to the world coordinate `CRVAL`. Moving by one pixel along a pixel axis adds the corresponding column of
the linear transform `M`.

For a one-based FITS pixel coordinate `p`, the mapping is:

```text
world = CRVAL + M (p - CRPIX)
```

Define `M` in one of the following ways:

- With only `CDELTi`, `M` is diagonal: `M_i,i = CDELTi`.
- With a PC matrix, omitted diagonal elements are 1 and all other omitted elements are 0. Then
  `M_i,j = CDELTi * PCi_j`.
- With a CD matrix, `M_i,j = CDi_j`. An omitted CD element is 0, and `CDELTi` is not used.

Rows of `M` are expressed in the units given by the corresponding `CUNITi`; columns correspond to FITS pixel axes.
A product uses either PC or CD, not both. The resulting matrix must be finite and nonsingular. It may represent
rotations, reflections, axis permutations, unequal scales, or linear shear.

For a Sun-centred axis with `NAXISi` voxels, place the reference at the centre of the axis:

```text
CRPIXi = (NAXISi + 1) / 2
CRVALi = 0
```

For example, a 256-voxel axis has `CRPIXi=128.5`. Other reference pixels and non-zero `CRVALi` values are valid and
can describe cropped or displaced volumes. This profile treats each FITS array value as a cell-centred voxel sample.
JHV therefore places the volume boundaries half a pixel step beyond the first and last sample centres. This aligns the
FITS voxel centres with the texel centres of the GLES 3D texture and makes the rendered parallelepiped enclose the
complete voxels.

## Lossless tiled compression

JHV reads the registered FITS tiled-image compression convention. `GZIP_2` is a useful lossless option for 16-bit
integer volumes because its byte shuffling often improves compression while preserving the stored integers exactly.

JHV's current `nom-tam-fits` reader can decode a compressed 3D image only when each tile is one voxel deep along FITS
axis 3. In other words, each tile contains part or all of one xy plane and never spans several planes. A 256-cubed
volume can use one complete xy plane per tile:

```fits
ZCMPTYPE= 'GZIP_2'
ZTILE1  =                  256
ZTILE2  =                  256
ZTILE3  =                    1
```

The physical representation is normally an empty primary HDU followed by a binary-table extension. FITS-aware
software exposes the table as the logical 3D image. Compression reduces storage and transfer size, but not the
decoded CPU or GPU footprint.

For generated products, it is useful to validate both the physical-HDU checksums and an exact decompression round
trip of the stored integers and logical image metadata.

## Complete FITS example

This header describes a 256-cubed, Sun-centred volume extending from -3 to +3 solar radii along each observer-aligned
axis. Stored codes 0 through 254 represent the dimensionless interval `[0, 1]`; 255 is reserved for `BLANK`.

```fits
SIMPLE  =                    T
BITPIX  =                    8
NAXIS   =                    3
NAXIS1  =                  256
NAXIS2  =                  256
NAXIS3  =                  256
OBJECT  = 'synthetic coronal density'
DATE-OBS= '2026-08-17T00:00:00.000'
TIMESYS = 'UTC'
BSCALE  =  0.003937007874016
BZERO   =                  0.0
BLANK   =                  255
WCSNAME = 'Heliocentric-cartesian'
CTYPE1  = 'SOLX'
CUNIT1  = 'solRad'
CRPIX1  =                128.5
CRVAL1  =                  0.0
CDELT1  =            0.0234375
CTYPE2  = 'SOLY'
CUNIT2  = 'solRad'
CRPIX2  =                128.5
CRVAL2  =                  0.0
CDELT2  =            0.0234375
CTYPE3  = 'SOLZ'
CUNIT3  = 'solRad'
CRPIX3  =                128.5
CRVAL3  =                  0.0
CDELT3  =            0.0234375
DSUN_OBS=       151470458469.0
CRLN_OBS=        168.635177770
CRLT_OBS=          6.722914954
RSUN_REF=          695700000.0
```

The first voxel centre is at -2.98828125 solar radii and the outer cell boundary is at -3. The corresponding last
cell boundary is at +3.

# glTF geometry-scene profile

glTF scenes are suitable for explicit geometry such as field lines, triangulated surfaces, or point samples. JHV
uses Assimp, the Open Asset Import Library, to read both `.gltf` and `.glb` files. Assimp reads the asset, while JHV
defines the supported feature set, interprets the solar metadata below, and places the geometry in the solar scene.

glTF defines many features beyond this profile. A file that works in a general glTF viewer may contain features that
JHV does not support. The following sections describe the subset producers can currently rely on.

## Solar metadata in scene extras

glTF does not define solar coordinates or an observation frame. This profile places the shared metadata in `extras`
on the default glTF scene:

```json
{
  "scene": 0,
  "scenes": [
    {
      "name": "coronal model",
      "extras": {
        "DATE-OBS": "2025-10-09T18:19:52.000",
        "DSUN_OBS": 149000000000.0,
        "CRLN_OBS": 123.4,
        "CRLT_OBS": 5.6,
        "RSUN_REF": 695700000.0,
        "CTYPE1": "SOLX",
        "CTYPE2": "SOLY",
        "CTYPE3": "SOLZ",
        "CUNIT1": "solRad",
        "CUNIT2": "solRad",
        "CUNIT3": "solRad",
        "WCSNAME": "Heliocentric-cartesian"
      }
    }
  ]
}
```

The three local position components correspond to `CTYPE1..3`, with the units in `CUNIT1..3`. All three Cartesian
components occur exactly once. Unlike a FITS array, a scene has no `CRPIX`, `CRVAL`, PC, or CD matrix: node transforms
and vertex positions already describe the local geometry.

JHV reads the metadata imported for the scene and incorporates the resulting solar placement into the root-node
transform. A general glTF viewer will ignore these application-specific extras and display the same geometry in its
local frame, as expected by the glTF standard.

## What JHV currently displays

JHV supports the following static scene content:

- triangle meshes, including open or closed surfaces;
- connected lines and polylines;
- point sets;
- a hierarchy of nodes with static translations, rotations, and scales, including repeated use of a mesh;
- a base colour and per-vertex RGBA colours on triangles, lines, and points;
- one base-colour texture per triangle material, embedded in the file or stored alongside a `.gltf` file;
- opaque, cut-out (`MASK`), and translucent (`BLEND`) materials; and
- single- or double-sided triangle surfaces.

All three primitive types use the same solar placement, scene rotation, depth buffer, and observation time as other
JHV layers. Colours in glTF use ordinary straight alpha; JHV performs the premultiplication required by its renderer.
Producers should therefore write normal, non-premultiplied glTF colours.

glTF does not define a portable visual width for a line or size for a point. JHV assigns its own fixed display width
and size, so a file cannot currently request thicker lines or larger markers. SunJSON remains the better choice when
JHV-specific line thickness or point size is part of the product.

## Current limits

JHV intentionally treats these files as scientific geometry rather than as fully lit 3D artwork. It uses base colour,
vertex colour, and a base-colour texture, but does not currently reproduce glTF lighting, metallic/roughness,
normal-map, or emissive effects. Cameras and lights stored in the scene do not control the JHV view.

Animations, skeletons, and morph targets are not supported. Textures on lines and points are not supported, nor are
additive blending, separate opacity textures, or transformed texture coordinates. Simple translucent surfaces work,
but complex overlapping translucent geometry may depend on draw order; splitting it into sensible objects generally
gives a more predictable result. Opaque or normally blended lines are preferable to cut-out (`MASK`) lines, whose
edge masking is only approximate in the current line renderer.

JHV also does not interpret arbitrary scientific vertex attributes as interactive data channels. A producer should
convert the chosen quantity to display-ready vertex colours or a base-colour texture and record the quantity, units,
and colour mapping in scene `extras`. Volumes remain separate FITS cube products.

# Worked COCONUT conversion

`extra/test/create_coconut_samples.py` is a reproducible reference conversion from a COCONUT CFmesh solution. It
produces:

- `coconut-corona-density-16.fits`, a compressed scalar density volume;
- `coconut-corona-scene.glb`, a scene containing traced magnetic field lines, a triangulated heliospheric current
  sheet, and point markers at the inner and outer boundary endpoints of open field lines.

Qorona can export the same class of traced field lines directly as SunJSON, which is the simpler product when JHV is
the only consumer. This example uses GLB specifically to demonstrate the general scene interface and to keep the
geometry usable by non-JHV glTF viewers. It should not be read as an extra conversion required by Qorona.

Run it from the repository root in an environment containing Qorona, PyVista/VTK, Astropy, SunPy, and pygltflib:

```shell
python extra/test/create_coconut_samples.py \
    /path/to/coconut_corona.CFmesh.xz \
    --timestamp 2025-10-09T18:19:52 \
    --output-directory extra/test/data
```

The source CFmesh file does not itself establish every fact needed by the interchange products. The reference
conversion therefore makes the following choices explicit:

- it treats the model coordinates as Carrington-aligned Cartesian coordinates in solar radii;
- it calculates the Earth observer at the supplied solution time and rotates both products into that observer's
  `SOLX/SOLY/SOLZ` frame;
- it resamples the native cells with Qorona's moving-least-squares machinery onto a logarithmic spherical field grid,
  then samples a 256-cubed Cartesian output volume spanning -6 to +6 solar radii;
- it treats Qorona's density as a relative shape, assumes an additional `10^14 m^-3` normalization, stores
  `log10(ne / m^-3)`, and clips it to `[10.9, 14.0]`;
- it reserves `-32768` for blank voxels and writes the defined values with `BITPIX=16`, `BSCALE`, and `BZERO`;
- it compresses the FITS image losslessly with `GZIP_2` and one xy plane per tile;
- it traces field lines in float64 with DOPRI5, `rtol=10^-8`, and `cfl=0.125`, while glTF positions are stored as
  float32;
- it marks both boundary endpoints of every complete open field line as glTF points, using the same polarity colour as
  the corresponding line;
- it defines the heliospheric current sheet as the zero isosurface of the radial magnetic-field component on the same
  high-resolution spherical grid, closes the periodic longitude seam before contouring, and exports the resulting
  triangle mesh as a translucent, double-sided surface; and
- it colours that surface by radial velocity with the `turbo` colour map over a fixed `-30` to `300 km/s` display
  interval. The model's dimensionless velocity is converted using the COOLFluiD `corona` normalization
  `v0 = 480 km/s`. Values outside the display interval use its endpoint colours.

Those values document one high-quality reference product; another producing project may make different justified
choices. The important interface points are the final grid or geometry, shared frame metadata, scalar meaning,
undefined-domain treatment, and enough provenance to reproduce the conversion.

The FITS `HISTORY` cards and glTF scene `extras` record the source name and SHA-256 digest, software version,
resampling configuration, source-cell count, and consequential density, tracing, and current-sheet parameters.

For the GLB, PyVista creates the polyline, triangle, and point geometry through VTK's glTF exporter. The script obtains
VTK's glTF document in memory, adds the solar metadata to the default scene, and uses pygltflib to package it as one
GLB without an intermediate `.gltf` file. Field-line and boundary-point vertex colours are normalized unsigned-byte,
opaque RGBA values. Current-sheet vertices likewise carry normalized unsigned-byte RGBA values, with RGB encoding
radial velocity and a common straight alpha. All base materials are white so they do not tint those vertex colours.
JHV converts the glTF colours to premultiplied alpha while rendering.

# Validation shared by both projects

Validation is most useful when it is divided between format production and integration rendering.

## Checks performed while producing a FITS volume

- Validate the physical FITS HDUs and any `CHECKSUM`/`DATASUM` cards.
- Reopen tiled-compressed images through the logical image interface and compare the stored integers exactly.
- Check the first, reference, and last voxel-centre coordinates, plus the eight half-voxel outer corners.
- Use an asymmetric test pattern to expose axis permutations and reflections.
- Check representative `BLANK` regions and recovered physical values.
- Use Astropy to check the linear WCS coordinates and SunPy to calculate the expected observer metadata independently.

The reference converter checks the physical-HDU checksums, compares the decompressed stored integers exactly, and
checks the recovered logical-image metadata.

## Checks performed while producing a glTF scene

- Reopen the completed `.gltf` or `.glb` product rather than validating only the exporter input.
- Confirm the default-scene extras, primitive modes, accessor counts and types, indices, colours, materials, and
  embedded binary payload.
- Confirm that coordinates remain finite after float32 conversion and that line conversion emits no degenerate
  adjacent segments.
- For an isosurface, confirm that the result is non-empty, triangulated, periodic where required, and uses the intended
  transparency and sidedness.
- Confirm that point samples remain point primitives and retain their per-vertex colours.
- For boundary markers, confirm that every point lies on the intended boundary and that its colour encodes the
  intended classification.

The reference converter checks the semantic inputs before export, then reopens the completed GLB and verifies its
metadata, primitive structure, accessors, materials, and binary packaging.

## Integration checks in JHV

- Load matching FITS and glTF products together and verify their relative placement.
- Inspect several viewpoints, including occultation by the solar sphere and intersections with other depth-writing
  layers.
- Exercise non-zero observer longitude and latitude; a nominal zero observer cannot reveal placement mistakes.
- For FITS, inspect mask boundaries and colour/opacity transfer at representative values.
- For geometry, inspect winding, transparency, lines, points, and scene rotation as applicable.

Repository tests provide additional synthetic coverage of volume WCS and masks, scene placement and materials,
primitive rendering, depth behaviour, rendering-state restoration, and generation of the paired reference products.

# Current extension points

This interface is intended to grow with the needs of producing projects. Possible future additions include more
scalar channels, time-sequenced products, vector quantities, alternative regular-grid representations, and richer
glTF materials. When a new need arises, the producing and JHV teams can define the extension together, supported by
a representative product and matching JHV integration tests. This keeps the document practical and ensures that new
capabilities work from production through visualization.

# External references

- [FITS Standard 4.0](https://fits.gsfc.nasa.gov/fits_standard.html)
- W. T. Thompson, [Coordinate systems for solar image data](https://doi.org/10.1051/0004-6361:20054262),
  *Astronomy & Astrophysics* 449 (2006), 791-803
- [FITS tiled-image compression convention](https://fits.gsfc.nasa.gov/registry/tilecompression.html)
- [SunPy coordinates and WCS](https://docs.sunpy.org/en/stable/topic_guide/coordinates/wcs.html)
- Qorona documentation: [Export to JHelioviewer](https://rayandhib.github.io/Qorona/jhelioviewer/),
  [squashing-factor render](https://rayandhib.github.io/Qorona/products/squashing-factor/),
  [white-light imaging](https://rayandhib.github.io/Qorona/products/white-light/), and
  [field lines](https://rayandhib.github.io/Qorona/products/fieldlines/)
- J. H. Guo et al., [Modeling the propagation of coronal mass ejections with COCONUT: Implementation of the
  regularized Biot-Savart law flux rope model](https://doi.org/10.1051/0004-6361/202347634), *Astronomy &
  Astrophysics* 683 (2024), A54; its COCONUT visualization depicts the heliospheric current sheet as the
  `B_r=0` isosurface
- [Khronos glTF Registry and current specification](https://registry.khronos.org/glTF/)
- [Open Asset Import Library (Assimp)](https://www.assimp.org/)
- [VTK `vtkGLTFExporter`](https://vtk.org/doc/nightly/html/classvtkGLTFExporter.html)
- [pygltflib](https://github.com/avaturn/pygltflib)
