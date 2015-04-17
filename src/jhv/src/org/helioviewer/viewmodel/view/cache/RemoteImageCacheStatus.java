package org.helioviewer.viewmodel.view.cache;

import java.util.concurrent.locks.ReentrantLock;

import org.helioviewer.jhv.gui.UIViewListenerDistributor;
import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason;
import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason.CacheType;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.MovieView;

/**
 * Implementation of JP2CacheStatus for remote movies.
 *
 * @author Markus Langenberg
 *
 */
public class RemoteImageCacheStatus implements ImageCacheStatus {

    private final MovieView parent;
    private final CacheStatus[] imageStatus;
    private int imagePartialUntil = -1;
    private int imageCompleteUntil = -1;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Default constructor
     *
     * @param _parent
     *            JP2Image, whose cache status is managed
     */
    public RemoteImageCacheStatus(MovieView _parent) {
        parent = _parent;
        imageStatus = new CacheStatus[parent.getMaximumFrameNumber() + 1];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {
        lock.lock();
        try {
            if (imageStatus[compositionLayer] == newStatus) {
                return;
            }

            // PARTIAL
            if (compositionLayer >= imagePartialUntil && newStatus == CacheStatus.PARTIAL && imageStatus[compositionLayer] != CacheStatus.COMPLETE) {
                imageStatus[compositionLayer] = CacheStatus.PARTIAL;

                int tempImagePartialUntil = 0;
                while (tempImagePartialUntil <= parent.getMaximumFrameNumber() && (imageStatus[tempImagePartialUntil] == CacheStatus.PARTIAL || imageStatus[tempImagePartialUntil] == CacheStatus.COMPLETE)) {
                    tempImagePartialUntil++;
                }
                tempImagePartialUntil--;

                if (tempImagePartialUntil > imagePartialUntil) {
                    imagePartialUntil = tempImagePartialUntil;

                    ChangeEvent changeEvent = new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.PARTIAL, imagePartialUntil));
                    UIViewListenerDistributor.getSingletonInstance().viewChanged(null, changeEvent);
                }
            // COMPLETE
            } else if (compositionLayer >= imageCompleteUntil && newStatus == CacheStatus.COMPLETE) {
                imageStatus[compositionLayer] = CacheStatus.COMPLETE;

                int tempImageCompleteUntil = 0;
                while (tempImageCompleteUntil <= parent.getMaximumFrameNumber() && imageStatus[tempImageCompleteUntil] == CacheStatus.COMPLETE) {
                    tempImageCompleteUntil++;
                }
                tempImageCompleteUntil--;

                if (tempImageCompleteUntil > imageCompleteUntil) {
                    imageCompleteUntil = tempImageCompleteUntil;
                    if (imagePartialUntil < imageCompleteUntil) {
                        imagePartialUntil = imageCompleteUntil;
                    }

                    ChangeEvent changeEvent = new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.COMPLETE, imageCompleteUntil));
                    UIViewListenerDistributor.getSingletonInstance().viewChanged(null, changeEvent);
                }
            // HEADER
            } else if (newStatus == CacheStatus.HEADER && imageStatus[compositionLayer] == null) {
                imageStatus[compositionLayer] = CacheStatus.HEADER;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void downgradeImageStatus(int compositionLayer) {
        lock.lock();
        try {
            if (imageStatus[compositionLayer] != CacheStatus.COMPLETE) {
                return;
            }

            imageStatus[compositionLayer] = CacheStatus.PARTIAL;

            int tempImageCompleteUntil = 0;
            while (tempImageCompleteUntil <= parent.getMaximumFrameNumber() && imageStatus[tempImageCompleteUntil] == CacheStatus.COMPLETE) {
                tempImageCompleteUntil++;
            }

            if (tempImageCompleteUntil > 0) {
                tempImageCompleteUntil--;
            }

            if (tempImageCompleteUntil < imageCompleteUntil) {
                imageCompleteUntil = tempImageCompleteUntil;

                ChangeEvent changeEvent = new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.COMPLETE, imageCompleteUntil));
                UIViewListenerDistributor.getSingletonInstance().viewChanged(null, changeEvent);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheStatus getImageStatus(int compositionLayer) {
        lock.lock();
        CacheStatus res = imageStatus[compositionLayer];
        lock.unlock();

        return res;
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
