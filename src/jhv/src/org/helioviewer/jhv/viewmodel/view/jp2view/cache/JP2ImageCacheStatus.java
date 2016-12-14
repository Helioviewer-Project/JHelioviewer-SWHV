package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;

public interface JP2ImageCacheStatus extends ImageCacheStatus {

    // Downgrades the status from complete to partial, if necessary
    void downgradeImageStatus(int startFrame, int endFrame);

    // Returns the highest frame until which the status is at least PARTIAL
    int getImageCachedPartiallyUntil();

    ResolutionSet getResolutionSet(int compositionLayer);

    boolean levelComplete(int level);

}
