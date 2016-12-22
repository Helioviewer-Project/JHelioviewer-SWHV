package org.helioviewer.jhv.viewmodel.imagecache;

import java.util.concurrent.atomic.AtomicBoolean;

public interface ImageCacheStatus {

    AtomicBoolean getVisibleStatus(int frame);

}
