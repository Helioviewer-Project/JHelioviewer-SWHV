package org.helioviewer.jhv.view.j2k;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import kdu_jni.KduException;

import org.helioviewer.jhv.Log;

interface CompletionLevel {

    int getPartialUntil();

    ResolutionSet getResolutionSet(int frame);

    boolean isComplete(int level);

    @Nullable
    AtomicBoolean getFrameStatus(int frame, int level);

    void setFrameComplete(int frame, int level) throws KduException;

    void setFramePartial(int frame) throws KduException;

    class Local implements CompletionLevel {

        private static final AtomicBoolean full = new AtomicBoolean(true);
        private final ResolutionSet[] resolutionSet;
        private final int maxFrame;

        Local(KakaduSource source, int _maxFrame) throws KduException {
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
        public void setFrameComplete(int frame, int level) {
        }

        @Override
        public void setFramePartial(int frame) {
        }

    }

    class Remote implements CompletionLevel {

        private final KakaduSource source;
        private final int maxFrame;
        private final ResolutionSet[] resolutionSet;

        private int partialUntil = 0;

        Remote(KakaduSource _source, int _maxFrame) throws KduException {
            source = _source;
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
                Log.error("resolutionSet[" + frame + "] is null"); // never happened?
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
        public void setFrameComplete(int frame, int level) throws KduException {
            if (fullyComplete)
                return;

            setFramePartial(frame);
            if (resolutionSet[frame] != null)
                resolutionSet[frame].setComplete(level);
        }

        @Override
        public void setFramePartial(int frame) throws KduException {
            if (resolutionSet[frame] == null) {
                resolutionSet[frame] = source.getResolutionSet(frame);
            }
        }

    }
}
