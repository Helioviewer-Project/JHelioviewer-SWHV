package org.helioviewer.jhv.view.jp2view.cache;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.helioviewer.jhv.view.jp2view.image.ResolutionSet;

public interface CacheStatus {

    int getPartialUntil();

    ResolutionSet getResolutionSet(int frame);

    boolean isComplete(int level);

    @Nullable
    AtomicBoolean getFrameStatus(int frame, int level);

    void setFrameComplete(int frame, int level);

    void setFramePartial(int frame);

}
