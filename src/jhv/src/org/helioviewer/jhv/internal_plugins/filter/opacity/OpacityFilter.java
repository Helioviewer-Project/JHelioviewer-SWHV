package org.helioviewer.jhv.internal_plugins.filter.opacity;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.filter.GLFilter;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

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
public class OpacityFilter extends AbstractFilter implements GLFilter {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private float opacity;
    private final OpacityShader shader = new OpacityShader();
    private OpacityPanel panel;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    public OpacityFilter(float initialOpacity) {
        opacity = initialOpacity;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This filter is a major filter.
     */
    @Override
    public boolean isMajorFilter() {
        return true;
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
     * Fragment shader setting the opacity.
     */
    private class OpacityShader extends GLFragmentShaderProgram {

        /**
         * Sets the new alpha value.
         *
         * @param gl
         *            Valid reference to the current gl object
         * @param alpha
         *            Alpha value
         */
        private void setAlpha(GL2 gl, float alpha) {
            ShaderFactory.setAlpha(alpha);
        }

        @Override
        public void bind(GL2 gl) {
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyGL(GL2 gl) {
        shader.setAlpha(gl, opacity);
        shader.bind(gl);
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
