package org.helioviewer.jhv.view;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.time.JHVDate;

public interface View {

    @Nullable
    APIRequest getAPIRequest();

    void abolish();

    void decode(Position viewpoint, double pixFactor, double factor);

    URI getURI();

    boolean isLocal();

    String getName();

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

    JHVDate getFrameTime(int frame);

    JHVDate getFirstTime();

    JHVDate getLastTime();

    // <!- only for Layers
    boolean setNearestFrame(JHVDate time);

    JHVDate getNearestTime(JHVDate time);

    JHVDate getLowerTime(JHVDate time);

    JHVDate getHigherTime(JHVDate time);

    MetaData getMetaData(JHVDate time);
    // -->

    @Nonnull
    String getXMLMetaData(JHVDate time) throws Exception;

}
