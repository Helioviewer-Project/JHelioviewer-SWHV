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
 * 
 * @author Markus Langenberg
 * 
 */
public interface ImageCacheStatus {

    enum CacheStatus {
        HEADER, PARTIAL, COMPLETE
    }

    /**
     * Sets the image cache status of one composition layer.
     *
     * @param compositionLayer
     *            Layer, whose status has changed
     * @param newStatus
     *            New image data cache status
     */
    void setImageStatus(int compositionLayer, CacheStatus newStatus);

    /**
     * Downgrades the status from complete to partial, if necessary.
     * 
     * This function may be called during zooming or panning, when entering
     * areas that have not been loaded so far.
     */
    void downgradeImageStatus(int startFrame, int endFrame);

    /**
     * Returns the image cache status of the given layer.
     * 
     * @param compositionLayer
     *            Layer to get the image cache status for
     * @return Image cache status
     */
    CacheStatus getImageStatus(int compositionLayer);

    /**
     * Returns the highest frame until which the status is at least PARTIAL.
     * 
     * @return highest frame until which the status is at least PARTIAL.
     */
    int getImageCachedPartiallyUntil();

}
