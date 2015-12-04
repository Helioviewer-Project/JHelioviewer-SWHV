package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;

public interface JP2ImageCacheStatus extends ImageCacheStatus {

    public ResolutionSet getResolutionSet(int compositionLayer);

}
