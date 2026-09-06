package org.helioviewer.jhv.plugins.swek.sources;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.JHVPositionInformation;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

final class HEKGeometry {

    private record HgsPoint(double longitudeDeg, double latitudeDeg) {}

    private List<HgsPoint> hgsBoundedBox;
    private List<HgsPoint> hgsBoundCC;
    private HgsPoint hgsCentralPoint;
    private Double hgsLongitudeDeg;
    private Double hgsLatitudeDeg;

    boolean readParameter(String key, String value) {
        switch (key) {
            case "hgs_bbox" -> hgsBoundedBox = parsePolygon(value);
            case "hgs_boundcc" -> hgsBoundCC = parsePolygon(value);
            case "hgs_coord" -> hgsCentralPoint = parsePoint(value);
            case "hgs_x" -> hgsLongitudeDeg = Double.valueOf(value);
            case "hgs_y" -> hgsLatitudeDeg = Double.valueOf(value);
            default -> { return false; }
        }
        return true;
    }

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

    @Nullable
    private static HgsPoint parsePoint(String value) {
        if (containsIgnoreCase(value, "point")) {
            return parseCoordinates(value.substring(value.indexOf('(') + 1, value.indexOf(')')));
        }
        return null;
    }

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

    void applyTo(JHVEvent currentEvent) {
        List<HgsPoint> hgsBoundedBox = checkAndFixBoundingBox(this.hgsBoundedBox);
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
        currentEvent.addPositionInformation(new JHVPositionInformation(jhvCentralPoint, jhvBoundary, currentEvent.isCactus() ? p : null)); // reduce memory usage
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
