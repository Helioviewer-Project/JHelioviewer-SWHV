package org.helioviewer.jhv.viewmodel.imagecache;

public class ImageCacheStatusInitial implements ImageCacheStatus {

    private CacheStatus imageStatus;

    @Override
    public CacheStatus getImageStatus(int compositionLayer) {
        if (compositionLayer != 0)
            throw new IllegalArgumentException();
        return imageStatus;
    }

    @Override
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
        if (compositionLayer != 0)
            throw new IllegalArgumentException();
        imageStatus = newStatus;
    }

    @Override
    public void downgradeImageStatus(int compositionLayer) {
    }

    @Override
    public int getImageCachedPartiallyUntil() {
        return -1;
    }

}
