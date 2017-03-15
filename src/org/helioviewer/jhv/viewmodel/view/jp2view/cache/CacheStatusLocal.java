package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import java.util.concurrent.atomic.AtomicBoolean;

import kdu_jni.KduException;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduEngine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;

public class CacheStatusLocal implements CacheStatus {

    private static final AtomicBoolean full = new AtomicBoolean(true);
    private final ResolutionSet[] resolutionSet;
    private final int maxFrame;

    public CacheStatusLocal(KakaduEngine engine, int _maxFrame) throws KduException {
        maxFrame = _maxFrame;
        resolutionSet = new ResolutionSet[maxFrame + 1];
        for (int i = 0; i <= maxFrame; ++i) {
            resolutionSet[i] = KakaduHelper.getResolutionSet(engine.getCompositor(), i);
            resolutionSet[i].setComplete(0);
        }
    }

    @Override
    public int getPartialUntil() {
        return maxFrame;
    }

    @Override
    public ResolutionSet getResolutionSet(int frame) {
        return resolutionSet[frame];
    }

    @Override
    public boolean isComplete(int level) {
        return true;
    }

    @Override
    public AtomicBoolean getFrameStatus(int frame, int level) {
        return full;
    }

    @Override
    public void setFrameComplete(int frame, int level) {
    }

    @Override
    public void setFramePartial(int frame) {
    }

}
