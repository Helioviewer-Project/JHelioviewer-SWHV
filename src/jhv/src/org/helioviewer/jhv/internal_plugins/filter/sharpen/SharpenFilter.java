package org.helioviewer.jhv.internal_plugins.filter.sharpen;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.filter.GLImageSizeFilter;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

/**
 * Filter for sharpen an image.
 *
 * <p>
 * This filter sharpens the image by applying the unsharp mask algorithm. It
 * uses the following formula:
 *
 * <p>
 * p_res(x,y) = (1 + a) * p_in(x,y) - a * p_low(x,y)
 *
 * <p>
 * Here, p_res means the resulting pixel, p_in means the original input pixel
 * and p_low the pixel of the lowpassed filtered original. As applying the
 * lowpass, the image is convoluted with the 3x3 Gauss-kernel:
 *
 * <p>
 * 1/16 * {{1, 2, 1}, {2, 4, 2}, {1, 2, 1}}
 *
 * <p>
 * If the weighting is zero, the input data stays untouched.
 *
 * <p>
 * The output of the filter always has the same image format as the input.
 *
 * <p>
 * This filter supports only software rendering, but there is an OpenGL
 * implementation in {@link SharpenGLFilter}. This is because the OpenGL
 * implementation may be invalid due to graphics card restrictions.
 *
 * @author Markus Langenberg
 *
 */
public class SharpenFilter extends AbstractFilter implements GLImageSizeFilter {

    // /////////////////////////
    // GENERAL //
    // /////////////////////////

    protected static final int span = 2;

    protected float weighting = 0.0f;

    private SharpenPanel panel;

    private final int convolveX[] = null;
    private final int convolveY[] = null;
    private float pixelWidth, pixelHeight;

    /**
     * Sets the corresponding sharpen panel.
     *
     * @param panel
     *            Corresponding panel.
     */
    void setPanel(SharpenPanel panel) {
        this.panel = panel;
        panel.setValue(weighting);
    }

    /**
     * Sets the weighting of the sharpening.
     *
     * @param newWeighting
     *            Weighting of the sharpening
     */
    void setWeighting(float newWeighting) {
        if (weighting == newWeighting) {
            return;
        }
        weighting = newWeighting;
        notifyAllListeners();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This filter is not a major filter.
     */
    @Override
    public boolean isMajorFilter() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        setWeighting(Float.parseFloat(state));
        panel.setValue(weighting);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return Float.toString(weighting);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyGL(GL2 gl) {
    }

    @Override
    public void setImageSize(int width, int height) {
        pixelWidth = 1.0f / width;
        pixelHeight = 1.0f / height;
        ShaderFactory.setFactors(weighting, pixelWidth, pixelHeight, span);
    }
}
