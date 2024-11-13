package org.helioviewer.jhv.view;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.JHVTime;

public interface View {

    String EMPTY_METAXML = "<xml/>";
    AtomicBoolean complete = new AtomicBoolean(true);

    @Nullable
    default APIRequest getAPIRequest() {
        return null;
    }

    default void abolish() {
    }

    default void clearCache() {
    }

    void setFilter(ImageFilter.Type t);

    ImageFilter.Type getFilter();

    default void decode(Position viewpoint, double pixFactor, float factor) {
    }

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

    void setDataHandler(ImageData.Handler dataHandler);

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
