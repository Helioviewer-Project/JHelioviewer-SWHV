package org.helioviewer.viewmodel.imagecache;

import java.awt.EventQueue;

import java.util.concurrent.locks.ReentrantLock;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.viewmodel.view.View;

/**
 * Implementation of JP2CacheStatus for remote movies.
 *
 * @author Markus Langenberg
 *
 */
public class RemoteImageCacheStatus implements ImageCacheStatus {

    private final View parent;
    private final int maxFrameNumber;
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
    public RemoteImageCacheStatus(View _parent, int _maxFrameNumber) {
        parent = _parent;
        maxFrameNumber = _maxFrameNumber;
        imageStatus = new CacheStatus[maxFrameNumber + 1];
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
                while (tempImagePartialUntil <= maxFrameNumber && (imageStatus[tempImagePartialUntil] == CacheStatus.PARTIAL || imageStatus[tempImagePartialUntil] == CacheStatus.COMPLETE)) {
                    tempImagePartialUntil++;
                }
                tempImagePartialUntil--;

                if (tempImagePartialUntil > imagePartialUntil) {
                    imagePartialUntil = tempImagePartialUntil;
                    updateUI(parent);
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
                    updateUI(parent);
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
            while (tempImageCompleteUntil <= maxFrameNumber && imageStatus[tempImageCompleteUntil] == CacheStatus.COMPLETE) {
                tempImageCompleteUntil++;
            }

            if (tempImageCompleteUntil > 0) {
                tempImageCompleteUntil--;
            }

            if (tempImageCompleteUntil < imageCompleteUntil) {
                imageCompleteUntil = tempImageCompleteUntil;
                updateUI(parent);
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

    private static void updateUI(View view) {
        EventQueue.invokeLater(new Runnable() {
            private View view;

            @Override
            public void run() {
                MoviePanel.cacheStatusChanged(view);
            }

            public Runnable init(View _view) {
                view = _view;
                return this;
            }
        }.init(view));
    }

}
