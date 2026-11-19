package org.helioviewer.jhv.layers.viewpoint;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.opengl.BufVertex;

public final class ViewpointOrbitTrailTest {

    public static void main(String[] args) throws Exception {
        long start = 1_000_000, end = start + 120_000;
        Position.Cartesian[] positions = {
                new Position.Cartesian(start, 20, 0, 0),
                new Position.Cartesian(start + 60_000, 20, 10, 0),
                new Position.Cartesian(end, 30, 10, 0)
        };
        Constructor<PositionResponse> ctor = PositionResponse.class.getDeclaredConstructor(
                Position.Cartesian[].class, Position.Cartesian[].class, long.class, long.class);
        ctor.setAccessible(true);
        PositionResponse linear = ctor.newInstance(positions, null, start, end);
        ViewpointOrbitTrail trail = new ViewpointOrbitTrail(linear, 0, 240_000);
        float[] marker = {0, 0, 0, 1};
        BufVertex complete = build(trail, 240_000, marker);
        check(complete.getCount() <= 8, "Trail retained redundant linear samples");
        check(Arrays.equals(marker, new float[]{30, 10, 0, 1}), "Wrong last marker");

        ByteBuffer bytes = complete.toBuffer().order(ByteOrder.nativeOrder());
        int stride = bytes.remaining() / complete.getCount();
        boolean beforeCorner = false;
        boolean afterCorner = false;
        for (int i = 1; i < complete.getCount() - 1; i++) {
            int offset = i * stride;
            float x = bytes.getFloat(offset);
            float y = bytes.getFloat(offset + Float.BYTES);
            beforeCorner |= x == 20 && y > 0 && y < 10;
            afterCorner |= y == 10 && x > 20 && x < 30;
        }
        check(beforeCorner && afterCorner, "A vertex next to the trajectory corner was removed");

        build(trail, 168_000, marker);
        check(Arrays.equals(marker, new float[]{24, 10, 0, 1}), "Wrong marker after seeking backwards");
        build(trail, 48_000, marker);
        check(Arrays.equals(marker, new float[]{20, 4, 0, 1}), "Wrong marker inside a compacted segment");

        Position.Cartesian[] velocities = {
                new Position.Cartesian(start, 0.001, 0, 0),
                new Position.Cartesian(start + 60_000, 0, 0.001, 0),
                new Position.Cartesian(end, 0.001, 0, 0)
        };
        PositionResponse state = ctor.newInstance(positions, velocities, start, end);
        ViewpointOrbitTrail curved = new ViewpointOrbitTrail(state, 0, 240_000);
        check(build(curved, 240_000, marker).getCount() > complete.getCount(), "Curved samples were compacted");
        System.out.println("PASS: orbit trail compaction, corner preservation, remapped playback, and backward seeks");
    }

    private static BufVertex build(ViewpointOrbitTrail trail, long time, float[] marker) {
        int samples = trail.sampleCount(time);
        int count = trail.vertexCount(samples, time);
        BufVertex buf = new BufVertex(count);
        trail.putVertices(buf, marker, Colors.White.bytes(), samples, time);
        check(buf.getCount() == count, "Orbit sizing disagrees with emitted vertices");
        return buf;
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
