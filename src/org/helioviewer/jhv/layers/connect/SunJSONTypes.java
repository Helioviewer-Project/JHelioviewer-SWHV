package org.helioviewer.jhv.layers.connect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.jogamp.opengl.GL3;

public class SunJSONTypes {

    public static class GeometryCollection {

        private final JHVTime time;
        private final HashMap<Double, BufVertex> linesMap = new HashMap<>();
        private final BufVertex pointsBuf;

        GeometryCollection(JHVTime _time, List<GeometryBuffer> bufList) {
            time = _time;

            // accelerate drawing by combining vertices
            List<BufVertex> pointsList = new ArrayList<>();
            ArrayListMultimap<Double, BufVertex> linesWidths = ArrayListMultimap.create();
            for (GeometryBuffer buf : bufList) {
                if (buf.type == BufType.line) { // lines separated into lists per thickness
                    linesWidths.put(buf.thickness, buf.vexBuf);
                } else { // points together in list
                    pointsList.add(buf.vexBuf);
                }
            }
            Multimaps.asMap(linesWidths).forEach((w, l) -> linesMap.put(w, BufVertex.join(l)));
            pointsBuf = pointsList.isEmpty() ? null : BufVertex.join(pointsList);
        }

        public JHVTime time() {
            return time;
        }

        public void render(GL3 gl, GLSLLine lines, GLSLShape points, double aspect, double factor) {
            linesMap.forEach((thickness, vexBuf) -> {
                lines.setVertexRepeatable(gl, vexBuf);
                lines.renderLine(gl, aspect, thickness * factor * 0.5e-2 /* TBD */);
            });
            if (pointsBuf != null) {
                points.setVertexRepeatable(gl, pointsBuf);
                points.renderPoints(gl, factor);
            }
        }
    }

    /**/
    record GeometryBuffer(BufType type, double thickness, BufVertex vexBuf) {
    }

    /**/
    private enum BufType {point, line} // type in BufVertex

    /**/
    static Vec3 convertCoord(double r, double lon, double lat) {
        if (r < 1)
            Log.warn("Radius < 1: " + r + ' ' + lon + ' ' + lat);
        lon = Math.toRadians(lon);
        lat = Math.toRadians(lat);
        return new Vec3(
                r * Math.cos(lat) * Math.sin(lon),
                r * Math.sin(lat),
                r * Math.cos(lat) * Math.cos(lon));
    }

    /**/
    static byte[] convertColor(int r, int g, int b, int a) {
        return Colors.bytes(
                MathUtils.clip(r, 0, 255),
                MathUtils.clip(g, 0, 255),
                MathUtils.clip(b, 0, 255),
                MathUtils.clip(a, 0, 255));
    }

    private enum GeometryType {point, line, ellipse} // type in JSON

    /**/
    static GeometryBuffer getGeometryBuffer(String typeString, List<Vec3> coordinates, List<byte[]> colors, double thickness) {
        GeometryType type = GeometryType.valueOf(typeString);

        if (colors.isEmpty())
            colors.add(Colors.Green);
        adjustColorsSize(type, coordinates, colors);

        BufVertex vexBuf = getVertices(type, coordinates, colors, thickness);
        return new GeometryBuffer(type == GeometryType.point ? BufType.point : BufType.line, thickness, vexBuf);
    }

    private static int getCoordsSize(GeometryType type, List<Vec3> coords) {
        int coordsSize = coords.size();
        switch (type) {
            case point -> {
                if (coordsSize < 1)
                    throw new IllegalArgumentException("Point type needs at least one coordinate");
            }
            case line -> {
                if (coordsSize < 2)
                    throw new IllegalArgumentException("Line type needs at least two coordinates");
            }
            case ellipse -> {
                if (coordsSize != 3)
                    throw new IllegalArgumentException("Ellipse type needs exactly three coordinates");
            }
        }
        return coordsSize;
    }

    private static void adjustColorsSize(GeometryType type, List<Vec3> coords, List<byte[]> colors) { // modifies colors
        int coordsSize = getCoordsSize(type, coords);
        int colorsSize = colors.size();
        if (colorsSize < coordsSize) {
            byte[] last = colors.get(colorsSize - 1);
            for (int i = 0; i < (coordsSize - colorsSize); i++) {
                colors.add(last);
            }
        } else if (colorsSize > coordsSize)
            colors.subList(coordsSize, colorsSize).clear();
    }

    private static BufVertex getVertices(GeometryType type, List<Vec3> coordinates, List<byte[]> colors, double thickness) {
        return switch (type) {
            case point -> getVerticesPoint(coordinates, colors, thickness);
            case line -> getVerticesLine(coordinates, colors);
            case ellipse -> getVerticesEllipse(coordinates, colors);
        };
    }

    private static BufVertex getVerticesPoint(List<Vec3> coordinates, List<byte[]> colors, double thickness) {
        int num = coordinates.size();
        BufVertex buf = new BufVertex(num * GLSLShape.stride);

        float pointSize = (float) (2 * thickness);
        for (int i = 0; i < num; i++) {
            Vec3 v = coordinates.get(i);
            buf.putVertex((float) v.x, (float) v.y, (float) v.z, pointSize, colors.get(i));
        }
        return buf;
    }

    private static BufVertex getVerticesLine(List<Vec3> coordinates, List<byte[]> colors) {
        int num = coordinates.size();
        BufVertex buf = new BufVertex((num + 2) * GLSLLine.stride);

        Vec3 v = coordinates.getFirst();
        buf.putVertex(v, Colors.Null);
        buf.repeatVertex(colors.getFirst());
        for (int i = 1; i < num; i++) {
            buf.putVertex(coordinates.get(i), colors.get(i));
        }
        buf.repeatVertex(Colors.Null);
        return buf;
    }

    private static final int SUBDIVISIONS = 360;

    private static BufVertex getVerticesEllipse(List<Vec3> coordinates, List<byte[]> colors) {
        BufVertex buf = new BufVertex((SUBDIVISIONS + 1 + 2) * GLSLLine.stride);

        Vec3 c = coordinates.get(0);
        Vec3 u = coordinates.get(1);
        Vec3 v = coordinates.get(2);
        byte[] color = colors.getFirst();

        u.minus(c);
        v.minus(c);
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double a = 2 * Math.PI * i / SUBDIVISIONS;
            double cost = Math.cos(a);
            double sint = Math.sin(a);

            double x = c.x + cost * u.x + sint * v.x;
            double y = c.y + cost * u.y + sint * v.y;
            double z = c.z + cost * u.z + sint * v.z;
            if (i == 0)
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            buf.putVertex((float) x, (float) y, (float) z, 1, color);
        }
        buf.repeatVertex(Colors.Null);
        return buf;
    }

}
