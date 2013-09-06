package org.helioviewer.viewmodel.view.cache;

import java.util.concurrent.locks.ReentrantLock;

import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason.CacheType;
import org.helioviewer.viewmodel.view.CachedMovieView;

/**
 * Implementation of JP2CacheStatus for remote movies.
 * 
 * @author Markus Langenberg
 * 
 */
public class RemoteImageCacheStatus implements ImageCacheStatus {

    private CachedMovieView parent;
    private CacheStatus[] imageStatus;
    private int imagePartialUntil = -1;
    private int imageCompleteUntil = -1;

    private ReentrantLock lock = new ReentrantLock();

    /**
     * Default constructor
     * 
     * @param _parent
     *            JP2Image, whose cache status is managed
     */
    public RemoteImageCacheStatus(CachedMovieView _parent) {
        parent = _parent;
        imageStatus = new CacheStatus[parent.getMaximumFrameNumber() + 1];
    }

    /**
     * {@inheritDoc}
     */
    public void setImageStatus(int compositionLayer, CacheStatus newStatus) {

        ChangeEvent changeEvent = null;
        lock.lock();
        try {
            if (imageStatus[compositionLayer] == newStatus)
                return;

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

                    if (imagePartialUntil <= parent.getDateTimeCache().getMetaStatus()) {
                        changeEvent = new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.PARTIAL, imagePartialUntil));
                    }
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

                    if (imageCompleteUntil <= parent.getDateTimeCache().getMetaStatus()) {
                        changeEvent = new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.COMPLETE, imageCompleteUntil));
                    }
                }

                // HEADER
            } else if (newStatus == CacheStatus.HEADER && imageStatus[compositionLayer] == null) {

                imageStatus[compositionLayer] = CacheStatus.HEADER;
            }
        } finally {
            lock.unlock();
        }
        if (changeEvent != null) {
            parent.fireChangeEvent(changeEvent);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void downgradeImageStatus(int compositionLayer) {
        ChangeEvent changeEvent = null;
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

                if (imageCompleteUntil <= parent.getDateTimeCache().getMetaStatus()) {
                    changeEvent = new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.COMPLETE, imageCompleteUntil));
                }
            }
        } finally {
            lock.unlock();
        }
        if (changeEvent != null) {
            parent.fireChangeEvent(changeEvent);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CacheStatus getImageStatus(int compositionLayer) {
        CacheStatus res = null;
        lock.lock();
        try {
            res = imageStatus[compositionLayer];
        } finally {
            lock.unlock();
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageCachedPartiallyUntil() {
        return imagePartialUntil;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageCachedCompletelyUntil() {
        return imageCompleteUntil;
    }
}
