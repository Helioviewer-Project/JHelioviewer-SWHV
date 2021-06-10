package org.helioviewer.jhv.view;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.JHVTime;

public interface View {

    @Nullable
    APIRequest getAPIRequest();

    void abolish();

    void clearCache();

    void setMGN(boolean b);

    boolean getMGN();

    void decode(Position viewpoint, double pixFactor, float factor);

    @Nullable
    URI getURI();

    @Nullable
    LUT getDefaultLUT();

    boolean isMultiFrame();

    int getCurrentFrameNumber();

    int getMaximumFrameNumber();

    void setDataHandler(ImageDataHandler dataHandler);

    boolean isDownloading();

    boolean isComplete();

    @Nullable
    AtomicBoolean getFrameCacheStatus(int frame);

    JHVTime getFrameTime(int frame);

    JHVTime getFirstTime();

    JHVTime getLastTime();

    // <!- only for Layers
    boolean setNearestFrame(JHVTime time);

    JHVTime getNearestTime(JHVTime time);

    JHVTime getLowerTime(JHVTime time);

    JHVTime getHigherTime(JHVTime time);

    MetaData getMetaData(JHVTime time);
    // -->

    @Nonnull
    String getXMLMetaData() throws Exception;

}
