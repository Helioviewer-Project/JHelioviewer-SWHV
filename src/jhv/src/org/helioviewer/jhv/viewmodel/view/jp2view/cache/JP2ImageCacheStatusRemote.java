package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import java.util.concurrent.atomic.AtomicBoolean;

import kdu_jni.KduException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduEngine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;

public class JP2ImageCacheStatusRemote implements JP2ImageCacheStatus {

    // r/w J2KReader, r MoviePanel/EDT

    private final int maxFrame;
    private final ResolutionSet[] resolutionSet;
    private KakaduEngine engine;

    private int imagePartialUntil = 0;

    public JP2ImageCacheStatusRemote(KakaduEngine _engine, int _maxFrame) throws KduException {
        maxFrame = _maxFrame;

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

    @Override
    public int getImageCachedPartiallyUntil() {
        int i;
        for (i = imagePartialUntil; i <= maxFrame; i++) {
            if (resolutionSet[i] == null)
                break;
        }
        imagePartialUntil = Math.max(0, i - 1);
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

    private boolean fullyComplete;
    private static final AtomicBoolean full = new AtomicBoolean(true);

    @Override
    public boolean levelComplete(int level) {
        if (fullyComplete)
            return true;

        for (int i = 0; i <= maxFrame; i++) {
            if (resolutionSet[i] == null)
                return false;
            AtomicBoolean status = resolutionSet[i].getComplete(level);
            if (status == null || !status.get())
                return false;
        }
        if (level == 0)
            fullyComplete = true;
        return true;
    }


    @Override
    public AtomicBoolean frameLevelComplete(int frame, int level) {
        if (fullyComplete)
            return full;
        if (resolutionSet[frame] == null)
            return null;
        return resolutionSet[frame].getComplete(level);
    }

    @Override
    public void setFrameLevelComplete(int frame, int level) {
        if (fullyComplete)
            return;

        setFrameLevelPartial(frame);
        if (resolutionSet[frame] != null)
            resolutionSet[frame].setComplete(level);
    }

    @Override
    public void setFrameLevelPartial(int frame) {
        if (resolutionSet[frame] == null) {
            try {
                resolutionSet[frame] = KakaduHelper.getResolutionSet(engine.getCompositor(), frame);
                destroyIfFull();
            } catch (KduException e) {
                e.printStackTrace();
            }
        }
    }

}
