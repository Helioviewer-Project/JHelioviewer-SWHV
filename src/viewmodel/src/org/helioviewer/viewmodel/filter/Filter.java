package org.helioviewer.viewmodel.filter;

import javax.media.opengl.GL2;

/**
 * Interface common for all filters.
 *
 * <p>
 * A filter represents an image processing operation, such as point operations
 * and convolutions.
 *
 * <p>
 * To actually apply the filter to an image within the view chain, it has to be
 * passed to a {@link org.helioviewer.viewmodel.view.FilterView}. Every time a
 * new image passes the FilterView, it will call the filter.
 *
 * @author Ludwig Schmidt
 */
public interface Filter {

    public void applyGL(GL2 gl);

}
