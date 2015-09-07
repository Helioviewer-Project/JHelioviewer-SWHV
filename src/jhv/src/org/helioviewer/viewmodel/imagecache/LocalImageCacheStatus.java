package org.helioviewer.viewmodel.imagecache;

import org.helioviewer.viewmodel.view.View;

/**
 * Implementation of JP2CacheStatus for local movies.
 * 
 * Since the image data is always completely available for local image, this
 * implementation just handles the meta data status.
 * 
 * @author Markus Langenberg
 */
public class LocalImageCacheStatus implements ImageCacheStatus {

    private final View parent;
    private final int maxFrameNumber;

    /**
     * Default constructor.
     */
    public LocalImageCacheStatus(View _parent, int _maxFrameNumber) {
        parent = _parent;
        maxFrameNumber = _maxFrameNumber;
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, always returns COMPLETE.
     */
    @Override
    public CacheStatus getImageStatus(int compositionLayer) {
        return CacheStatus.COMPLETE;
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, does nothing.
     */
    @Override
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, does nothing.
     */
    @Override
    public void downgradeImageStatus(int compositionLayer) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageCachedPartiallyUntil() {
        return maxFrameNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageCachedCompletelyUntil() {
        return maxFrameNumber;
    }

}
