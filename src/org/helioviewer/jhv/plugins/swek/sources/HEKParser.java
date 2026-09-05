package org.helioviewer.jhv.plugins.swek.sources;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.JHVPositionInformation;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class HEKParser {

    private static final ThreadLocal<DecimalFormat> formatter1 = ThreadLocal.withInitial(() -> MathUtils.numberFormatter("0", 1));

    private record HgsPoint(double longitudeDeg, double latitudeDeg) {}

    static void parseResult(JSONObject result, JHVEvent currentEvent, boolean full) throws JSONException {
        List<HgsPoint> hgsBoundedBox = null;
        List<HgsPoint> hgsBoundCC = null;
        HgsPoint hgsCentralPoint = null;
        Double hgsLongitudeDeg = null;
        Double hgsLatitudeDeg = null;

        boolean waveCM = false;
        String waveValue = null;

        // First iterate over parameters in the config file
        List<SWEK.Parameter> plist = currentEvent.getSupplier().getParameterList();
        Iterator<SWEK.Parameter> paramIterator = plist.iterator();
        HashSet<String> insertedKeys = new HashSet<>();

        Iterator<String> keys = result.keys();
        while (paramIterator.hasNext() || keys.hasNext()) {
            String key = paramIterator.hasNext() ? paramIterator.next().name() : keys.next();
            String lowKey = key.toLowerCase();
            if (!insertedKeys.add(lowKey))
                continue;

            if (result.isNull(lowKey))
                continue;

            if (lowKey.equals("refs")) {
                parseRefs(currentEvent, result.getJSONArray(key));
            } else {
                String value = result.optString(lowKey);
                if (lowKey.equals("hgs_bbox")) {
                    hgsBoundedBox = parsePolygon(value);
                } else if (lowKey.equals("hgs_boundcc")) {
                    hgsBoundCC = parsePolygon(value);
                } else if (lowKey.equals("hgs_coord")) {
                    hgsCentralPoint = parsePoint(value);
                } else if (lowKey.equals("hgs_x")) {
                    hgsLongitudeDeg = Double.valueOf(value);
                } else if (lowKey.equals("hgs_y")) {
                    hgsLatitudeDeg = Double.valueOf(value);
                } else if (lowKey.equals("rasterscan") || lowKey.equals("bound_chaincode") ||
                        lowKey.startsWith("hgc_") || lowKey.startsWith("hgs_") || lowKey.startsWith("hpc_") || lowKey.startsWith("hrc_")) {
                    // nothing, delete
                } else {
                    value = value.trim();
                    if (!value.isEmpty()) {
                        if (lowKey.equals("obs_wavelunit") && value.equals("cm"))
                            waveCM = true;

                        if (lowKey.equals("obs_meanwavel"))
                            waveValue = value;
                        else
                            currentEvent.addParameter(lowKey, value, full);
                    }
                }
            }
        }

        if (waveValue != null) {
            try {
                if (waveCM)
                    waveValue = formatter1.get().format(Double.parseDouble(waveValue) * (1e-2 /*m*/ * 1e9 /*nm*/)) + "nm";
            } catch (Exception ignore) {}
            currentEvent.addParameter("obs_meanwavel", waveValue, full);
        }

        handleHGSCoordinates(currentEvent, checkAndFixBoundingBox(hgsBoundedBox), hgsBoundCC, hgsCentralPoint, hgsLongitudeDeg, hgsLatitudeDeg);
    }

    private static void parseRefs(JHVEvent currentEvent, JSONArray refs) throws JSONException {
        int len = refs.length();
        for (int i = 0; i < len; i++) {
            parseRef(currentEvent, refs.getJSONObject(i));
        }
    }

    private static void parseRef(JHVEvent currentEvent, JSONObject ref) throws JSONException {
        String url = "", type = null;

        Iterator<String> keys = ref.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = ref.getString(key);

            String lowerKey = key.toLowerCase();
            if (lowerKey.equals("ref_type")) {
                String lvalue = value.toLowerCase();
                switch (lvalue) {
                    case "movie" -> type = "Reference Movie";
                    case "image" -> type = "Reference Image";
                    case "html" -> type = "Reference Link";
                    default -> {}
                }
            } else if (lowerKey.equals("ref_url")) {
                url = value;
            }
        }
        if (type != null) {
            currentEvent.addParameter(type, type, url, true, true);
        }
    }

    /**
     * Parse a string of the format
     * "POLYGON((0.745758 77.471192,0.667026 75.963757,...,0.691115 69.443955,0.767379 71.565051,0.745758 77.471192))"
     *
     * @param value the value to parse
     * @return a list of HGS longitude/latitude points
     */
    private static List<HgsPoint> parsePolygon(String value) {
        List<HgsPoint> polygonPoints = new ArrayList<>();
        if (containsIgnoreCase(value, "polygon")) {
            String coordinatesString = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')'));
            String coordinates = coordinatesString.substring(coordinatesString.indexOf('(') + 1, coordinatesString.lastIndexOf(')'));

            for (String coordinate : coordinates.split(",")) {
                HgsPoint tempPoint = parseCoordinates(coordinate);
                if (tempPoint != null) {
                    polygonPoints.add(tempPoint);
                }
            }
        }
        return polygonPoints;
    }

    /**
     * Parses a point of the format POINT(0.716676950817756 73.6104596659652).
     *
     * @param value the point to parse
     * @return the HGS longitude/latitude point or null if it could not be parsed
     */
    @Nullable
    private static HgsPoint parsePoint(String value) {
        if (containsIgnoreCase(value, "point")) {
            return parseCoordinates(value.substring(value.indexOf('(') + 1, value.indexOf(')')));
        }
        return null;
    }

    /**
     * Parses a string of the format "0.716676950817756 73.6104596659652" to an
     * HGS longitude/latitude point.
     *
     * @param value the string to parse
     * @return the HGS point or null if it could not be parsed
     */
    @Nullable
    private static HgsPoint parseCoordinates(String value) {
        String[] parts = Regex.MultiSpace.split(value.trim(), 3);
        if (parts.length < 2)
            return null;

        try {
            return new HgsPoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static List<HgsPoint> checkAndFixBoundingBox(List<HgsPoint> hgsBoundedBox) {
        if (hgsBoundedBox == null || hgsBoundedBox.isEmpty())
            return hgsBoundedBox;

        HgsPoint first = hgsBoundedBox.getFirst();
        double minX = first.longitudeDeg(), maxX = minX;
        double minY = first.latitudeDeg(), maxY = minY;

        for (int i = 1; i < hgsBoundedBox.size(); i++) {
            HgsPoint p = hgsBoundedBox.get(i);
            minX = Math.min(minX, p.longitudeDeg());
            maxX = Math.max(maxX, p.longitudeDeg());
            minY = Math.min(minY, p.latitudeDeg());
            maxY = Math.max(maxY, p.latitudeDeg());
        }

        if (maxX - minX > 160 && maxY - minY > 160)
            return null;
        return hgsBoundedBox;
    }

    private static void handleHGSCoordinates(JHVEvent currentEvent, List<HgsPoint> hgsBoundedBox, List<HgsPoint> hgsBoundCC, HgsPoint hgsCentralPoint, Double hgsLongitudeDeg, Double hgsLatitudeDeg) {
        if (hgsBoundedBox == null && hgsCentralPoint == null && (hgsLongitudeDeg == null || hgsLatitudeDeg == null) && hgsBoundCC == null)
            return;

        List<HgsPoint> boundary = hgsBoundCC != null && !hgsBoundCC.isEmpty() ? hgsBoundCC : hgsBoundedBox;
        if (boundary == null) boundary = List.of();
        HgsPoint centralPoint = hgsCentralPoint;
        if (centralPoint == null && hgsLongitudeDeg != null && hgsLatitudeDeg != null)
            centralPoint = new HgsPoint(hgsLongitudeDeg, hgsLatitudeDeg);

        Position p = Sun.getEarth(new JHVTime(currentEvent.start));
        double elon = p.lon;

        float[] jhvBoundary = new float[3 * boundary.size()];
        for (int i = 0; i < boundary.size(); i++) {
            Vec3 point = hgsToJhv(boundary.get(i), elon);
            jhvBoundary[3 * i] = (float) point.x;
            jhvBoundary[3 * i + 1] = (float) point.y;
            jhvBoundary[3 * i + 2] = (float) point.z;
        }

        Vec3 jhvCentralPoint = centralPoint != null ? hgsToJhv(centralPoint, elon) : null;
        currentEvent.addPositionInformation(new JHVPositionInformation(jhvCentralPoint, jhvBoundary,
                currentEvent.isCactus() ? p : null)); // reduce memory usage
    }

    private static Vec3 hgsToJhv(HgsPoint point, double elon) {
        return SphericalCoords.unit(Math.toRadians(point.longitudeDeg()) - elon, Math.toRadians(point.latitudeDeg()));
    }

    private static boolean containsIgnoreCase(String value, String token) {
        int limit = value.length() - token.length();
        for (int i = 0; i <= limit; i++) {
            if (value.regionMatches(true, i, token, 0, token.length()))
                return true;
        }
        return false;
    }

}
