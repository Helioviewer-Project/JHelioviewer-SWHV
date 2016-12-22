package org.helioviewer.jhv.viewmodel.imagecache;

import java.util.concurrent.atomic.AtomicBoolean;

public class ImageCacheStatusLocal implements ImageCacheStatus {

    private static final AtomicBoolean full = new AtomicBoolean(true);

    @Override
    public AtomicBoolean getVisibleStatus(int frame) {
        return full;
    }

}
