package org.helioviewer.jhv.view;

import java.awt.EventQueue;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.time.JHVTime;

public class BaseView implements View {

    protected final LatestWorker<DecodedImage> executor;
    protected final DataUri dataUri;

    protected final ImageProcessingSettings processingSettings;
    protected LUT builtinLUT;
    protected MetaData[] metaData = {BasicMetaData.EMPTY}; // paranoia

    public BaseView(LatestWorker<DecodedImage> _executor, DataUri _dataUri, ImageProcessingSettings _processingSettings) {
        executor = _executor;
        dataUri = _dataUri;
        processingSettings = _processingSettings;
    }

    @Nullable
    @Override
    public String getBaseName() {
        return dataUri == null ? null : dataUri.baseName();
    }

    @Override
    public JHVTime getFirstTime() {
        return metaData[0].getViewpoint().time;
    }

    @Override
    public JHVTime getLastTime() {
        return metaData[0].getViewpoint().time;
    }

    @Override
    public boolean setNearestFrame(JHVTime time) {
        return true;
    }

    @Override
    public JHVTime getNearestTime(JHVTime time) {
        return getFirstTime();
    }

    @Override
    public JHVTime getLowerTime(JHVTime time) {
        return getFirstTime();
    }

    @Override
    public JHVTime getHigherTime(JHVTime time) {
        return getLastTime();
    }

    @Override
    public JHVTime getFrameTime(int frame) {
        return getFirstTime();
    }

    @Override
    public MetaData getMetaData(JHVTime time) {
        return metaData[0];
    }

    @Nullable
    @Override
    public LUT getDefaultLUT() {
        if (builtinLUT != null)
            return builtinLUT;
        MetaData m = metaData[0];
        return m instanceof FitsMetaData fm ? LUT.get(fm) : null;
    }

    @Nullable
    protected View.DataHandler dataHandler;

    @Override
    public void setDataHandler(@Nullable View.DataHandler _dataHandler) {
        dataHandler = _dataHandler;
    }

    protected final LatestWorker.Callback<DecodedImage> decodeCallback(
            ImageBufferCache.Key key, int frame, Position viewpoint, BooleanSupplier isCurrent) {
        return new LatestWorker.Callback<>() {
            @Override
            public void onSuccess(DecodedImage result, boolean fresh) {
                if (dataHandler == null || !isCurrent.getAsBoolean()) {
                    result.release();
                    return;
                }

                ImageBufferCache.put(key, result);
                // Keep valid superseded results in the cache, without delivering them.
                if (fresh)
                    sendDataToHandler(frame, viewpoint, result, isCurrent);
            }

            @Override
            public void onFailure(Throwable t, boolean fresh) {
                if (dataHandler != null)
                    Log.errorStack(t);
            }
        };
    }

    protected final void sendDataToHandler(int frame, Position viewpoint, DecodedImage image, BooleanSupplier isCurrent) {
        MetaData m = metaData[frame];
        View.ImageData data = new View.ImageData(image, m, viewpoint);
        image.retain(); // The queued delivery owns a reference until its callback ends.
        EventQueue.invokeLater(() -> {
            try {
                if (dataHandler != null && isCurrent.getAsBoolean())
                    dataHandler.handleData(data);
            } finally {
                image.release();
            }
        });
    }

}
