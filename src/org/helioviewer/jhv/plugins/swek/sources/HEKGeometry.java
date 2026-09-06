package org.helioviewer.jhv.plugins.swek.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.JHVPositionInformation;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONObject;

final class HEKGeometry {

    private record HgsPoint(double longitudeDeg, double latitudeDeg) {}

    private static final Pattern POINT = Pattern.compile("\\s*POINT\\s*\\(([^()]*)\\)\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern POLYGON = Pattern.compile("\\s*POLYGON\\s*\\(\\s*\\(([^()]*)\\)\\s*\\)\\s*", Pattern.CASE_INSENSITIVE);

    private final List<HgsPoint> hgsBoundedBox;
    private final List<HgsPoint> hgsBoundCC;
    private final HgsPoint hgsCentralPoint;
    private final Double hgsLongitudeDeg;
    private final Double hgsLatitudeDeg;

    HEKGeometry(JSONObject result) {
        hgsBoundedBox = result.isNull("hgs_bbox") ? null : parsePolygon(result.optString("hgs_bbox"));
        hgsBoundCC = result.isNull("hgs_boundcc") ? null : parsePolygon(result.optString("hgs_boundcc"));
        hgsCentralPoint = result.isNull("hgs_coord") ? null : parsePoint(result.optString("hgs_coord"));
        hgsLongitudeDeg = result.isNull("hgs_x") ? null : parseNumber(result.optString("hgs_x"));
        hgsLatitudeDeg = result.isNull("hgs_y") ? null : parseNumber(result.optString("hgs_y"));
    }

    private static List<HgsPoint> parsePolygon(String value) {
        Matcher matcher = POLYGON.matcher(value);
        if (!matcher.matches()) return List.of();
        List<HgsPoint> points = new ArrayList<>();
        for (String coordinate : matcher.group(1).split(",", -1)) {
            HgsPoint point = parseCoordinates(coordinate);
            // Dropping a bad vertex would invent an edge between its neighbors.
            if (point == null) return List.of();
            points.add(point);
        }
        if (points.size() < 4) return List.of();
        HgsPoint first = points.getFirst(), last = points.getLast();
        if (first.longitudeDeg() != last.longitudeDeg() || first.latitudeDeg() != last.latitudeDeg()) return List.of();
        return points;
    }

    @Nullable
    private static HgsPoint parsePoint(String value) {
        Matcher matcher = POINT.matcher(value);
        return matcher.matches() ? parseCoordinates(matcher.group(1)) : null;
    }

    @Nullable
    private static HgsPoint parseCoordinates(String value) {
        String[] parts = Regex.MultiSpace.split(value.trim(), 3);
        return parts.length == 2 ? point(parseNumber(parts[0]), parseNumber(parts[1])) : null;
    }

    @Nullable
    private static Double parseNumber(String value) {
        try {
            double number = Double.parseDouble(value);
            return Double.isFinite(number) ? number : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static HgsPoint point(Double longitude, Double latitude) {
        return longitude != null && latitude != null && Math.abs(latitude) <= 90 ? new HgsPoint(longitude, latitude) : null;
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

    void applyTo(JHVEvent currentEvent) {
        List<HgsPoint> hgsBoundedBox = checkAndFixBoundingBox(this.hgsBoundedBox);
        if (hgsBoundedBox == null && hgsCentralPoint == null && (hgsLongitudeDeg == null || hgsLatitudeDeg == null) && hgsBoundCC == null)
            return;

        List<HgsPoint> boundary = hgsBoundCC != null && !hgsBoundCC.isEmpty() ? hgsBoundCC : hgsBoundedBox;
        if (boundary == null) boundary = List.of();
        HgsPoint centralPoint = hgsCentralPoint;
        if (centralPoint == null && hgsLongitudeDeg != null && hgsLatitudeDeg != null)
            centralPoint = point(hgsLongitudeDeg, hgsLatitudeDeg);

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
        currentEvent.addPositionInformation(new JHVPositionInformation(jhvCentralPoint, jhvBoundary, currentEvent.isCactus() ? p : null)); // reduce memory usage
    }

    private static Vec3 hgsToJhv(HgsPoint point, double elon) {
        return SphericalCoords.unit(Math.toRadians(point.longitudeDeg()) - elon, Math.toRadians(point.latitudeDeg()));
    }

}
