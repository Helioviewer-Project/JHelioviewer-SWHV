package org.helioviewer.jhv.plugins.pfss;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.DirectBufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.thread.LatestWorker;

final class PfssLineWorker {

    private final LatestWorker<Line> worker = new LatestWorker<>("PFSS-Line");
    private final Callback callback = new Callback();
    private final Consumer<Line> onReady;
    private Parameters submittedParameters;

    PfssLineWorker(@Nonnull Consumer<Line> _onReady) {
        onReady = _onReady;
    }

    void submit(Parameters parameters) {
        if (parameters.equals(submittedParameters))
            return;
        submittedParameters = parameters;
        worker.submit(new Builder(parameters), callback);
    }

    void cancel() {
        worker.cancel();
        submittedParameters = null;
    }

    private record Builder(Parameters parameters) implements Callable<Line> {
        @Nonnull
        @Override
        public Line call() {
            BufVertex lineBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64k
            PfssLine.calculatePositions(parameters.data, parameters.detail, parameters.fixedColor, parameters.radius, parameters.whiteBackground, lineBuf);
            return new Line(parameters, new DirectBufVertex(lineBuf));
        }
    }

    private final class Callback implements LatestWorker.Callback<Line> {
        @Override
        public void onSuccess(Line result, boolean fresh) {
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

    record Parameters(PfssLoader.Data data, int detail, boolean fixedColor, double radius, boolean whiteBackground) {
    }

    record Line(Parameters parameters, DirectBufVertex vertices) {
    }
}
