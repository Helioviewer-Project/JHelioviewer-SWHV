package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import kdu_jni.KduException;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;

public class JP2ImageCacheStatusRemote implements JP2ImageCacheStatus {

    private final int maxFrameNumber;
    private final ResolutionSet[] resolutionSet;

    // accessed from J2KReader, read also from EDT by MoviePanel, for the latter not very important if values are consistent
    private final CacheStatus[] imageStatus;
    private int imagePartialUntil = -1;

    public JP2ImageCacheStatusRemote(Kdu_region_compositor compositor, int _maxFrameNumber) throws KduException {
        maxFrameNumber = _maxFrameNumber;
        imageStatus = new CacheStatus[maxFrameNumber + 1];

        resolutionSet = new ResolutionSet[maxFrameNumber + 1];
        resolutionSet[0] = KakaduHelper.getResolutionSet(compositor, 0);
        for (int i = 1; i <= maxFrameNumber; ++i) {
            resolutionSet[i] = resolutionSet[0];
        }
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

    @Override
    public ResolutionSet getResolutionSet(int compositionLayer) {
        return resolutionSet[compositionLayer];
    }

}
