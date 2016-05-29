package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parser able to parse events coming from the HEK server.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class HEKParser implements SWEKParser {

    @Override
    public JHVEvent parseEventJSON(JSONObject json, JHVEventType type, int id, long start, long end, boolean full) throws JSONException {
        JHVEvent currentEvent = new JHVEvent(type, id, start, end);

        currentEvent.initParams();
        parseResult(json, currentEvent, full);
        currentEvent.finishParams();

        return currentEvent;
    }

    private void parseResult(JSONObject result, JHVEvent currentEvent, boolean full) throws JSONException {
        List<Vec3> hgsBoundedBox = null;
        List<Vec3> hgsBoundCC = null;
        Vec3 hgsCentralPoint = null;
        Double hgsX = null;
        Double hgsY = null;

        Iterator<?> keys = result.keys();
        while (keys.hasNext()) {
            Object key = keys.next();
            if (key instanceof String) {
                String originalKeyString = (String) key;

                String keyString = originalKeyString.toLowerCase(Locale.ENGLISH);
                if (keyString.equals("refs")) {
                    parseRefs(currentEvent, result.getJSONArray(originalKeyString));
                } else {
                    String value = null;
                    if (!result.isNull(keyString)) {
                        value = result.optString(keyString);
                    } else {
                        continue;
                    }
                    if (keyString.equals("hgs_bbox")) {
                        hgsBoundedBox = parsePolygon(value);
                    } else if (keyString.equals("hgs_boundcc")) {
                        hgsBoundCC = parsePolygon(value);
                    } else if (keyString.equals("hgs_coord")) {
                        hgsCentralPoint = parsePoint(value);
                    } else if (keyString.equals("hgs_x")) {
                        hgsX = Double.valueOf(value);
                    } else if (keyString.equals("hgs_y")) {
                        hgsY = Double.valueOf(value);
                    } else if (keyString.equals("rasterscan") || keyString.equals("bound_chaincode") ||
                            keyString.startsWith("hgc_") || keyString.startsWith("hgs_") || keyString.startsWith("hpc_") || keyString.startsWith("hrc_")) {
                        // nothing, delete
                    } else {
                        value = value.trim();
                        if (value.length() != 0)
                            currentEvent.addParameter(originalKeyString, value, full);
                    }
                }
            }
        }
        handleCoordinates(currentEvent, hgsBoundedBox, hgsBoundCC, hgsCentralPoint, hgsX, hgsY);
    }

    private void parseRefs(JHVEvent currentEvent, JSONArray refs) throws JSONException {
        for (int i = 0; i < refs.length(); i++) {
            parseRef(currentEvent, refs.getJSONObject(i));
        }
    }

    private void parseRef(JHVEvent currentEvent, JSONObject ref) throws JSONException {
        Iterator<?> keys = ref.keys();
        String url = "", type = "";
        boolean ok = false;
        while (keys.hasNext()) {
            Object key = keys.next();
            if (key instanceof String) {
                String keyString = (String) key;
                String lkeyString = keyString.toLowerCase(Locale.ENGLISH);

                String value = ref.getString(keyString);
                if (lkeyString.equals("ref_type")) {
                    String lvalue = value.toLowerCase(Locale.ENGLISH);
                    if (lvalue.equals("movie")) {
                        type = "Reference Movie";
                        ok = true;
                    } else if (lvalue.equals("image")) {
                        type = "Reference Image";
                        ok = true;
                    } else if (lvalue.equals("html")) {
                        type = "Reference Link";
                        ok = true;
                    }
                } else if (lkeyString.equals("ref_url")) {
                    url = value;
                }
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
     * @param value
     *            the value to parse
     * @return a list of JHV points
     */
    private List<Vec3> parsePolygon(String value) {
        List<Vec3> polygonPoints = new ArrayList<Vec3>();
        if (value.toLowerCase(Locale.ENGLISH).contains("polygon")) {
            String coordinatesString = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')'));
            String coordinates = coordinatesString.substring(coordinatesString.indexOf('(') + 1, coordinatesString.lastIndexOf(')'));

            Scanner s = new Scanner(coordinates);
            s.useDelimiter(",");
            while (s.hasNext()) {
                String coordinateString = s.next();
                Vec3 tempPoint = parseCoordinates(coordinateString);
                if (tempPoint != null) {
                    polygonPoints.add(tempPoint);
                }
            }
            s.close();
        }
        return polygonPoints;
    }

    /**
     * Parses a point of the format POINT(0.716676950817756 73.6104596659652).
     *
     * @param value
     *            the point to parse
     * @return The GL3DVec3 or null if it could not be parsed.
     */
    private Vec3 parsePoint(String value) {
        if (value.toLowerCase(Locale.ENGLISH).contains("point")) {
            String coordinates = value.substring(value.indexOf('(') + 1, value.indexOf(')'));
            return parseCoordinates(coordinates);
        }
        return null;
    }

    /**
     * Parses a string of the format "0.716676950817756 73.6104596659652" to a
     * GL3DVec3
     *
     * @param coordinateString
     *            the string to parse
     * @return the GL3DVec3 or null of it could not be parsed
     */
    private Vec3 parseCoordinates(String coordinateString) {
        double[] coordinate = new double[] { 0., 0., 0. };
        boolean notnull = false;

        Scanner coordinatesScanner = new Scanner(coordinateString);
        coordinatesScanner.useDelimiter(" ");

        for (int i = 0; i < 3; i++) {
            if (coordinatesScanner.hasNext()) {
                coordinate[i] = Double.parseDouble(coordinatesScanner.next());
                notnull = true;
            }
        }
        coordinatesScanner.close();

        if (notnull) {
            return new Vec3(coordinate[0], coordinate[1], coordinate[2]);
        }
        return null;
    }

    private void handleCoordinates(JHVEvent currentEvent, List<Vec3> hgsBoundedBox, List<Vec3> hgsBoundCC, Vec3 hgsCentralPoint, Double hgsX, Double hgsY) {
        hgsBoundedBox = checkAndFixBoundingBox(hgsBoundedBox);
        handleHGSCoordinates(currentEvent, hgsBoundedBox, hgsBoundCC, hgsCentralPoint, hgsX, hgsY);
    }

    private List<Vec3> checkAndFixBoundingBox(List<Vec3> hgsBoundedBox) {
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
                hgsBoundedBox = null;
            }
        }
        return hgsBoundedBox;
    }

    private void handleHGSCoordinates(JHVEvent currentEvent, List<Vec3> hgsBoundedBox, List<Vec3> hgsBoundCC, Vec3 hgsCentralPoint, Double hgsX, Double hgsY) {
        if (hgsBoundedBox != null || hgsCentralPoint != null || (hgsX != null && hgsY != null) || hgsBoundCC != null) {
            List<Vec3> localHGSBoundedBox;
            List<Vec3> localHGSBoundCC;
            Vec3 localHGSCentralPoint = null;

            if (hgsBoundedBox != null) {
                localHGSBoundedBox = hgsBoundedBox;
            } else {
                localHGSBoundedBox = new ArrayList<Vec3>();
            }

            if (hgsBoundCC != null) {
                localHGSBoundCC = hgsBoundCC;
            } else {
                localHGSBoundCC = new ArrayList<Vec3>();
            }

            if (hgsCentralPoint != null) {
                localHGSCentralPoint = hgsCentralPoint;
            } else {
                if (hgsX != null && hgsY != null) {
                    localHGSCentralPoint = new Vec3(hgsX, hgsY, 0);
                }
            }

            Position.L p = Sun.getEarth(new JHVDate(currentEvent.start));

            ArrayList<Vec3> jhvBoundedBox = new ArrayList<Vec3>(localHGSBoundedBox.size());
            for (Vec3 el : localHGSBoundedBox) {
                jhvBoundedBox.add(convertHGSJHV(el, p));
            }

            ArrayList<Vec3> jhvBoundCC = new ArrayList<Vec3>(localHGSBoundCC.size());
            for (Vec3 el : localHGSBoundCC) {
                jhvBoundCC.add(convertHGSJHV(el, p));
            }

            Vec3 jhvCentralPoint = null;
            if (localHGSCentralPoint != null) {
                jhvCentralPoint = convertHGSJHV(localHGSCentralPoint, p);
            }

            currentEvent.addPositionInformation(new JHVPositionInformation(jhvCentralPoint, jhvBoundedBox, jhvBoundCC,
                    currentEvent.getName().equals("Coronal Mass Ejection") ? new Position.Q(p.time, p.rad, new Quat(p.lat, p.lon)) : null)); // reduce memory usage
        }
    }

    private Vec3 convertHGSJHV(Vec3 el, Position.L p) {
        double theta = Math.PI / 180 * el.y;
        double phi = Math.PI / 180 * el.x - p.lon;

        double x = Math.cos(theta) * Math.sin(phi);
        double z = Math.cos(theta) * Math.cos(phi);
        double y = -Math.sin(theta);
        return new Vec3(x, y, z);
    }

}
