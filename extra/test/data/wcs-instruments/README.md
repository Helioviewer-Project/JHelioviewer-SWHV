# Instrument metadata regression fixtures

`cases.json` records each source, original dimensions, FITS reference pixel,
celestial reference and expected angular matrix. The eleven server XML files
were captured on 2026-10-17 and are retained without editing. They represent
EIT, MDI, TRACE, XRT, SXT, GONG, RHESSI, SPICE, PUNCH, Kanzelhoehe and early LASCO.

All server fixtures also run through `FitsMetaData`. `gong-no-scale.xml` is a
keyword/value XML export of SunPy's `sunpy/data/test/gong_halpha.header`.
The GONG header without CDELT/CD must retain the loader's fallback
for uncalibrated images, rather than acquiring an invented one-arcsecond scale.

Expected matrices are calculated from the recorded CDELT/PC/CD/CROTA values and
unit conversion, independently of WCSLIB. Forward and inverse checks construct
celestial vectors from the tangent plane (TAN) or equidistant plane (ARC) and an
orthonormal frame at CRVAL.

The wider audit compared 344 actual server headers from ROB, IAS and GSFC,
including current channels and historical samples per instrument/detector.
All 344 pass the actual `FitsMetaData` path. Reference pixels and effective PV
coefficients match the old adapter. Angular matrices match apart from LASCO's
CROTA2 precedence, which its existing derotation removes from displayed geometry.
An additional 198 local FITS/header samples cover Metis, WISPR, CCOR2, surface
maps, Euhforia and other solar products. These include non-image tables and
headers without JHV-compatible observation dates; adapter acceptance alone is
not a claim of full image-loader support.

One historical IAS IRIS 2796 request returned HTTP 500; current IAS and GSFC
samples of that channel were captured. Support for Cartesian SOLIS and raw
MDI/HMI synoptic formats is outside this patch's scope. Unsupported projection/unit
combinations remain errors rather than falling back to angular TAN.
