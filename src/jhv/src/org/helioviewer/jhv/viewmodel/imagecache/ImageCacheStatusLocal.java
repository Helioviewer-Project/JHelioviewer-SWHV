package org.helioviewer.jhv.viewmodel.imagecache;

public class ImageCacheStatusLocal implements ImageCacheStatus {

    protected final int maxFrameNumber;

    public ImageCacheStatusLocal(int _maxFrameNumber) {
        maxFrameNumber = _maxFrameNumber;
    }

    @Override
    public CacheStatus getImageStatus(int compositionLayer) {
        return CacheStatus.COMPLETE;
    }

    @Override
    public void setImageStatus(int compositionLayer, int level, CacheStatus newStatus) {
    }

}
