package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.cache.DateTimeCache;
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
     * Fires a ChangeEvent into the view chain.
     * 
     * @param aEvent
     *            ChangeEvent to fire
     */
    public void fireChangeEvent(ChangeEvent aEvent);

    /**
     * Returns the image cache status.
     * 
     * @return image cache status
     */
    public ImageCacheStatus getImageCacheStatus();

    /**
     * Returns the date time cache, extracted from the meta data.
     * 
     * @return date time cache
     */
    public DateTimeCache getDateTimeCache();
}
