package org.helioviewer.jhv.view.j2k.cache;

import java.util.concurrent.atomic.AtomicBoolean;

import kdu_jni.KduException;

import org.helioviewer.jhv.view.j2k.image.ResolutionSet;
import org.helioviewer.jhv.view.j2k.kakadu.KakaduSource;

public class CacheStatusLocal implements CacheStatus {

    private static final AtomicBoolean full = new AtomicBoolean(true);
    private final ResolutionSet[] resolutionSet;
    private final int maxFrame;

    public CacheStatusLocal(KakaduSource source, int _maxFrame) throws KduException {
        maxFrame = _maxFrame;
        resolutionSet = new ResolutionSet[maxFrame + 1];
        for (int i = 0; i <= maxFrame; ++i) {
            resolutionSet[i] = source.getResolutionSet(i);
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
    public void setFrameComplete(KakaduSource source, int frame, int level) {
    }

    @Override
    public void setFramePartial(KakaduSource source, int frame) {
    }

}
