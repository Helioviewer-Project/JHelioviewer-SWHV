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
    private boolean rebuildTable = true;
    private final ContrastShader shader = new ContrastShader();

    private byte[] contrastTable8 = null;
    private short[] contrastTable16 = null;

    private boolean forceRefilter = false;

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
        rebuildTable = true;
        notifyAllListeners();
    }

    /**
     * Internal function for building the lookup table for 8-bit input data.
     */
    private void buildTable8() {
        if (contrastTable8 == null) {
            contrastTable8 = new byte[0x100];
        }

        float N = 0xFF;
        for (int i = 0; i < 0x100; i++) {
            int v = (int) (N * (0.5f * Math.signum(2 * i / N - 1) * Math.pow(Math.abs(2 * i / N - 1), Math.pow(1.5, -contrast)) + 0.5f));
            contrastTable8[i] = (byte) v;
        }

        rebuildTable = false;
    }

    /**
     * Internal function for building the lookup table for 16-bit input data.
     */
    private void buildTable16(int bitDepth) {
        int maxValue = 1 << bitDepth;

        if (contrastTable16 == null) {
            contrastTable16 = new short[maxValue];
        }

        float N = maxValue - 1;
        for (int i = 0; i < maxValue; i++) {
            int v = (int) (N * (0.5f * Math.signum(2 * i / N - 1) * Math.pow(Math.abs(2 * i / N - 1), Math.pow(1.5, -contrast)) + 0.5f));
            contrastTable16[i] = (short) v;
        }

        rebuildTable = false;
    }

    /**
     * Fragment shader for enhancing the contrast.
     */
    private class ContrastShader extends GLFragmentShaderProgram {

        private void setContrast(GL2 gl, float contrast) {
            ShaderFactory.setContrast(contrast);
        }

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
        shader.setContrast(gl, contrast);
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
    public void forceRefilter() {
        forceRefilter = true;
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
