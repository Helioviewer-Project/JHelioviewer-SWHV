package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;

public class JP2ImageCacheStatusLocal implements ImageCacheStatus {

    private final int maxFrameNumber;

    public JP2ImageCacheStatusLocal(int _maxFrameNumber) {
        maxFrameNumber = _maxFrameNumber;
    }

    @Override
    public CacheStatus getImageStatus(int compositionLayer) {
        return CacheStatus.COMPLETE;
    }

    @Override
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
    }

    @Override
    public void downgradeImageStatus(int compositionLayer) {
    }

    @Override
    public int getImageCachedPartiallyUntil() {
        return maxFrameNumber;
    }

}
