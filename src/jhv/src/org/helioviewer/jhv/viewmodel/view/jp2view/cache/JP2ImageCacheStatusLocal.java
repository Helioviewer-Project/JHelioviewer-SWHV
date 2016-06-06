package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import kdu_jni.KduException;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatusLocal;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;

public class JP2ImageCacheStatusLocal extends ImageCacheStatusLocal implements JP2ImageCacheStatus {

    private final ResolutionSet[] resolutionSet;

    public JP2ImageCacheStatusLocal(Kdu_region_compositor compositor, int _maxFrameNumber) throws KduException {
        super(_maxFrameNumber);

        resolutionSet = new ResolutionSet[_maxFrameNumber + 1];
        for (int i = 0; i <= _maxFrameNumber; ++i) {
            resolutionSet[i] = KakaduHelper.getResolutionSet(compositor, i);
        }
    }

    @Override
    public ResolutionSet getResolutionSet(int compositionLayer) {
        return resolutionSet[compositionLayer];
    }

}
