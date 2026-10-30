package org.helioviewer.jhv.metadata;

import java.util.regex.Pattern;

import org.helioviewer.jhv.wcs.WcsSolution;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;

// Adapt Helioviewer metadata conventions; WCSLIB owns FITS WCS interpretation.
final class WcsInterpreter {
    private static final Pattern WCS_KEY = Pattern.compile(
            "LONPOLE|LATPOLE|(?:CRPIX|CRVAL|CDELT|CROTA)[12]|(?:PC|CD)[12]_[12]|PV[12]_\\d+");

    static WcsSolution read(MetaDataContainer metadata, int width, int height, double observerUnitsPerRad) {
        try {
            Header header = new Header();
            // JHV displays a two-dimensional image. SPICE and PUNCH JP2 headers
            // can retain WCSAXES and non-spatial axes from their original FITS cube.
            header.addValue("NAXIS", 2, null);
            for (String key : metadata.keys()) {
                if (WCS_KEY.matcher(key).matches())
                    header.addValue(key, metadata.getRequiredDouble(key), null);
            }
            // Without recorded pixel scales, a FITS default of one angular unit
            // per pixel would invent solar geometry instead of using BasicMetaData.
            boolean useCd = hasMatrix(header, "CD") && !hasMatrix(header, "PC");
            if (!useCd && (!header.containsKey("CDELT1") || !header.containsKey("CDELT2")))
                throw new IllegalArgumentException("Image WCS requires CDELT1 and CDELT2 or a CD matrix");
            String ctype1 = metadata.getString("CTYPE1").orElse("HPLN-TAN").strip().toUpperCase();
            String ctype2 = metadata.getString("CTYPE2").orElse("HPLT-TAN").strip().toUpperCase();
            // Known legacy solar image axes, without a FITS projection suffix.
            // Early LASCO images use zero placeholders for both labels and units.
            if ((ctype1.replace('_', '-').equals("SOLAR-X") && ctype2.replace('_', '-').equals("SOLAR-Y")) ||
                    (ctype1.equals("GONGX") && ctype2.equals("GONGY")) ||
                    (ctype1.equals("ARCSEC") && ctype2.equals("ARCSEC")) ||
                    (ctype1.equals("0") && ctype2.equals("0"))) {
                ctype1 = "HPLN-TAN";
                ctype2 = "HPLT-TAN";
            }
            boolean surface = ctype1.endsWith("CAR") || ctype1.endsWith("CEA");
            boolean legacyCeaLatitude = ctype1.endsWith("CEA") && metadata.getString("CUNIT2")
                    .map(unit -> unit.strip().equalsIgnoreCase("Sine Latitude")).orElse(true);
            header.addValue("CTYPE1", ctype1, null);
            header.addValue("CTYPE2", ctype2, null);
            for (int axis = 1; axis <= 2; axis++) {
                String unit = metadata.getString("CUNIT" + axis).orElse(surface ? "deg" : "arcsec").strip().toLowerCase();
                if (unit.equals("0"))
                    unit = "arcsec";
                else if (unit.equals("degree") || unit.equals("degrees"))
                    unit = "deg";
                if (axis == 2 && legacyCeaLatitude)
                    unit = "deg";
                header.addValue("CUNIT" + axis, unit, null);
            }
            if (!header.containsKey("CRPIX1"))
                header.addValue("CRPIX1", (width + 1) / 2., null);
            if (!header.containsKey("CRPIX2"))
                header.addValue("CRPIX2", (height + 1) / 2., null);

            if (legacyCeaLatitude)
                convertLegacyCeaLatitude(header, useCd);

            // CROTA2 is the standard latitude-axis rotation. Preserve the observer
            // images' historical fallbacks only when that keyword is absent.
            if (!surface && !header.containsKey("CROTA2")) {
                metadata.getDouble("CROTA").or(() -> metadata.getDouble("CROTA1"))
                        .ifPresent(rotation -> header.addValue("CROTA2", rotation, null));
            }
            return new WcsSolution(header, surface ? 1 : observerUnitsPerRad);
        } catch (HeaderCardException e) {
            throw new IllegalArgumentException("Invalid WCS metadata", e);
        }
    }

    // Historical CEA maps with missing CUNIT2 or "Sine Latitude" store the plane's normalized Y row.
    // CRVAL2 remains a celestial latitude in degrees.
    private static void convertLegacyCeaLatitude(Header header, boolean useCd) throws HeaderCardException {
        if (useCd) {
            for (String key : new String[]{"CD2_1", "CD2_2"})
                if (header.containsKey(key))
                    header.addValue(key, Math.toDegrees(header.getDoubleValue(key)), null);
        } else {
            header.addValue("CDELT2", Math.toDegrees(header.getDoubleValue("CDELT2")), null);
        }
    }

    private static boolean hasMatrix(Header header, String prefix) {
        return header.containsKey(prefix + "1_1") || header.containsKey(prefix + "1_2") ||
                header.containsKey(prefix + "2_1") || header.containsKey(prefix + "2_2");
    }

    private WcsInterpreter() {}
}
