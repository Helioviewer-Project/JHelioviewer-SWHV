package org.helioviewer.viewmodel.imagecache;

/**
 * Implementation of JP2CacheStatus for local movies.
 * 
 * Since the image data is always completely available for local image, this
 * implementation just handles the meta data status.
 * 
 * @author Markus Langenberg
 */
public class LocalImageCacheStatus implements ImageCacheStatus {

    private final int maxFrameNumber;
    private final CacheStatus[] imageStatus;

    /**
     * Default constructor.
     */
    public LocalImageCacheStatus(int _maxFrameNumber) {
        maxFrameNumber = _maxFrameNumber;
        imageStatus = new CacheStatus[maxFrameNumber + 1];
        java.util.Arrays.fill(imageStatus, CacheStatus.COMPLETE);
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

    @Override
    public CacheStatus[] getImageStatus() {
        return imageStatus;
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
