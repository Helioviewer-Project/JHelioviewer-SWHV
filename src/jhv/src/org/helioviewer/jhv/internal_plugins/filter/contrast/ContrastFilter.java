package org.helioviewer.jhv.internal_plugins.filter.contrast;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.filter.GLFilter;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

/**
 * Filter for enhancing the contrast of the image.
 *
 * <p>
 * It uses the following formula:
 *
 * <p>
 * p_res(x,y) = 255 * (0.5 * sign(2x/255 - 1) * abs(2x/255 - 1)^(1.5^c) + 0.5)
 *
 * <p>
 * Here, p_res means the resulting pixel, p_in means the original input pixel
 * and contrast the parameter used.
 *
 * <p>
 * Since this is a point operation, it is optimized using a lookup table filled
 * by precomputing the output value for every possible input value. The actual
 * filtering is performed by using that lookup table.
 *
 * <p>
 * The output of the filter always has the same image format as the input.
 *
 * <p>
 * This filter supports software rendering as well as rendering in OpenGL2.
 *
 * @author Markus Langenberg
 */
public class ContrastFilter extends AbstractFilter implements GLFilter {

    private ContrastPanel panel;

    private float contrast = 0.0f;
    private final ContrastShader shader = new ContrastShader();

    /**
     * Sets the corresponding contrast panel.
     *
     * @param panel
     *            Corresponding panel.
     */
    void setPanel(ContrastPanel panel) {
        this.panel = panel;
        panel.setValue(contrast);
    }

    /**
     * Sets the contrast parameter.
     *
     * @param newContrast
     *            New contrast parameter.
     */
    void setContrast(float newContrast) {
        if (contrast == newContrast) {
            return;
        }
        contrast = newContrast;
        ShaderFactory.setContrast(contrast);
        notifyAllListeners();
    }

    /**
     * Fragment shader for enhancing the contrast.
     */
    private class ContrastShader extends GLFragmentShaderProgram {

        @Override
        public void bind(GL2 gl) {
            gl.glBindProgramARB(GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.getFragmentId());
            ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.contrastParamRef, ShaderFactory.contrastParamFloat);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyGL(GL2 gl) {
        shader.bind(gl);
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
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        setContrast(Float.parseFloat(state));
        panel.setValue(contrast);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return Float.toString(contrast);
    }

}
