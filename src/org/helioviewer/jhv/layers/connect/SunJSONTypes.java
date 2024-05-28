package org.helioviewer.jhv.layers.connect;

import java.util.List;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;

import com.jogamp.opengl.GL2;

public class SunJSONTypes {

    public record GeometryCollection(JHVTime time, List<GeometryBuffer> bufList) {
        public void render(GL2 gl, GLSLLine lines, GLSLShape points, double aspect, double factor) {
            for (GeometryBuffer buf : bufList) {
                if (buf.g.type == GeometryType.point) {
                    points.setVertexRepeatable(gl, buf.vexBuf);
                    points.renderPoints(gl, factor);
                } else {
                    lines.setVertexRepeatable(gl, buf.vexBuf);
                    lines.renderLine(gl, aspect, buf.g.thickness * factor * 0.5e-2 /* TBD */);
                }
            }
        }
    }

    /**/enum GeometryType {point, line, ellipse}

    /**/record Geometry(GeometryType type, List<Vec3> coordinates, List<byte[]> colors, double thickness) {
    }

    /**/record GeometryBuffer(Geometry g, BufVertex vexBuf) {
    }

    /**/
    static void adjustColorsSize(GeometryType type, List<Vec3> coords, List<byte[]> colors) { // modifies colors
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

    /**/
    static BufVertex getVertices(Geometry g) {
        return switch (g.type) {
            case point -> getVerticesPoint(g);
            case line -> getVerticesLine(g);
            case ellipse -> getVerticesEllipse(g);
        };
    }

    private static void toCartesian(Vec3 v, double r, double lon, double lat) {
        v.x = r * Math.cos(lat) * Math.sin(lon);
        v.y = r * Math.sin(lat);
        v.z = r * Math.cos(lat) * Math.cos(lon);
    }

    private static BufVertex getVerticesPoint(Geometry g) {
        int num = g.coordinates.size();
        BufVertex buf = new BufVertex(num * GLSLShape.stride);

        Vec3 v = new Vec3();
        float pointSize = (float) (2 * g.thickness);
        for (int i = 0; i < num; i++) {
            Vec3 coord = g.coordinates.get(i);
            if (coord.x > 1) {
                toCartesian(v, coord.x, coord.y, coord.z);
                buf.putVertex((float) v.x, (float) v.y, (float) v.z, pointSize, g.colors.get(i));
            }
        }
        return buf;
    }

    private static BufVertex getVerticesLine(Geometry g) {
        int num = g.coordinates.size();
        BufVertex buf = new BufVertex((num + 2) * GLSLLine.stride);

        Vec3 v = new Vec3();
        boolean broken = false;
        for (int i = 0; i < num; i++) {
            Vec3 coord = g.coordinates.get(i);
            toCartesian(v, coord.x, coord.y, coord.z);
            if (i == 0)
                buf.putVertex(v, Colors.Null);
            if (coord.x <= 1) {
                buf.repeatVertex(Colors.Null);
                broken = true;
            } else {
                if (broken) {
                    buf.putVertex(v, Colors.Null);
                    broken = false;
                }
                buf.putVertex(v, g.colors.get(i));
            }
            if (i == num - 1)
                buf.repeatVertex(Colors.Null);
        }
        return buf;
    }

    private static final int SUBDIVISIONS = 360;

    private static BufVertex getVerticesEllipse(Geometry g) {
        BufVertex buf = new BufVertex((SUBDIVISIONS + 1 + 2) * GLSLLine.stride);

        Vec3 c = g.coordinates.get(0);
        toCartesian(c, c.x, c.y, c.z);
        Vec3 u = g.coordinates.get(1);
        toCartesian(u, u.x, u.y, u.z);
        Vec3 v = g.coordinates.get(2);
        toCartesian(v, v.x, v.y, v.z);
        byte[] color = g.colors.get(0);

        u.minus(c);
        v.minus(c);
        for (int j = 0; j <= SUBDIVISIONS; j++) {
            double a = 2 * Math.PI * j / SUBDIVISIONS;
            double cost = Math.cos(a);
            double sint = Math.sin(a);

            double x = c.x + cost * u.x + sint * v.x;
            double y = c.y + cost * u.y + sint * v.y;
            double z = c.z + cost * u.z + sint * v.z;

            if (Math.sqrt(x * x + y * y + z * z) < 1) {
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
                continue;
            }

            if (j == 0)
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            buf.putVertex((float) x, (float) y, (float) z, 1, color);
            if (j == SUBDIVISIONS)
                buf.repeatVertex(Colors.Null);
        }
        return buf;
    }

}
