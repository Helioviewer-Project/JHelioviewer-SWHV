package org.helioviewer.jhv.plugins.pfss;

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.DirectBufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.threads.LatestWorker;

final class PfssLineWorker {

    interface Listener {
        void lineReady(Line line);
    }

    private final LatestWorker<Line> worker = new LatestWorker<>("PFSS-Line");
    private final Callback callback = new Callback();
    private Listener listener;
    private Parameters submittedParameters;

    void setListener(Listener _listener) {
        listener = _listener;
    }

    void submit(Parameters parameters) {
        if (parameters.equals(submittedParameters))
            return;
        submittedParameters = parameters;
        worker.submit(new Decoder(parameters), callback);
    }

    void cancel() {
        worker.cancel();
        submittedParameters = null;
    }

    private record Decoder(Parameters parameters) implements Callable<Line> {
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
            if (listener != null)
                listener.lineReady(result);
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
