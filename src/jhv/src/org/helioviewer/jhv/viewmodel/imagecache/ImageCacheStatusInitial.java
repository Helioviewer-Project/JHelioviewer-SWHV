package org.helioviewer.jhv.viewmodel.imagecache;

public class ImageCacheStatusInitial implements ImageCacheStatus {

    private CacheStatus imageStatus;

    @Override
    public CacheStatus getImageStatus(int compositionLayer) {
        assert (compositionLayer == 0);
        return imageStatus;
    }

    @Override
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
        assert (compositionLayer == 0);
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
