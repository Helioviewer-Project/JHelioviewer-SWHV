package org.helioviewer.viewmodel.filter;

import javax.media.opengl.GL;

/**
 * Filter which supports in OpenGL rendering mode.
 * 
 * <p>
 * If possible, an OpenGL implementation should be provided for every filter,
 * since the performance of OpenGL accelerated filters usually is much higher
 * than pure software implementations.
 * 
 * <p>
 * The applyGL function will be called on every rendering cycle when rendering
 * in OpenGL mode.
 * 
 * <p>
 * In software mode, this interface has no effect. See {@link StandardFilter}
 * for filtering in software mode.
 * 
 * <p>
 * For further information about rendering in OpenGL, see
 * {@link org.helioviewer.viewmodel.view.opengl.GLView}.
 * 
 * @author Markus Langenberg
 * 
 */
public interface GLFilter extends Filter {

    /**
     * Calls the OpenGL commands necessary for applying the filter.
     * 
     * Usually, this includes binding the shaders performing the filter
     * operation.
     * 
     * <p>
     * This function only is called before the rendering takes place. To be able
     * to also perform commands after rendering, implement {@link GLPostFilter}
     * as well.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public void applyGL(GL gl);
}
