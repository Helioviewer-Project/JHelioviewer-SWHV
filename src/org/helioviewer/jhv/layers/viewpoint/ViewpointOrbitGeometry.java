package org.helioviewer.jhv.layers.viewpoint;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.opengl.BufVertex;

public final class ViewpointOrbitGeometry {

    static final float SIZE_PLANET = 5;

    private final ViewpointOrbitTrail.Cache orbitTrails = new ViewpointOrbitTrail.Cache();

    public Prepared build(Parameters parameters) {
        int entryCount = parameters.entries.size();
        ArrayList<PositionLoad> positionLoads = new ArrayList<>(entryCount);
        ArrayList<ViewpointOrbitTrail> trails = new ArrayList<>(entryCount);
        int[] sampleCounts = new int[entryCount];
        int orbitVertexCount = 0;
        for (int i = 0; i < entryCount; i++) {
            Entry entry = parameters.entries.get(i);
            positionLoads.add(entry.positionLoad);
            ViewpointOrbitTrail trail = orbitTrails.get(entry.positionLoad, entry.response, parameters.start, parameters.end);
            trails.add(trail);
            int sampleCount = trail.sampleCount(parameters.time);
            sampleCounts[i] = sampleCount;
            orbitVertexCount += trail.vertexCount(sampleCount, parameters.time);
        }

        BufVertex orbitBuf = new BufVertex(orbitVertexCount);
        BufVertex planetBuf = new BufVertex(entryCount);
        ArrayList<Marker> markers = new ArrayList<>(entryCount);
        float[] currentPoint = {0, 0, 0, 1};
        for (int i = 0; i < entryCount; i++) {
            Entry entry = parameters.entries.get(i);
            trails.get(i).putVertices(orbitBuf, currentPoint, entry.color, sampleCounts[i], parameters.time);
            planetBuf.putVertex(currentPoint[0], currentPoint[1], currentPoint[2], SIZE_PLANET, entry.color);
            markers.add(new Marker(entry.positionLoad, currentPoint[0], currentPoint[1], currentPoint[2]));
        }
        orbitTrails.prune(positionLoads);

        if (orbitBuf.getCount() < 4)
            orbitBuf.clear();
        return new Prepared(orbitBuf, planetBuf, markers);
    }

    public record Entry(PositionLoad positionLoad, PositionResponse response, byte[] color) {
    }

    public record Parameters(List<Entry> entries, long time, long start, long end) {
        public Parameters {
            time = start < end ? Math.clamp(time, start, end) : start;
        }
    }

    public record Marker(PositionLoad load, float x, float y, float z) {
    }

    public record Prepared(BufVertex orbitVertices, BufVertex planetVertices, List<Marker> markers) {
        public Prepared {
            markers = List.copyOf(markers);
        }
    }
}
