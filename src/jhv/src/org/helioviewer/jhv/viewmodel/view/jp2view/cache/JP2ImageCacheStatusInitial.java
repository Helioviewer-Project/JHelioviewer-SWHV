package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;

public class JP2ImageCacheStatusInitial implements JP2ImageCacheStatus {

    private CacheStatus imageStatus;

    @Override
    public void setVisibleStatus(int frame, CacheStatus newStatus) {
        if (frame != 0)
            throw new IllegalArgumentException();
        imageStatus = newStatus;
    }

    @Override
    public CacheStatus getVisibleStatus(int frame) {
        if (frame != 0)
            throw new IllegalArgumentException();
        return imageStatus;
    }

    @Override
    public void downgradeVisibleStatus(int level) {
    }

    @Override
    public int getImageCachedPartiallyUntil() {
        return -1;
    }

    @Override
    public ResolutionSet getResolutionSet(int frame) {
        return null;
    }

    @Override
    public boolean levelComplete(int level) {
        return false;
    }

    @Override
    public boolean imageComplete(int frame, int level) {
        return false;
    }

    @Override
    public void setImageComplete(int frame, int level) {
    }

}
