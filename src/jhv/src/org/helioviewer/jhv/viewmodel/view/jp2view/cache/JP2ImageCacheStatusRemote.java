package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import kdu_jni.KduException;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;

public class JP2ImageCacheStatusRemote implements JP2ImageCacheStatus {

    private final int maxFrameNumber;
    private final ResolutionSet[] resolutionSet;
    private final Kdu_region_compositor compositor;

    // accessed from J2KReader, read also from EDT by MoviePanel, for the latter not very important if values are consistent
    private final CacheStatus[] imageStatus;
    private int imagePartialUntil = -1;

    public JP2ImageCacheStatusRemote(Kdu_region_compositor _compositor, int _maxFrameNumber) throws KduException {
        maxFrameNumber = _maxFrameNumber;
        imageStatus = new CacheStatus[maxFrameNumber + 1];

        compositor = _compositor;
        resolutionSet = new ResolutionSet[maxFrameNumber + 1];
        resolutionSet[0] = KakaduHelper.getResolutionSet(compositor, 0);
    }

    // not threadsafe
    @Override
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
        if (resolutionSet[compositionLayer] == null && newStatus == CacheStatus.PARTIAL) {
            try {
                resolutionSet[compositionLayer] = KakaduHelper.getResolutionSet(compositor, compositionLayer);
            } catch (KduException e) {
                e.printStackTrace();
            }
        }
        imageStatus[compositionLayer] = newStatus;
    }

    // not threadsafe
    @Override
    public void downgradeImageStatus(int startFrame, int endFrame) {
        startFrame = startFrame < 0 ? 0 : startFrame;
        endFrame = endFrame > maxFrameNumber ? maxFrameNumber : endFrame;

        for (int i = startFrame; i <= endFrame; i++) {
            if (imageStatus[i] == CacheStatus.COMPLETE) //!
                imageStatus[i] = CacheStatus.PARTIAL;
        }
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
        if (resolutionSet[compositionLayer] == null) // temporary
            return resolutionSet[0];
        return resolutionSet[compositionLayer];
    }

}
