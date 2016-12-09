package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;

public class JP2ImageCacheStatusInitial implements JP2ImageCacheStatus {

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
    public void downgradeImageStatus(int startFrame, int endFrame) {
    }

    @Override
    public int getImageCachedPartiallyUntil() {
        return -1;
    }

    @Override
    public ResolutionSet getResolutionSet(int compositionLayer) {
        return null;
    }

}
