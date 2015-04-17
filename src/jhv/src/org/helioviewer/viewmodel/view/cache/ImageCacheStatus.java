package org.helioviewer.viewmodel.view.cache;

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

    public enum CacheStatus {
        HEADER, PARTIAL, COMPLETE
    };

    /**
     * Sets the image cache status of one composition layer.
     * 
     * If necessary, fires a
     * {@link org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason}.
     * 
     * @param compositionLayer
     *            Layer, whose status has changed
     * @param newStatus
     *            New image data cache status
     */
    public void setImageStatus(int compositionLayer, CacheStatus newStatus);

    /**
     * Downgrades the status from complete to partial, if necessary.
     * 
     * This function may be called during zooming or panning, when entering
     * areas that have not been loaded so far.
     * 
     * @param compositionLayer
     *            Layer to downgrade.
     */
    public void downgradeImageStatus(int compositionLayer);

    /**
     * Returns the image cache status of the given layer.
     * 
     * @param compositionLayer
     *            Layer to get the image cache status for
     * @return Image cache status
     */
    public CacheStatus getImageStatus(int compositionLayer);

    /**
     * Returns the highest frame, until which the status is at least PARTIAL.
     * 
     * @return highest frame, until which the status is at least PARTIAL.
     */
    public int getImageCachedPartiallyUntil();

    /**
     * Returns the highest frame, until which the status is COMPLETE.
     * 
     * @return highest frame, until which the status is COMPLETE.
     */
    public int getImageCachedCompletelyUntil();
}
