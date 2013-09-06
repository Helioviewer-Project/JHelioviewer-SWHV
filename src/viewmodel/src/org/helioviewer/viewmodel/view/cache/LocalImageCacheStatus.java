package org.helioviewer.viewmodel.view.cache;

import org.helioviewer.viewmodel.view.MovieView;

/**
 * Implementation of JP2CacheStatus for local movies.
 * 
 * Since the image data is always completely available for local image, this
 * implementation just handles the meta data status.
 * 
 * @author Markus Langenberg
 */
public class LocalImageCacheStatus implements ImageCacheStatus {

    private MovieView parent;

    /**
     * Default constructor.
     */
    public LocalImageCacheStatus(MovieView _parent) {
        parent = _parent;
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, always returns COMPLETE.
     */
    public CacheStatus getImageStatus(int compositionLayer) {
        return CacheStatus.COMPLETE;
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, does nothing.
     */
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, does nothing.
     */
    public void downgradeImageStatus(int compositionLayer) {
    }

    /**
     * {@inheritDoc}
     */
    public int getImageCachedPartiallyUntil() {
        return parent.getMaximumFrameNumber();
    }

    /**
     * {@inheritDoc}
     */
    public int getImageCachedCompletelyUntil() {
        return parent.getMaximumFrameNumber();
    }
}
