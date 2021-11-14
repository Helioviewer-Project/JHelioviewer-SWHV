package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventParameter;
import org.helioviewer.jhv.events.JHVPositionInformation;
import org.helioviewer.jhv.events.SWEKParameter;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class HEKParser {

    private static final ThreadLocal<DecimalFormat> formatter1 = ThreadLocal.withInitial(() -> MathUtils.numberFormatter("0", 1));

    static void parseResult(JSONObject result, JHVEvent currentEvent, boolean full) throws JSONException {
        List<Vec3> hgsBoundedBox = null;
        List<Vec3> hgsBoundCC = null;
        Vec3 hgsCentralPoint = null;
        Double hgsX = null;
        Double hgsY = null;

        boolean waveCM = false;
        String waveValue = null;

        // First iterate over parameters in the config file
        List<SWEKParameter> plist = currentEvent.getSupplier().getGroup().getParameterList();
        Iterator<SWEKParameter> paramIterator = plist.iterator();
        HashSet<String> insertedKeys = new HashSet<>();

        Iterator<String> keys = result.keys();
        while (paramIterator.hasNext() || keys.hasNext()) {
            String key = paramIterator.hasNext() ? paramIterator.next().name() : keys.next();
            String lowKey = key.toLowerCase(Locale.ENGLISH);
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
                    hgsX = Double.valueOf(value);
                } else if (lowKey.equals("hgs_y")) {
                    hgsY = Double.valueOf(value);
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

        handleCoordinates(currentEvent, hgsBoundedBox, hgsBoundCC, hgsCentralPoint, hgsX, hgsY);
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

            String lowerKey = key.toLowerCase(Locale.ENGLISH);
            if (lowerKey.equals("ref_type")) {
                String lvalue = value.toLowerCase(Locale.ENGLISH);
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
     * @return a list of JHV points
     */
    private static List<Vec3> parsePolygon(String value) {
        List<Vec3> polygonPoints = new ArrayList<>();
        if (value.toLowerCase(Locale.ENGLISH).contains("polygon")) {
            String coordinatesString = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')'));
            String coordinates = coordinatesString.substring(coordinatesString.indexOf('(') + 1, coordinatesString.lastIndexOf(')'));

            try (Scanner s = new Scanner(coordinates)) {
                s.useDelimiter(",");
                while (s.hasNext()) {
                    Vec3 tempPoint = parseCoordinates(s.next());
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
     * @return The Vec3 or null if it could not be parsed.
     */
    @Nullable
    private static Vec3 parsePoint(String value) {
        if (value.toLowerCase(Locale.ENGLISH).contains("point")) {
            return parseCoordinates(value.substring(value.indexOf('(') + 1, value.indexOf(')')));
        }
        return null;
    }

    /**
     * Parses a string of the format "0.716676950817756 73.6104596659652" to a
     * GL3DVec3
     *
     * @param value the string to parse
     * @return the Vec3 or null of it could not be parsed
     */
    @Nullable
    private static Vec3 parseCoordinates(String value) {
        double[] coordinate = {0, 0, 0};
        boolean notnull = false;

        try (Scanner s = new Scanner(value)) {
            s.useDelimiter(" ");
            for (int i = 0; i < 3; i++) {
                if (s.hasNext()) {
                    coordinate[i] = Double.parseDouble(s.next());
                    notnull = true;
                }
            }
        }

        if (notnull) {
            return new Vec3(coordinate[0], coordinate[1], coordinate[2]);
        }
        return null;
    }

    private static void handleCoordinates(JHVEvent currentEvent, List<Vec3> hgsBoundedBox, List<Vec3> hgsBoundCC, Vec3 hgsCentralPoint, Double hgsX, Double hgsY) {
        hgsBoundedBox = checkAndFixBoundingBox(hgsBoundedBox);
        handleHGSCoordinates(currentEvent, hgsBoundedBox, hgsBoundCC, hgsCentralPoint, hgsX, hgsY);
    }

    @Nullable
    private static List<Vec3> checkAndFixBoundingBox(List<Vec3> hgsBoundedBox) {
        if (hgsBoundedBox != null) {
            double minX = 0.0;
            double minY = 0.0;
            double maxX = 0.0;
            double maxY = 0.0;
            boolean first = true;
            for (Vec3 p : hgsBoundedBox) {
                if (first) {
                    minX = p.x;
                    maxX = p.x;
                    minY = p.y;
                    maxY = p.y;
                    first = false;
                } else {
                    minX = Math.min(minX, p.x);
                    maxX = Math.max(maxX, p.x);
                    minY = Math.min(minY, p.y);
                    maxY = Math.max(maxY, p.y);
                }
            }

            if ((maxX - minX) > 160 && (maxY - minY) > 160) {
                return null;
            }
        }
        return hgsBoundedBox;
    }

    private static void handleHGSCoordinates(JHVEvent currentEvent, List<Vec3> hgsBoundedBox, List<Vec3> hgsBoundCC, Vec3 hgsCentralPoint, Double hgsX, Double hgsY) {
        if (hgsBoundedBox != null || hgsCentralPoint != null || (hgsX != null && hgsY != null) || hgsBoundCC != null) {
            List<Vec3> localHGSBoundedBox = hgsBoundedBox == null ? new ArrayList<>() : hgsBoundedBox;
            List<Vec3> localHGSBoundCC = hgsBoundCC == null ? new ArrayList<>() : hgsBoundCC;

            Vec3 localHGSCentralPoint = null;
            if (hgsCentralPoint != null) {
                localHGSCentralPoint = hgsCentralPoint;
            } else {
                if (hgsX != null && hgsY != null) {
                    localHGSCentralPoint = new Vec3(hgsX, hgsY, 0);
                }
            }

            Position p = Sun.getEarth(new JHVTime(currentEvent.start));
            double elon = p.lon;

            ArrayList<Vec3> jhvBoundedBox = new ArrayList<>(localHGSBoundedBox.size());
            localHGSBoundedBox.forEach(el -> jhvBoundedBox.add(convertHGSJHV(el, elon)));

            ArrayList<Vec3> jhvBoundCC = new ArrayList<>(localHGSBoundCC.size());
            localHGSBoundCC.forEach(el -> jhvBoundCC.add(convertHGSJHV(el, elon)));

            Vec3 jhvCentralPoint = null;
            if (localHGSCentralPoint != null) {
                jhvCentralPoint = convertHGSJHV(localHGSCentralPoint, elon);
            }

            currentEvent.addPositionInformation(new JHVPositionInformation(jhvCentralPoint, jhvBoundedBox, jhvBoundCC,
                    currentEvent.isCactus() ? p : null)); // reduce memory usage
        }
    }

    private static Vec3 convertHGSJHV(Vec3 el, double elon) {
        double theta = Math.PI / 180 * el.y;
        double phi = Math.PI / 180 * el.x - elon;

        double x = Math.cos(theta) * Math.sin(phi);
        double z = Math.cos(theta) * Math.cos(phi);
        double y = -Math.sin(theta);
        return new Vec3(x, y, z);
    }

}
