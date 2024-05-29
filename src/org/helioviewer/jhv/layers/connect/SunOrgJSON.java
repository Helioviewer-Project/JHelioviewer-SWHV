package org.helioviewer.jhv.layers.connect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import org.helioviewer.jhv.Log;
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
        }).filter(Objects::nonNull).toList();
    }

    private static SunJSONTypes.GeometryBuffer createGeometry(JSONObject go) {
        List<Vec3> coords = new ArrayList<>();
        for (Object oc : go.getJSONArray("coordinates")) {
            if (oc instanceof JSONArray coord) {
                double c0 = coord.getDouble(0), c1 = coord.getDouble(1), c2 = coord.getDouble(2);
                coords.add(SunJSONTypes.convertCoord(c0, c1, c2));
            }
        }

        List<byte[]> colors = new ArrayList<>();
        JSONArray ca = go.optJSONArray("colors");
        if (ca != null) {
            for (Object oc : ca) {
                if (oc instanceof JSONArray color) {
                    int c0 = color.getInt(0), c1 = color.getInt(1), c2 = color.getInt(2), c3 = color.getInt(3);
                    colors.add(SunJSONTypes.convertColor(c0, c1, c2, c3));
                }
            }
        }

        double thickness = go.optDouble("thickness", 2 * GLSLLine.LINEWIDTH_BASIC);
        return SunJSONTypes.getGeometryBuffer(go.getString("type"), coords, colors, thickness);
    }

}
