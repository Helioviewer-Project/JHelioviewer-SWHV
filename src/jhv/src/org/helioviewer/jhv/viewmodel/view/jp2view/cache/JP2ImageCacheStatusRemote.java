package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import kdu_jni.KduException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduEngine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;

public class JP2ImageCacheStatusRemote implements JP2ImageCacheStatus {

    private final int maxFrame;
    private final ResolutionSet[] resolutionSet;
    private KakaduEngine engine;

    // r/w image load, r/w J2KReader, r MoviePanel/EDT
    private final CacheStatus[] imageStatus;
    private int imagePartialUntil = -1;

    public JP2ImageCacheStatusRemote(KakaduEngine _engine, int _maxFrame) throws KduException {
        maxFrame = _maxFrame;
        imageStatus = new CacheStatus[maxFrame + 1];

        engine = _engine;
        resolutionSet = new ResolutionSet[maxFrame + 1];
        resolutionSet[0] = KakaduHelper.getResolutionSet(engine.getCompositor(), 0);
        destroyIfFull();
    }

    private void destroyIfFull() {
        for (int i = 0; i <= maxFrame; i++) {
            if (resolutionSet[i] == null) {
                return;
            }
        }
        engine = null;
    }

    // not threadsafe
    @Override
    public void setVisibleStatus(int frame, CacheStatus newStatus) {
        imageStatus[frame] = newStatus;
        if (resolutionSet[frame] == null && newStatus == CacheStatus.PARTIAL) {
            try {
                resolutionSet[frame] = KakaduHelper.getResolutionSet(engine.getCompositor(), frame);
                destroyIfFull();
            } catch (KduException e) {
                e.printStackTrace();
            }
        }
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
            if (imageStatus[i] == CacheStatus.COMPLETE && (resolutionSet[i] == null || !resolutionSet[i].getComplete(level))) { //!
                imageStatus[i] = CacheStatus.PARTIAL;
            }
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
            if (resolutionSet[i] == null || !resolutionSet[i].getComplete(level)) {
                return false;
            }
        }
        if (level == 0)
            fullyComplete = true;
        return true;
    }

    private boolean fullyComplete;

    @Override
    public boolean frameLevelComplete(int frame, int level) {
        return resolutionSet[frame] != null && resolutionSet[frame].getComplete(level);
    }

    @Override
    public void setFrameLevelComplete(int frame, int level) {
        if (resolutionSet[frame] != null) {
            resolutionSet[frame].setComplete(level);
        }
    }

}
