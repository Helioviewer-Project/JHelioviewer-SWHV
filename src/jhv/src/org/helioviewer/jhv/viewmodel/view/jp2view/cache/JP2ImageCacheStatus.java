package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;

public interface JP2ImageCacheStatus extends ImageCacheStatus {

    // Downgrades the status from complete to partial, if necessary
    void downgradeVisibleStatus(int level);

    // Returns the highest frame until which the status is at least PARTIAL
    int getImageCachedPartiallyUntil();

    ResolutionSet getResolutionSet(int frame);

    boolean levelComplete(int level);

    boolean imageComplete(int frame, int level);

    void setImageComplete(int frame, int level);

}
