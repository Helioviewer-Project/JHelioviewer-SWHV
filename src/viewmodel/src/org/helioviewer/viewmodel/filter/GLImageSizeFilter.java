package org.helioviewer.viewmodel.filter;

/**
 * Filter which uses the image size in OpenGL rendering mode.
 * 
 * <p>
 * Although fragment shaders only operate on a single pixel, the size of the
 * whole image might be interesting. To receive this information, filters have
 * to implement this interface. The
 * {@link org.helioviewer.viewmodel.view.opengl.GLFilterView} will recognize
 * this and provide the information.
 * 
 * <p>
 * This is not necessary in software rendering mode, since the size of the image
 * is part of the image data object.
 * 
 * @author Markus Langenberg
 * 
 */
public interface GLImageSizeFilter extends GLFilter {

    /**
     * Sets the image size.
     * 
     * Usually, this function will be called by the
     * {@link org.helioviewer.viewmodel.view.opengl.GLFilterView}
     * 
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     */
    public void setImageSize(int width, int height);

}
