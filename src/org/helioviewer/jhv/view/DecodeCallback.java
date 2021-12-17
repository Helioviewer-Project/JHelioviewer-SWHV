package org.helioviewer.jhv.view;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log2;
import org.helioviewer.jhv.imagedata.ImageBuffer;

import com.google.common.util.concurrent.FutureCallback;

public abstract class DecodeCallback implements FutureCallback<ImageBuffer> {

    @Override
    public abstract void onSuccess(ImageBuffer result);

    @Override
    public void onFailure(@Nonnull Throwable t) {
        Log2.error(t);
    }

}
