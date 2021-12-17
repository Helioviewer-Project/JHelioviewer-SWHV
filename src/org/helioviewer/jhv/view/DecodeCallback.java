package org.helioviewer.jhv.view;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;

import com.google.common.util.concurrent.FutureCallback;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DecodeCallback implements FutureCallback<ImageBuffer> {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    @Override
    public abstract void onSuccess(ImageBuffer result);

    @Override
    public void onFailure(@Nonnull Throwable t) {
        LOGGER.log(Level.SEVERE, "Decode error", t);
    }

}
