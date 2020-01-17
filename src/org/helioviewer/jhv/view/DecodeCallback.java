package org.helioviewer.jhv.view;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.log.Log;

import com.google.common.util.concurrent.FutureCallback;

public abstract class DecodeCallback implements FutureCallback<ImageBuffer> {

    @Override
    public abstract void onSuccess(ImageBuffer result);

    @Override
    public void onFailure(@Nonnull Throwable t) {
        Log.error("Decode error", t);
    }

}
