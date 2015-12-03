package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;

public class JP2ImageCacheStatusRemote implements ImageCacheStatus {

    private final int maxFrameNumber;
    // accessed from J2KReader, read also from EDT by MoviePanel, for the latter not very important if values are consistent
    private final CacheStatus[] imageStatus;
    private int imagePartialUntil = -1;

    public JP2ImageCacheStatusRemote(int _maxFrameNumber) {
        maxFrameNumber = _maxFrameNumber;
        imageStatus = new CacheStatus[maxFrameNumber + 1];
    }

    // not threadsafe
    @Override
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
        imageStatus[compositionLayer] = newStatus;
    }

    // not threadsafe
    @Override
    public void downgradeImageStatus(int compositionLayer) {
        if (imageStatus[compositionLayer] != CacheStatus.COMPLETE) { //!
            return;
        }
        imageStatus[compositionLayer] = CacheStatus.PARTIAL;
    }

    // not threadsafe
    @Override
    public CacheStatus getImageStatus(int compositionLayer) {
        return imageStatus[compositionLayer];
    }

    @Override
    public int getImageCachedPartiallyUntil() {
        int i = Math.max(0, imagePartialUntil);
        for (; i <= maxFrameNumber; i++) {
            if (imageStatus[i] != CacheStatus.PARTIAL && imageStatus[i] != CacheStatus.COMPLETE) {
                break;
            }
        }
        imagePartialUntil = i - 1;

        return imagePartialUntil;
    }

}
