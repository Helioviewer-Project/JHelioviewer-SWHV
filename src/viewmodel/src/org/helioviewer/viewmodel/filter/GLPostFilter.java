package org.helioviewer.viewmodel.filter;

import javax.media.opengl.GL;

/**
 * Filter which has to perform some commands after rendering
 * 
 * <p>
 * In addition to applyGL, which is called before rendering, this filter also
 * has a second function, postApplyGL, which is called after the rendering took
 * place. This might be useful for performing some clean ups.
 * 
 * @author Markus Langenberg
 */
public interface GLPostFilter extends GLFilter {

    /**
     * Is called after the rendering took place.
     * 
     * Usually, this should be used for cleaning up.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public void postApplyGL(GL gl);
}
