package org.helioviewer.jhv.internal_plugins.filter.opacity;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.view.opengl.shader.GLSLShader;

/**
 * Filter for changing the opacity of an image.
 *
 * <p>
 * The output of the filter always is an ARGB image, since that is currently the
 * only format supporting an alpha channel. Thus, this filter should be applied
 * as late as possible.
 *
 * <p>
 * This filter supports software rendering as well as rendering in OpenGL2.
 *
 * @author Markus Langenberg
 *
 */
public class OpacityFilter extends AbstractFilter implements Filter {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private float opacity;
    private OpacityPanel panel;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    public OpacityFilter(float initialOpacity) {
        opacity = initialOpacity;
    }

    /**
     * Sets the corresponding opacity panel.
     *
     * @param panel
     *            Corresponding panel.
     */
    void setPanel(OpacityPanel panel) {
        this.panel = panel;
        panel.setValue(opacity);
    }

    /**
     * Sets the opacity.
     *
     * This function does not the slider, thus should only be called by the
     * slider itself. Otherwise, use {@link #setOpacityExternal(float)}.
     *
     * @param newOpacity
     *            New opacity, value has to be within [0, 1]
     */
    void setOpacity(float newOpacity) {
        if (opacity == newOpacity) {
            return;
        }
        opacity = newOpacity;
        notifyAllListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyGL(GL2 gl) {
        GLSLShader.setAlpha(opacity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        setOpacity(Float.parseFloat(state));
        panel.setValue(opacity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return Float.toString(opacity);
    }

}
