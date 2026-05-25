package org.helioviewer.jhv.view;

import java.util.concurrent.Callable;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.threads.LatestWorker;

public class DecodeExecutor {

    private final LatestWorker<ImageBuffer> worker = new LatestWorker<>("Decoder");

    public void decode(Callable<ImageBuffer> callable, DecodeCallback callback) {
        worker.submit(callable, new LatestWorker.Callback<>() {
            @Override
            public void onSuccess(ImageBuffer result, boolean fresh) {
                callback.onSuccess(result, fresh);
            }

            @Override
            public void onFailure(Throwable t, boolean fresh) {
                callback.onFailure(t, fresh);
            }
        });
    }

    public void cancel() {
        worker.cancel();
    }

    public void abolish() {
        worker.abolish();
    }

}
