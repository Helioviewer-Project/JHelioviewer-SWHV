package org.helioviewer.jhv.layers.connect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class SunOrgJSON {

    static SunJSONTypes.GeometryCollection process(JSONObject jo) {
        JHVTime time = TimeUtils.J2000;
        if ("SunJSON".equals(jo.optString("type"))) {
            if (jo.has("time"))
                time = new JHVTime(jo.getString("time"));

            JSONArray ga = jo.optJSONArray("geometry");
            if (ga instanceof JSONArray) {
                return new SunJSONTypes.GeometryCollection(time, parseInput(ga));
            }
        }
        return new SunJSONTypes.GeometryCollection(time, Collections.emptyList());
    }

    private static List<SunJSONTypes.GeometryBuffer> parseInput(JSONArray ga) {
        return StreamSupport.stream(ga.spliterator(), true).map(og -> {
            if (!(og instanceof JSONObject go))
                return null;
            try {
                return createGeometry(go);
            } catch (Exception e) {
                Log.error(e);
                return null;
            }
        }).filter(Objects::nonNull).map(SunJSONTypes::getGeometryBuffer).toList();
    }

    private static SunJSONTypes.Geometry createGeometry(JSONObject go) {
        SunJSONTypes.GeometryType type = SunJSONTypes.GeometryType.valueOf(go.getString("type"));

        List<Vec3> coords = new ArrayList<>();
        for (Object oc : go.getJSONArray("coordinates")) {
            if (oc instanceof JSONArray coord) {
                coords.add(new Vec3(coord.getDouble(0), Math.toRadians(coord.getDouble(1)), Math.toRadians(coord.getDouble(2)))); // should check range
            }
        }

        List<byte[]> colors = new ArrayList<>();
        JSONArray ca = go.optJSONArray("colors");
        if (ca != null) {
            for (Object oc : ca) {
                if (oc instanceof JSONArray color) {
                    colors.add(Colors.bytes(
                            MathUtils.clip(color.getInt(0), 0, 255),
                            MathUtils.clip(color.getInt(1), 0, 255),
                            MathUtils.clip(color.getInt(2), 0, 255),
                            MathUtils.clip(color.getInt(3), 0, 255)));
                }
            }
        }
        if (colors.isEmpty())
            colors.add(Colors.Green);
        SunJSONTypes.adjustColorsSize(type, coords, colors);

        double thickness = go.optDouble("thickness", 2 * GLSLLine.LINEWIDTH_BASIC);
        return new SunJSONTypes.Geometry(type, coords, colors, thickness);
    }

}
