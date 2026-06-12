package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.DirectBufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.thread.LatestWorker;

final class ViewpointOrbitWorker {

    private static final float SIZE_PLANET = 5;
    private static final DirectBufVertex EMPTY_VERTICES = new DirectBufVertex(new BufVertex(0));

    private final LatestWorker<Prepared> worker = new LatestWorker<>("Viewpoint-Orbit");
    private final ViewpointOrbitTrail.Cache orbitTrails = new ViewpointOrbitTrail.Cache();
    private final Callback callback = new Callback();
    private final Consumer<Prepared> onReady;

    private Parameters submittedParameters;

    ViewpointOrbitWorker(@Nonnull Consumer<Prepared> _onReady) {
        onReady = _onReady;
    }

    void submit(Parameters parameters) {
        if (parameters.compatibleWith(submittedParameters))
            return;
        submittedParameters = parameters;
        worker.submit(new Builder(parameters, orbitTrails), callback);
    }

    void cancel() {
        worker.cancel();
        submittedParameters = null;
    }

    private final class Callback implements LatestWorker.Callback<Prepared> {
        @Override
        public void onSuccess(Prepared result, boolean fresh) {
            if (!fresh)
                return;
            submittedParameters = null;
            onReady.accept(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t, boolean fresh) {
            if (fresh)
                submittedParameters = null;
            LatestWorker.Callback.super.onFailure(t, fresh);
        }
    }

    private record Builder(Parameters parameters, ViewpointOrbitTrail.Cache orbitTrails) implements Callable<Prepared> {
        @Nonnull
        @Override
        public Prepared call() {
            BufVertex orbitBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64k
            BufVertex planetBuf = new BufVertex(Math.max(parameters.entries.size(), 1) * GLSLShape.stride);
            float[] currentPoint = {0, 0, 0, 1};

            ArrayList<PositionLoad> positionLoads = new ArrayList<>(parameters.entries.size());
            for (Entry entry : parameters.entries) {
                positionLoads.add(entry.positionLoad);
                ViewpointOrbitTrail trail = orbitTrails.get(entry.positionLoad, entry.response, parameters.start, parameters.end);
                trail.putVertices(orbitBuf, currentPoint, entry.color, parameters.time);
                planetBuf.putVertex(currentPoint[0], currentPoint[1], currentPoint[2], SIZE_PLANET, entry.color);
            }
            orbitTrails.prune(positionLoads);

            DirectBufVertex orbitVertices = orbitBuf.getCount() >= 4 ? new DirectBufVertex(orbitBuf) : EMPTY_VERTICES;
            return new Prepared(parameters, orbitVertices, new DirectBufVertex(planetBuf));
        }
    }

    record Entry(PositionLoad positionLoad, PositionResponse response, byte[] color) {
    }

    record Parameters(List<Entry> entries, long time, long start, long end) {
        boolean compatibleWith(Parameters other) {
            return other != null && start == other.start && end == other.end && entries.equals(other.entries);
        }
    }

    record Prepared(Parameters parameters, DirectBufVertex orbitVertices, DirectBufVertex planetVertices) {
    }
}
