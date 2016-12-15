package org.helioviewer.jhv.viewmodel.imagecache;

/**
 * Interface for keeping track of the caching progress.
 * 
 * The interface keeps track of two different informations: The progress of the
 * image data and the progress of the meta data.
 * 
 * For the image data, different statuses possible: Only the header is cached, a
 * partial image information, which means the image can be shown, but not yet in
 * full quality, and a complete image information, which means the image can be
 * shown in full quality.
 */
public interface ImageCacheStatus {

    enum CacheStatus {
        HEADER, PARTIAL, COMPLETE
    }

    void setVisibleStatus(int frame, CacheStatus newStatus);

    CacheStatus getVisibleStatus(int frame);

}
