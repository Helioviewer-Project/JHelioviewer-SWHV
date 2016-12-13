package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import kdu_jni.KduException;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;

public class JP2ImageCacheStatusRemote implements JP2ImageCacheStatus {

    private final int maxFrameNumber;
    private final ResolutionSet[] resolutionSet;
    private final Kdu_region_compositor compositor;

    // r/w image load, r/w J2KReader, r MoviePanel/EDT
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
    public void setImageStatus(int compositionLayer, int level, CacheStatus newStatus) {
        if (resolutionSet[compositionLayer] == null && newStatus == CacheStatus.PARTIAL) {
            try {
                resolutionSet[compositionLayer] = KakaduHelper.getResolutionSet(compositor, compositionLayer);
            } catch (KduException e) {
                e.printStackTrace();
            }
        }

        imageStatus[compositionLayer] = newStatus;
        if (newStatus == CacheStatus.COMPLETE && resolutionSet[compositionLayer] != null)
            resolutionSet[compositionLayer].setComplete(level);
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
        int i;
        for (i = Math.max(0, imagePartialUntil); i <= maxFrameNumber; i++) {
            if (imageStatus[i] != CacheStatus.PARTIAL && imageStatus[i] != CacheStatus.COMPLETE) {
                break;
            }
        }
        imagePartialUntil = i - 1;

        return imagePartialUntil;
    }

    @Override
    public ResolutionSet getResolutionSet(int compositionLayer) {
        if (resolutionSet[compositionLayer] == null) {
            Log.error("resolutionSet[" + compositionLayer + "] null"); // never happened?
            return resolutionSet[0];
        }
        return resolutionSet[compositionLayer];
    }

    @Override
    public boolean currentComplete() {
        for (int i = 0; i <= maxFrameNumber; i++) {
            if (imageStatus[i] != CacheStatus.COMPLETE)
                return false;
        }
        return true;
    }

    @Override
    public boolean levelComplete(int level) {
        if (fullyComplete)
            return true;

        for (int i = 0; i <= maxFrameNumber; i++) {
            if (resolutionSet[i] == null || !resolutionSet[i].getComplete(level))
                return false;
        }
        if (level == 0)
            fullyComplete = true;
        return true;
    }

    private boolean fullyComplete;

}
