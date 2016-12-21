package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import kdu_jni.KduException;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatusLocal;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduEngine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;

public class JP2ImageCacheStatusLocal extends ImageCacheStatusLocal implements JP2ImageCacheStatus {

    private final ResolutionSet[] resolutionSet;

    public JP2ImageCacheStatusLocal(KakaduEngine engine, int _maxFrame) throws KduException {
        super(_maxFrame);

        resolutionSet = new ResolutionSet[maxFrame + 1];
        for (int i = 0; i <= maxFrame; ++i) {
            resolutionSet[i] = KakaduHelper.getResolutionSet(engine.getCompositor(), i);
            resolutionSet[i].setComplete(0);
        }
    }

    @Override
    public void downgradeVisibleStatus(int level) {
    }

    @Override
    public int getImageCachedPartiallyUntil() {
        return maxFrame;
    }

    @Override
    public ResolutionSet getResolutionSet(int frame) {
        return resolutionSet[frame];
    }

    @Override
    public boolean levelComplete(int level) {
        return true;
    }

    @Override
    public boolean frameLevelComplete(int frame, int level) {
        return true;
    }

    @Override
    public void setFrameLevelComplete(int frame, int level) {
    }

}
