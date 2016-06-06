package org.helioviewer.jhv.viewmodel.imagecache;

public class ImageCacheStatusLocal implements ImageCacheStatus {

    private final int maxFrameNumber;

    public ImageCacheStatusLocal(int _maxFrameNumber) {
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
    public void downgradeImageStatus(int startFrame, int endFrame) {
    }

    @Override
    public int getImageCachedPartiallyUntil() {
        return maxFrameNumber;
    }

}
