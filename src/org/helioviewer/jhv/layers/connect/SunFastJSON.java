package org.helioviewer.jhv.layers.connect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

import com.alibaba.fastjson2.JSON;

public class SunFastJSON {

    static SunJSONTypes.GeometryCollection process(String string) {
        return JSON.parseObject(string, JObject.class).jparse();
    }

    private record JObject(String type, String time, List<JGeometry> geometry) {
        SunJSONTypes.GeometryCollection jparse() {
            if (!"SunJSON".equals(type))
                throw new IllegalArgumentException("Unknown type: " + type);

            JHVTime jtime = new JHVTime(time);
            List<SunJSONTypes.GeometryBuffer> gl = geometry.parallelStream().map(jg -> {
                try {
                    return createGeometry(jg);
                } catch (Exception e) {
                    Log.error(e);
                    return null;
                }
            }).filter(Objects::nonNull).toList();
            return new SunJSONTypes.GeometryCollection(jtime, gl);
        }
    }

    private static SunJSONTypes.GeometryBuffer createGeometry(JGeometry jg) {
        SunJSONTypes.GeometryType type = SunJSONTypes.GeometryType.valueOf(jg.type);
        int size = jg.coordinates.size();

        List<Vec3> coords = new ArrayList<>(size);
        for (double[] c : jg.coordinates) {
            if (c.length != 3)
                throw new IllegalArgumentException("Coordinate length not 3");
            // should check range
            coords.add(new Vec3(c[0], Math.toRadians(c[1]), Math.toRadians(c[2]))); // should check range
        }

        List<byte[]> colors = new ArrayList<>(size);
        for (int[] c : jg.colors) {
            if (c.length != 4)
                throw new IllegalArgumentException("Color length not 4");
            colors.add(Colors.bytes(
                    MathUtils.clip(c[0], 0, 255),
                    MathUtils.clip(c[1], 0, 255),
                    MathUtils.clip(c[2], 0, 255),
                    MathUtils.clip(c[3], 0, 255)));
        }
        if (colors.isEmpty())
            colors.add(Colors.Green);
        SunJSONTypes.adjustColorsSize(type, coords, colors);

        double thickness = MathUtils.clip(jg.thickness, 1e-5, 1e-1);

        SunJSONTypes.Geometry g = new SunJSONTypes.Geometry(type, coords, colors, thickness);
        return SunJSONTypes.getGeometryBuffer(g);
    }

    private record JGeometry(String type, List<double[]> coordinates, List<int[]> colors, double thickness) {
    }

}
