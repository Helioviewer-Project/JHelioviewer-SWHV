package org.helioviewer.jhv.view.j2k.cache;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import kdu_jni.KduException;

import org.helioviewer.jhv.view.j2k.image.ResolutionSet;
import org.helioviewer.jhv.view.j2k.kakadu.KakaduSource;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheStatusRemote implements CacheStatus {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private final int maxFrame;
    private final ResolutionSet[] resolutionSet;

    private int partialUntil = 0;

    public CacheStatusRemote(KakaduSource source, int _maxFrame) throws KduException {
        maxFrame = _maxFrame;
        resolutionSet = new ResolutionSet[maxFrame + 1];
        resolutionSet[0] = source.getResolutionSet(0);
    }

    @Override
    public int getPartialUntil() {
        int i;
        for (i = partialUntil; i <= maxFrame; i++) {
            if (resolutionSet[i] == null)
                break;
        }
        partialUntil = Math.max(0, i - 1);
        return partialUntil;
    }

    @Override
    public ResolutionSet getResolutionSet(int frame) {
        if (resolutionSet[frame] == null) {
            LOGGER.log(Level.SEVERE, "resolutionSet[" + frame + "] null"); // never happened?
            return resolutionSet[0];
        }
        return resolutionSet[frame];
    }

    private boolean fullyComplete;
    private static final AtomicBoolean full = new AtomicBoolean(true);

    @Override
    public boolean isComplete(int level) {
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

    @Nullable
    @Override
    public AtomicBoolean getFrameStatus(int frame, int level) {
        if (fullyComplete)
            return full;
        if (resolutionSet[frame] == null)
            return null;
        return resolutionSet[frame].getComplete(level);
    }

    @Override
    public void setFrameComplete(KakaduSource source, int frame, int level) throws KduException {
        if (fullyComplete)
            return;

        setFramePartial(source, frame);
        if (resolutionSet[frame] != null)
            resolutionSet[frame].setComplete(level);
    }

    @Override
    public void setFramePartial(KakaduSource source, int frame) throws KduException {
        if (resolutionSet[frame] == null) {
            resolutionSet[frame] = source.getResolutionSet(frame);
        }
    }

}
