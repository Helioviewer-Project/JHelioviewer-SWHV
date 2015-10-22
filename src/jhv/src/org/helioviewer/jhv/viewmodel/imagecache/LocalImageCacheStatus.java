package org.helioviewer.jhv.viewmodel.imagecache;

public class LocalImageCacheStatus implements ImageCacheStatus {

    private final int maxFrameNumber;

    public LocalImageCacheStatus(int _maxFrameNumber) {
        maxFrameNumber = _maxFrameNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheStatus getImageStatus(int compositionLayer) {
        return CacheStatus.COMPLETE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void downgradeImageStatus(int compositionLayer) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageCachedPartiallyUntil() {
        return maxFrameNumber;
    }

}
