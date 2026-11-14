package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.time.TimeUtils;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;

// Helioviewer LASCO JP2: jp2gen turns the raw image north-up about CRPIX taken as 0-based
// subscripts and leaves the SolarSoft distortion uncorrected.
final class LascoWcs {
    // Older Helioviewer frames follow other, inconsistent conventions and keep the plain reading.
    private static final long CUTOFF = TimeUtils.parseDate("2011-05-05");
    private static final double MM_PER_PIXEL = 0.021;
    private static final double ARCSEC = Math.PI / (180 * 3600);

    // SolarSoft c2/c3_distortion coefficients, occltr_cntr centers (0-based raw pixels),
    // and the measured plate scale factor
    private enum Detector {
        C2(0.0051344125, -0.00012233862, 1.0978595e-7, 512.634, 505.293, 1),
        C3(-0.0151657, 0.000165455, 0, 516.456, 531.132, 1.002);

        final double c0, c1, c2, occulterX, occulterY, scale;

        Detector(double _c0, double _c1, double _c2, double _occulterX, double _occulterY, double _scale) {
            c0 = _c0;
            c1 = _c1;
            c2 = _c2;
            occulterX = _occulterX;
            occulterY = _occulterY;
            scale = _scale;
        }
    }

    static void adapt(MetaDataContainer metadata, Header header, String detectorName, long observationMilli) {
        try {
            header.addValue("CROTA2", 0., null);
            Detector detector = switch (detectorName) {
                case "C2" -> Detector.C2;
                case "C3" -> Detector.C3;
                default -> null;
            };
            if (detector != null && observationMilli >= CUTOFF)
                correct(metadata, header, detector);
        } catch (HeaderCardException e) {
            throw new IllegalArgumentException("Invalid LASCO WCS metadata", e);
        }
    }

    private static void correct(MetaDataContainer metadata, Header header, Detector detector) throws HeaderCardException {
        double crpix1 = header.getDoubleValue("CRPIX1");
        double crpix2 = header.getDoubleValue("CRPIX2");
        double cdelt = header.getDoubleValue("CDELT1") * detector.scale;
        double angle = Math.toRadians(metadata.getRequiredDouble("CROTA"));
        double cos = Math.cos(angle), sin = Math.sin(angle);
        // jp2gen's turn about the pivot (CRPIX1, CRPIX2), IDL 0-based coordinates
        double sunX = crpix1 + sin - cos, sunY = crpix2 - sin - cos;
        double dx = detector.occulterX - crpix1, dy = detector.occulterY - crpix2;
        double occulterX = crpix1 + cos * dx - sin * dy, occulterY = crpix2 + sin * dx + cos * dy;

        // raw radius = A tan + B tan^3 + C tan^5, expanded to the fifth power of the angle
        double k = MM_PER_PIXEL / (cdelt * ARCSEC);
        double a = 1 + detector.c0, b = detector.c1 * k * k, c = detector.c2 * k * k * k * k;
        header.addValue("CTYPE1", "HPLN-ZPN", null);
        header.addValue("CTYPE2", "HPLT-ZPN", null);
        header.addValue("CRPIX1", occulterX + 1, null);
        header.addValue("CRPIX2", occulterY + 1, null);
        header.addValue("CRVAL1", (occulterX - sunX) * cdelt / a, null);
        header.addValue("CRVAL2", (occulterY - sunY) * cdelt / a, null);
        header.addValue("CDELT1", cdelt, null);
        header.addValue("CDELT2", cdelt, null);
        header.addValue("PV2_1", a, null);
        header.addValue("PV2_3", a / 3 + b, null);
        header.addValue("PV2_5", 2 * a / 15 + b + c, null);
    }

    private LascoWcs() {}
}
