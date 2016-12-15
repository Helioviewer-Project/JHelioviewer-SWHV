package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import kdu_jni.KduException;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;

public class JP2ImageCacheStatusRemote implements JP2ImageCacheStatus {

    private final int maxFrame;
    private final ResolutionSet[] resolutionSet;
    private final Kdu_region_compositor compositor;

    // r/w image load, r/w J2KReader, r MoviePanel/EDT
    private final CacheStatus[] imageStatus;
    private int imagePartialUntil = -1;

    public JP2ImageCacheStatusRemote(Kdu_region_compositor _compositor, int _maxFrame) throws KduException {
        maxFrame = _maxFrame;
        imageStatus = new CacheStatus[maxFrame + 1];

        compositor = _compositor;
        resolutionSet = new ResolutionSet[maxFrame + 1];
        resolutionSet[0] = KakaduHelper.getResolutionSet(compositor, 0);
    }

    // not threadsafe
    @Override
    public void setVisibleStatus(int frame, CacheStatus newStatus) {
        if (resolutionSet[frame] == null && newStatus == CacheStatus.PARTIAL) {
            try {
                resolutionSet[frame] = KakaduHelper.getResolutionSet(compositor, frame);
            } catch (KduException e) {
                e.printStackTrace();
            }
        }

        imageStatus[frame] = newStatus;
    }

    // not threadsafe
    @Override
    public CacheStatus getVisibleStatus(int frame) {
        return imageStatus[frame];
    }

    // not threadsafe
    @Override
    public void downgradeVisibleStatus(int level) {
        for (int i = 0; i <= maxFrame; i++) {
            if (imageStatus[i] == CacheStatus.COMPLETE && ((resolutionSet[i] == null || !resolutionSet[i].getComplete(level)))) //!
                imageStatus[i] = CacheStatus.PARTIAL;
        }
    }

    @Override
    public int getImageCachedPartiallyUntil() {
        int i;
        for (i = Math.max(0, imagePartialUntil); i <= maxFrame; i++) {
            if (imageStatus[i] != CacheStatus.PARTIAL && imageStatus[i] != CacheStatus.COMPLETE) {
                break;
            }
        }
        imagePartialUntil = i - 1;

        return imagePartialUntil;
    }

    @Override
    public ResolutionSet getResolutionSet(int frame) {
        if (resolutionSet[frame] == null) {
            Log.error("resolutionSet[" + frame + "] null"); // never happened?
            return resolutionSet[0];
        }
        return resolutionSet[frame];
    }

    @Override
    public boolean levelComplete(int level) {
        if (fullyComplete)
            return true;

        for (int i = 0; i <= maxFrame; i++) {
            if (resolutionSet[i] == null || !resolutionSet[i].getComplete(level))
                return false;
        }
        if (level == 0)
            fullyComplete = true;
        return true;
    }

    private boolean fullyComplete;

    @Override
    public boolean imageComplete(int frame, int level) {
        return resolutionSet[frame].getComplete(level);
    }

    @Override
    public void setImageComplete(int frame, int level) {
        resolutionSet[frame].setComplete(level);
    }

}
