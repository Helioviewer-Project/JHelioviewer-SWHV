package org.helioviewer.jhv.internal_plugins.filter.gammacorrection;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.filter.GLFilter;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

/**
 * Filter for applying gamma correction.
 *
 * <p>
 * It uses the following formula:
 *
 * <p>
 * p_res(x,y) = 255 * power( p_in(x,y) / 255, gamma)
 *
 * <p>
 * Here, p_res means the resulting pixel, p_in means the original input pixel
 * and gamma the gamma value used.
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
public class GammaCorrectionFilter extends AbstractFilter implements GLFilter {

    private GammaCorrectionPanel panel;

    private float gamma = 1.0f;

    /**
     * Sets the corresponding gamma correction panel.
     *
     * @param panel
     *            Corresponding panel.
     */
    void setPanel(GammaCorrectionPanel panel) {
        this.panel = panel;
        panel.setValue(gamma);
    }

    /**
     * Sets the gamma value.
     *
     * @param newGamma
     *            New gamma value.
     */
    void setGamma(float newGamma) {
        if (gamma == newGamma) {
            return;
        }
        gamma = newGamma;
        notifyAllListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyGL(GL2 gl) {
        ShaderFactory.setGamma(gamma);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        setGamma(Float.parseFloat(state));
        panel.setValue(gamma);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return Float.toString(gamma);
    }

}
