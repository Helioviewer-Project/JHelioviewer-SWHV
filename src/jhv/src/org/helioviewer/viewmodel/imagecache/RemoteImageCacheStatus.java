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
    public RemoteImageCacheStatus(View _parent) {
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
                    updateUI(parent, false, imagePartialUntil);
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
                    updateUI(parent, true, imageCompleteUntil);
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
                updateUI(parent, true, imageCompleteUntil);
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

    private static void updateUI(View view, boolean complete, int until) {
        EventQueue.invokeLater(new Runnable() {
            private View view;
            private boolean complete;
            private int until;

            @Override
            public void run() {
                MoviePanel.cacheStatusChanged(view, complete, until);
            }

            public Runnable init(View _view, boolean _complete, int _until) {
                view = _view;
                complete = _complete;
                until = _until;
                return this;
            }
        }.init(view, complete, until));
    }

}
