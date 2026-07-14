package org.helioviewer.jhv.view;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.time.JHVTime;

public interface View {

    record ImageData(
            @Nonnull ImageBuffer imageBuffer,
            @Nonnull MetaData metaData,
            @Nonnull Region region,
            @Nonnull Position viewpoint) {}

    interface DataHandler {
        void handleData(ImageData imageData);
    }

    String EMPTY_METAXML = "<xml/>";
    AtomicBoolean complete = new AtomicBoolean(true);

    @Nullable
    default APIRequest getAPIRequest() {
        return null;
    }

    default void abolish() {}

    default void clearCache() {}

    // Set a shared fixed [min, max] display range across this view's frames (FITS only); no-op otherwise
    default void setRange(double min, double max) {}

    void setFilter(ImageFilter.Type t);

    ImageFilter.Type getFilter();

    default void decode(Position viewpoint, double pixFactor, float factor) {}

    @Nullable
    default String getBaseName() {
        return null;
    }

    @Nullable
    default LUT getDefaultLUT() {
        return null;
    }

    default boolean isMultiFrame() {
        return false;
    }

    default int getCurrentFrameNumber() {
        return 0;
    }

    default int getMaximumFrameNumber() {
        return 0;
    }

    void setDataHandler(DataHandler dataHandler);

    default boolean isDownloading() {
        return false;
    }

    default boolean isComplete() {
        return true;
    }

    @Nullable
    default AtomicBoolean getFrameCompletion(int frame) {
        return complete;
    }

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
    default String getXMLMetaData() {
        return EMPTY_METAXML;
    }

}
