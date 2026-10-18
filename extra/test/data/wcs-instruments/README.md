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

Unsupported projection/unit combinations remain errors rather than falling back to angular TAN.
