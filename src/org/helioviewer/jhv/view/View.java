package org.helioviewer.jhv.view;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.time.JHVTime;

public interface View {

    record ImageData(
            @Nonnull DecodedImage image,
            @Nonnull MetaData metaData,
            @Nonnull Position viewpoint) {

        public ImageBuffer imageBuffer() {
            return image.imageBuffer();
        }

        public Region region() {
            return image.region();
        }

    }

    interface DataHandler {
        // Borrowed for this call. Retain the decoded image if it will be kept afterward.
        void handleData(@Nonnull ImageData imageData);
    }

    String EMPTY_METAXML = "<xml/>";

    @Nullable
    default APIRequest getAPIRequest() {
        return null;
    }

    default void abolish() {}

    default void collectCacheOwners(Set<Object> owners) {}

    default void clearCache() {
        Set<Object> owners = new HashSet<>();
        collectCacheOwners(owners);
        ImageBufferCache.invalidateOwners(owners);
    }

    // Use a cleanup worker for local sources; remote readers schedule their own shutdown.
    default void closeSources() {}

    default void decode(Position viewpoint, double pixFactor, @Nullable ClipSet.Range clipRange) {}

    @Nullable
    default ClipSet getClipSet() {
        return null;
    }

    @Nullable
    default String getBaseName() {
        return null;
    }

    @Nullable
    default LUT getDefaultLUT() {
        return null;
    }

    default boolean hasFITS() {
        return false;
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

    void setDataHandler(@Nullable DataHandler dataHandler);

    default boolean isDownloading() {
        return false;
    }

    default boolean isComplete() {
        return true;
    }

    // Snapshot: null when unavailable, false when partial, true when complete.
    @Nullable
    default Boolean getFrameCompletion(int frame) {
        return true;
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
    default String getXMLMetaData(JHVTime time) {
        return EMPTY_METAXML;
    }

}
