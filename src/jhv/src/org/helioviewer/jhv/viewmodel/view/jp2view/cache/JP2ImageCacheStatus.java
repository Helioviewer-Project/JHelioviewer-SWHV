package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;

public interface JP2ImageCacheStatus {

    int getImageCachedPartiallyUntil();

    ResolutionSet getResolutionSet(int frame);

    boolean isLevelComplete(int level);

    AtomicBoolean getFrameLevelStatus(int frame, int level);

    void setFrameLevelComplete(int frame, int level);

    void setFrameLevelPartial(int frame);

}
