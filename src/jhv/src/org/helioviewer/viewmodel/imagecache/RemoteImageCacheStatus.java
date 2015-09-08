package org.helioviewer.viewmodel.imagecache;

/**
 * Implementation of ImageCacheStatus for remote movies
 *
 * @author Markus Langenberg
 *
 */
public class RemoteImageCacheStatus implements ImageCacheStatus {

    // accessed from LoadRemote (JP2Image.initRemote()) and J2KReader threads, never concurrently, no need to lock
    private final CacheStatus[] imageStatus;

    private final int maxFrameNumber;
    private int imagePartialUntil = -1;
    private int imageCompleteUntil = -1;

    /**
     * Default constructor
     *
     * @param _parent
     *            JP2Image whose cache status is managed
     */
    public RemoteImageCacheStatus(int _maxFrameNumber) {
        maxFrameNumber = _maxFrameNumber;
        imageStatus = new CacheStatus[maxFrameNumber + 1];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
        if (imageStatus[compositionLayer] == newStatus) {
            return;
        }

        // PARTIAL
        if (compositionLayer >= imagePartialUntil && newStatus == CacheStatus.PARTIAL && imageStatus[compositionLayer] != CacheStatus.COMPLETE) {
            imageStatus[compositionLayer] = CacheStatus.PARTIAL;

            int tempImagePartialUntil = 0;
            while (tempImagePartialUntil <= maxFrameNumber && (imageStatus[tempImagePartialUntil] == CacheStatus.PARTIAL || imageStatus[tempImagePartialUntil] == CacheStatus.COMPLETE)) {
                tempImagePartialUntil++;
            }
            tempImagePartialUntil--;

            if (tempImagePartialUntil > imagePartialUntil) {
                imagePartialUntil = tempImagePartialUntil;
            }
        // COMPLETE
        } else if (compositionLayer >= imageCompleteUntil && newStatus == CacheStatus.COMPLETE) {
            imageStatus[compositionLayer] = CacheStatus.COMPLETE;

            int tempImageCompleteUntil = 0;
            while (tempImageCompleteUntil <= maxFrameNumber && imageStatus[tempImageCompleteUntil] == CacheStatus.COMPLETE) {
                tempImageCompleteUntil++;
            }
            tempImageCompleteUntil--;

            if (tempImageCompleteUntil > imageCompleteUntil) {
                imageCompleteUntil = tempImageCompleteUntil;
                if (imagePartialUntil < imageCompleteUntil) {
                    imagePartialUntil = imageCompleteUntil;
                }
            }
        // HEADER
        } else if (newStatus == CacheStatus.HEADER && imageStatus[compositionLayer] == null) {
            imageStatus[compositionLayer] = CacheStatus.HEADER;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void downgradeImageStatus(int compositionLayer) {
        if (imageStatus[compositionLayer] != CacheStatus.COMPLETE) {
            return;
        }

        imageStatus[compositionLayer] = CacheStatus.PARTIAL;

        int tempImageCompleteUntil = 0;
        while (tempImageCompleteUntil <= maxFrameNumber && imageStatus[tempImageCompleteUntil] == CacheStatus.COMPLETE) {
            tempImageCompleteUntil++;
        }

        if (tempImageCompleteUntil > 0) {
            tempImageCompleteUntil--;
        }

        if (tempImageCompleteUntil < imageCompleteUntil) {
            imageCompleteUntil = tempImageCompleteUntil;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheStatus getImageStatus(int compositionLayer) {
        return imageStatus[compositionLayer];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageCachedPartiallyUntil() {
        return imagePartialUntil;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageCachedCompletelyUntil() {
        return imageCompleteUntil;
    }

}
