package org.helioviewer.jhv.metadata;

import java.io.IOException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

public final class HeliocentricCartesianMetaData {

    public interface Source {
        boolean contains(String key);
        String string(String key) throws IOException;
        double number(String key) throws IOException;
        IOException error(String message);
    }

    public static final class CartesianAxes {
        private final int[] components;
        private final double[] scales;

        private CartesianAxes(int[] _components, double[] _scales) {
            components = _components;
            scales = _scales;
        }

        public int sourceAxis(int component) {
            for (int axis = 0; axis < 3; axis++) {
                if (components[axis] == component)
                    return axis;
            }
            throw new AssertionError();
        }

        public Vec3 vector(int axis) {
            double scale = scales[axis];
            return switch (components[axis]) {
                case 0 -> new Vec3(scale, 0, 0);
                case 1 -> new Vec3(0, scale, 0);
                case 2 -> new Vec3(0, 0, scale);
                default -> throw new AssertionError();
            };
        }

        public double scale(int axis) {
            return scales[axis];
        }
    }

    public static @Nullable JHVTime observationTime(Source source) throws IOException {
        for (String key : new String[]{"DATE-AVG", "DATE_AVG", "DATE_OBS", "DATE-OBS"}) {
            if (!source.contains(key))
                continue;
            String value = source.string(key).strip();
            if (value.endsWith("Z"))
                value = value.substring(0, value.length() - 1);
            if (value.length() == 10)
                value += "T00:00:00";
            try {
                return new JHVTime(value);
            } catch (RuntimeException e) {
                throw source.error("invalid " + key + ": " + value);
            }
        }
        return null;
    }

    public static JHVTime requiredObservationTime(Source source) throws IOException {
        JHVTime time = observationTime(source);
        if (time == null)
            throw source.error("heliocentric Cartesian coordinates require DATE-OBS or DATE-AVG");
        return time;
    }

    public static boolean hasCartesianAxes(Source source) {
        for (int axis = 1; axis <= 3; axis++) {
            if (source.contains("CTYPE" + axis) || source.contains("CUNIT" + axis))
                return true;
        }
        return false;
    }

    public static CartesianAxes cartesianAxes(Source source) throws IOException {
        int[] components = new int[3];
        double[] scales = new double[3];
        boolean[] present = new boolean[3];
        for (int axis = 0; axis < 3; axis++) {
            int component = switch (source.string("CTYPE" + (axis + 1)).strip()) {
                case "SOLX" -> 0;
                case "SOLY" -> 1;
                case "SOLZ" -> 2;
                default -> throw source.error("CTYPE1, CTYPE2, and CTYPE3 must contain SOLX, SOLY, and SOLZ");
            };
            if (present[component])
                throw source.error("CTYPE1, CTYPE2, and CTYPE3 must contain SOLX, SOLY, and SOLZ exactly once");
            present[component] = true;
            components[axis] = component;
            scales[axis] = unitScale(source, "CUNIT" + (axis + 1));
        }
        return new CartesianAxes(components, scales);
    }

    public static Quat observerRotation(Source source, @Nullable JHVTime time) throws IOException {
        double longitude;
        double latitude;
        // JHV's view rotation is the inverse of the observer's physical heliographic longitude.
        if (source.contains("HGLN_OBS") && source.contains("HGLT_OBS")) {
            if (time == null)
                throw source.error("HGLN_OBS/HGLT_OBS require DATE-OBS or DATE-AVG");
            longitude = Sun.getEarth(time).lon - Math.toRadians(source.number("HGLN_OBS"));
            latitude = Math.toRadians(source.number("HGLT_OBS"));
        } else if (source.contains("CRLN_OBS") && source.contains("CRLT_OBS")) {
            longitude = -Math.toRadians(source.number("CRLN_OBS"));
            latitude = Math.toRadians(source.number("CRLT_OBS"));
        } else {
            throw source.error("heliocentric Cartesian coordinates require HGLN_OBS/HGLT_OBS or CRLN_OBS/CRLT_OBS");
        }
        if (Math.abs(latitude) > Math.PI / 2)
            throw source.error("observer latitude must be between -90 and 90 degrees");
        return Quat.createXY(latitude, longitude);
    }

    private static double unitScale(Source source, String key) throws IOException {
        return switch (source.string(key).strip()) {
            case "solRad" -> 1;
            case "m" -> 1 / Sun.RadiusMeter;
            case "km" -> 1_000 / Sun.RadiusMeter;
            case "Mm" -> 1_000_000 / Sun.RadiusMeter;
            default -> throw source.error(key + " must be solRad, m, km, or Mm");
        };
    }

    private HeliocentricCartesianMetaData() {}
}
