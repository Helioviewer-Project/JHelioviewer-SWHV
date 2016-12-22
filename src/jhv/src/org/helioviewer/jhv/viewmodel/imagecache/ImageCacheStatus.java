package org.helioviewer.jhv.viewmodel.imagecache;

public interface ImageCacheStatus {

    enum CacheStatus {
        PARTIAL, COMPLETE
    }

    void setVisibleStatus(int frame, CacheStatus newStatus);

    CacheStatus getVisibleStatus(int frame);

}
