package org.helioviewer.jhv.source;

import java.io.IOException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.image.lut.LUT;

// The pixels of one file or one JPIP stream. Knows nothing about layers.
public interface Source {

    interface Listener {
        // Called from the reader thread; frame is the source's own index.
        void frameUpdated(Source source, int frame);
    }

    int frames();

    // Helioviewer XML, null when the frame has none.
    @Nullable
    String xml(int frame) throws IOException;

    // One level for sources without a resolution pyramid.
    ResolutionSet levels(int frame);

    @Nullable
    LUT lut();

    @Nullable
    ClipSet clipSet();

    boolean usesFITSParameters();

    // Runs on a decode worker. A source ignores the arguments it has no use for.
    ImageBuffer decode(int frame, int level, ImageFilter filter,
                       @Nullable ImageProcessingSettings.FITSParameters fits, @Nullable ClipSet.Range clip) throws Exception;

    // Held weakly by the source.
    default void setListener(@Nullable Listener listener) {}

    // Begins background retrieval; keys name the frames in the disk cache, null for none.
    default void start(String[] cacheKey) {}

    // A frame whose coarsest level is present.
    default boolean displayable(int frame) {
        return true;
    }

    // Asks a remote source for the level; the source keeps its own request state.
    default void request(int frame, int level, boolean priority) {}

    // Null: not displayable; false: displayable, incomplete at the requested level; true: complete.
    @Nullable
    default Boolean completion(int frame) {
        return true;
    }

    default boolean isComplete() {
        return true;
    }

    default boolean isDownloading() {
        return false;
    }

    void close();

}
