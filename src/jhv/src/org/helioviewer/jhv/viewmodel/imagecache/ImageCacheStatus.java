package org.helioviewer.jhv.viewmodel.imagecache;

public interface ImageCacheStatus {

    enum CacheStatus {
        PARTIAL, COMPLETE
    }

    CacheStatus getVisibleStatus(int frame);

}
