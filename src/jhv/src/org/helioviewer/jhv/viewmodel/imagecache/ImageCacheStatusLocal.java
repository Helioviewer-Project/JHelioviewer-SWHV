package org.helioviewer.jhv.viewmodel.imagecache;

public class ImageCacheStatusLocal implements ImageCacheStatus {

    protected final int maxFrame;

    public ImageCacheStatusLocal(int _maxFrame) {
        maxFrame = _maxFrame;
    }

    @Override
    public CacheStatus getVisibleStatus(int frame) {
        return CacheStatus.COMPLETE;
    }

}
