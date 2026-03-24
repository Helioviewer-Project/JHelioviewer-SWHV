package org.helioviewer.jhv.plugins.swek.sources;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventParameter;
import org.helioviewer.jhv.events.JHVPositionInformation;
import org.helioviewer.jhv.events.SWEK;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class HEKParser {

    private static final ThreadLocal<DecimalFormat> formatter1 = ThreadLocal.withInitial(() -> MathUtils.numberFormatter("0", 1));

    private record HgsPoint(double longitudeDeg, double latitudeDeg) {
    }

    static void parseResult(JSONObject result, JHVEvent currentEvent, boolean full) throws JSONException {
        List<HgsPoint> hgsBoundedBox = null;
        List<HgsPoint> hgsBoundCC = null;
        HgsPoint hgsCentralPoint = null;
        Double hgsLongitudeDeg = null;
        Double hgsLatitudeDeg = null;

        boolean waveCM = false;
        String waveValue = null;

        // First iterate over parameters in the config file
        List<SWEK.Parameter> plist = currentEvent.getSupplier().getGroup().getParameterList();
        Iterator<SWEK.Parameter> paramIterator = plist.iterator();
        HashSet<String> insertedKeys = new HashSet<>();

        Iterator<String> keys = result.keys();
        while (paramIterator.hasNext() || keys.hasNext()) {
            String key = paramIterator.hasNext() ? paramIterator.next().name() : keys.next();
            String lowKey = key.toLowerCase();
            if (insertedKeys.contains(lowKey))
                continue;
            insertedKeys.add(lowKey);

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
            } catch (Exception ignore) {
            }
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
        String url = "", type = "";
        boolean ok = false;

        Iterator<String> keys = ref.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = ref.getString(key);

            String lowerKey = key.toLowerCase();
            if (lowerKey.equals("ref_type")) {
                String lvalue = value.toLowerCase();
                switch (lvalue) {
                    case "movie" -> {
                        type = "Reference Movie";
                        ok = true;
                    }
                    case "image" -> {
                        type = "Reference Image";
                        ok = true;
                    }
                    case "html" -> {
                        type = "Reference Link";
                        ok = true;
                    }
                    default -> {
                    }
                }
            } else if (lowerKey.equals("ref_url")) {
                url = value;
            }
        }
        if (ok) {
            currentEvent.addParameter(new JHVEventParameter(type, type, url), true, true, true);
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

            try (Scanner s = new Scanner(coordinates)) {
                s.useDelimiter(",");
                while (s.hasNext()) {
                    HgsPoint tempPoint = parseCoordinates(s.next());
                    if (tempPoint != null) {
                        polygonPoints.add(tempPoint);
                    }
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
        Double longitudeDeg = null;
        Double latitudeDeg = null;

        try (Scanner s = new Scanner(value)) {
            s.useDelimiter(" ");
            if (s.hasNextDouble())
                longitudeDeg = s.nextDouble();
            if (s.hasNextDouble())
                latitudeDeg = s.nextDouble();
        }

        return longitudeDeg != null && latitudeDeg != null ? new HgsPoint(longitudeDeg, latitudeDeg) : null;
    }

    @Nullable
    private static List<HgsPoint> checkAndFixBoundingBox(List<HgsPoint> hgsBoundedBox) {
        if (hgsBoundedBox != null) {
            double minX = 0.0;
            double minY = 0.0;
            double maxX = 0.0;
            double maxY = 0.0;
            boolean first = true;
            for (HgsPoint p : hgsBoundedBox) {
                if (first) {
                    minX = p.longitudeDeg();
                    maxX = p.longitudeDeg();
                    minY = p.latitudeDeg();
                    maxY = p.latitudeDeg();
                    first = false;
                } else {
                    minX = Math.min(minX, p.longitudeDeg());
                    maxX = Math.max(maxX, p.longitudeDeg());
                    minY = Math.min(minY, p.latitudeDeg());
                    maxY = Math.max(maxY, p.latitudeDeg());
                }
            }

            if ((maxX - minX) > 160 && (maxY - minY) > 160) {
                return null;
            }
        }
        return hgsBoundedBox;
    }

    private static void handleHGSCoordinates(JHVEvent currentEvent, List<HgsPoint> hgsBoundedBox, List<HgsPoint> hgsBoundCC, HgsPoint hgsCentralPoint, Double hgsLongitudeDeg, Double hgsLatitudeDeg) {
        if (hgsBoundedBox == null && hgsCentralPoint == null && (hgsLongitudeDeg == null || hgsLatitudeDeg == null) && hgsBoundCC == null)
            return;

        List<HgsPoint> boundedBox = hgsBoundedBox == null ? List.of() : hgsBoundedBox;
        List<HgsPoint> boundCC = hgsBoundCC == null ? List.of() : hgsBoundCC;
        HgsPoint centralPoint = hgsCentralPoint != null ? hgsCentralPoint
                : hgsLongitudeDeg != null && hgsLatitudeDeg != null ? new HgsPoint(hgsLongitudeDeg, hgsLatitudeDeg)
                : null;

        Position p = Sun.getEarth(new JHVTime(currentEvent.start));
        double elon = p.lon;

        List<Vec3> jhvBoundedBox = new ArrayList<>(boundedBox.size());
        boundedBox.forEach(point -> jhvBoundedBox.add(hgsToJhv(point, elon)));

        List<Vec3> jhvBoundCC = new ArrayList<>(boundCC.size());
        boundCC.forEach(point -> jhvBoundCC.add(hgsToJhv(point, elon)));

        Vec3 jhvCentralPoint = centralPoint != null ? hgsToJhv(centralPoint, elon) : null;
        currentEvent.addPositionInformation(new JHVPositionInformation(jhvCentralPoint, jhvBoundedBox, jhvBoundCC,
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
