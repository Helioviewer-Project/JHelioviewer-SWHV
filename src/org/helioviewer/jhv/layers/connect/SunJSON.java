package org.helioviewer.jhv.layers.connect;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class SunJSON {

    private static final int SUBDIVISIONS = 360;

    private enum GeometryType {Point, Line, Ellipse}

    private record Geometry(GeometryType type, List<Vec3> coordinates, List<byte[]> colors, double thickness) {
    }

    private record GeometryBuffer(Geometry g, BufVertex vexBuf) {
    }

    public record GeometryCollection(JHVTime time, List<GeometryBuffer> list) {
        public void render(GL2 gl, GLSLLine lines, GLSLShape points, double aspect, double factor) {
            for (GeometryBuffer buf : list) {
                if (buf.g.type == GeometryType.Point) {
                    points.setVertexRepeatable(gl, buf.vexBuf);
                    points.renderPoints(gl, factor);
                } else {
                    lines.setVertexRepeatable(gl, buf.vexBuf);
                    lines.renderLine(gl, aspect, buf.g.thickness * factor * 0.5e-2 /* TBD */);
                }
            }
        }
    }

    static GeometryCollection process(JSONObject jo) throws JSONException {
        Pair<JHVTime, List<Geometry>> res = parse(jo);

        List<GeometryBuffer> bufs = new ArrayList<>();
        for (Geometry g : res.right()) {
            int coords = g.coordinates.size();

            BufVertex vexBuf = switch (g.type) {
                case Point -> new BufVertex(coords * GLSLShape.stride);
                case Line -> new BufVertex((coords + 2) * GLSLLine.stride);
                case Ellipse -> new BufVertex((SUBDIVISIONS + 1 + 2) * GLSLLine.stride);
            };
            putGeometry(g, vexBuf);
            bufs.add(new GeometryBuffer(g, vexBuf));
        }
        return new GeometryCollection(res.left(), bufs);
    }

    private static Pair<JHVTime, List<Geometry>> parse(JSONObject jo) throws JSONException {
        List<Geometry> geometryList = new ArrayList<>();
        JHVTime time = TimeUtils.J2000;

        if ("SunJSON".equals(jo.optString("type"))) {
            if (jo.has("time"))
                time = new JHVTime(jo.getString("time"));

            for (Object og : jo.getJSONArray("geometry")) {
                if (og instanceof JSONObject go) {
                    String strType = go.getString("type");
                    GeometryType type = switch (strType) {
                        case "point" -> GeometryType.Point;
                        case "line" -> GeometryType.Line;
                        case "ellipse" -> GeometryType.Ellipse;
                        default -> throw new JSONException("Unknown geometry type: " + strType);
                    };

                    List<Vec3> coords = new ArrayList<>();
                    for (Object oc : go.getJSONArray("coordinates")) {
                        if (oc instanceof JSONArray coord) {
                            coords.add(new Vec3(coord.getDouble(0), Math.toRadians(coord.getDouble(1)), Math.toRadians(coord.getDouble(2))));
                        }
                    }
                    int coordsSize = coords.size();
                    switch (type) {
                        case Point -> {
                            if (coordsSize < 1)
                                throw new JSONException("Point type needs at least one coordinate");
                        }
                        case Line -> {
                            if (coordsSize < 2)
                                throw new JSONException("Line type needs at least two coordinates");
                        }
                        case Ellipse -> {
                            if (coordsSize != 3)
                                throw new JSONException("Ellipse type needs exactly three coordinates");
                        }
                    }

                    List<byte[]> colors = new ArrayList<>();
                    JSONArray ca = go.optJSONArray("colors");
                    if (ca != null) {
                        for (Object oc : ca) {
                            if (oc instanceof JSONArray color) {
                                colors.add(
                                        Colors.bytes(
                                                MathUtils.clip(color.getInt(0), 0, 255),
                                                MathUtils.clip(color.getInt(1), 0, 255),
                                                MathUtils.clip(color.getInt(2), 0, 255),
                                                MathUtils.clip(color.getInt(3), 0, 255)));
                            }
                        }
                    }
                    if (colors.isEmpty())
                        colors.add(Colors.Green);

                    int colorsSize = colors.size();
                    if (colorsSize < coordsSize) {
                        byte[] last = colors.get(colorsSize - 1);
                        for (int i = 0; i < (coordsSize - colorsSize); i++) {
                            colors.add(last);
                        }
                    } else if (colorsSize > coordsSize)
                        colors.subList(coordsSize, colorsSize).clear();

                    double thickness = go.optDouble("thickness", 2 * GLSLLine.LINEWIDTH_BASIC);
                    geometryList.add(new Geometry(type, coords, colors, thickness));
                }
            }
        }

        return new Pair<>(time, geometryList);
    }

    private static void toCartesian(Vec3 v, double r, double lon, double lat) {
        v.x = r * Math.cos(lat) * Math.sin(lon);
        v.y = r * Math.sin(lat);
        v.z = r * Math.cos(lat) * Math.cos(lon);
    }

    private static void putGeometry(Geometry g, BufVertex buf) {
        switch (g.type) {
            case Point -> {
                Vec3 v = new Vec3();
                int coordsSize = g.coordinates.size();
                float pointSize = (float) (2 * g.thickness);

                for (int i = 0; i < coordsSize; i++) {
                    Vec3 coord = g.coordinates.get(i);
                    if (coord.x > 1) {
                        toCartesian(v, coord.x, coord.y, coord.z);
                        buf.putVertex((float) v.x, (float) v.y, (float) v.z, pointSize, g.colors.get(i));
                    }
                }
            }
            case Line -> {
                Vec3 v = new Vec3();
                int coordsSize = g.coordinates.size();
                boolean broken = false;

                for (int i = 0; i < coordsSize; i++) {
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
                    if (i == coordsSize - 1)
                        buf.repeatVertex(Colors.Null);
                }
            }
            case Ellipse -> {
                Vec3 c = g.coordinates.get(0);
                toCartesian(c, c.x, c.y, c.z);
                Vec3 u = g.coordinates.get(1);
                toCartesian(u, u.x, u.y, u.z);
                Vec3 v = g.coordinates.get(2);
                toCartesian(v, v.x, v.y, v.z);
                byte[] color = g.colors.get(0);

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
            }
        }
    }

}
