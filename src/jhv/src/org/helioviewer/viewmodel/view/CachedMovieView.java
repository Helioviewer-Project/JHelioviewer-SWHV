package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;

/**
 * View to provide data about cached information
 *
 * Mainly, this view is used for movies. It provides information about the cache
 * status of the image data as well as the meta data.
 *
 * @author Markus Langenberg
 *
 */
public interface CachedMovieView extends MovieView {

    /**
     * Returns the image cache status.
     *
     * @return image cache status
     */
    public ImageCacheStatus getImageCacheStatus();

}
