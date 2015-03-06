package org.helioviewer.jhv.internal_plugins.filter.sharpen;

import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.imagedata.ImageData;

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
public class SharpenFilter extends AbstractFilter {

    // /////////////////////////
    // GENERAL //
    // /////////////////////////

    protected static final int span = 2;

    protected float weighting = 0.0f;

    private SharpenPanel panel;

    private int convolveX[] = null;
    private int convolveY[] = null;

    private ImageData lastImageData;

    private boolean forceRefilter = false;

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

    // /////////////////////////
    // STANDARD //
    // /////////////////////////

    /**
     * Blurs a single channel image by applying a 3x3 Gauss lowpass filter.
     *
     * Since a convolution with a Gauss kernel is separable, this function is
     * optimized by doing so.
     *
     * <p>
     * If the image has more than one channel, this function has to be called
     * multiple times.
     *
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     * @param input
     *            Pixel data of the image, given as an integer
     * @return Blurred single channel image
     */
    private int[] blur(int width, int height, byte input[]) {
        if (convolveY == null || convolveY.length < width * height)
            convolveY = new int[width * height];

        for (int i = 0; i < width * height; i++) {
            convolveY[i] = input[i] & 0xFF;
        }
        return blur(width, height, convolveY);
    }

    /**
     * Blurs a single channel image by applying a 3x3 Gauss lowpass filter.
     *
     * Since a convolution with a Gauss kernel is separable, this function is
     * optimized by doing so.
     *
     * <p>
     * If the image has more than one channel, this function has to be called
     * multiple times.
     *
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     * @param input
     *            Pixel data of the image, given as an integer
     * @param mask
     *            to apply on the input data
     * @return Blurred single channel image
     */
    private int[] blur(int width, int height, short input[], int mask) {
        if (convolveY == null || convolveY.length < width * height)
            convolveY = new int[width * height];

        for (int i = 0; i < width * height; i++) {
            convolveY[i] = input[i] & mask;
        }
        return blur(width, height, convolveY);
    }

    /**
     * Blurs a single channel image by applying a 3x3 Gauss lowpass filter.
     *
     * Since a convolution with a Gauss kernel is separable, this function is
     * optimized by doing so.
     *
     * <p>
     * If the image has more than one channel, this function has to be called
     * multiple times.
     *
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     * @param input
     *            Pixel data of the image, given as an integer
     * @return Blurred single channel image
     */
    private int[] blur(int width, int height, int[] input) {
        if (width < 2 * span || height < 2 * span) {
            return input;
        }
        if (convolveX == null || convolveX.length < width * height)
            convolveX = new int[width * height];
        if (convolveY == null || convolveY.length < width * height)
            convolveY = new int[width * height];

        int tmpIndex;

        // convolve borders in x direction
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < span; j++) {
                tmpIndex = i * width + j;
                convolveX[tmpIndex] = ((input[tmpIndex - j] + (input[tmpIndex] << 1) + input[tmpIndex + span]) >> 2);

                tmpIndex = (i + 1) * width - 1 - j;
                convolveX[tmpIndex] = ((input[tmpIndex + j] + (input[tmpIndex] << 1) + input[tmpIndex - span]) >> 2);
            }
        }

        // convolve inner region in x direction
        for (int i = 0; i < height; i++) {
            for (int j = span; j < width - span; j++) {
                tmpIndex = i * width + j;
                convolveX[tmpIndex] = ((input[tmpIndex - span] + (input[tmpIndex] << 1) + input[tmpIndex + span]) >> 2);
            }
        }

        int spanTimesWidth = span * width;

        // convolve borders in y direction
        for (int i = 0; i < span; i++) {
            for (int j = 0; j < width; j++) {
                tmpIndex = i * width + j;
                convolveY[tmpIndex] = ((convolveX[tmpIndex - i * width] + (convolveX[tmpIndex] << 1) + convolveX[tmpIndex + spanTimesWidth]) >> 2);

                tmpIndex = (height - i) * width - 1 - j;
                convolveY[tmpIndex] = ((convolveX[tmpIndex + i * width] + (convolveX[tmpIndex] << 1) + convolveX[tmpIndex - spanTimesWidth]) >> 2);
            }
        }

        // convolve inner region in y direction
        for (int i = span; i < height - span; i++) {
            for (int j = 0; j < width; j++) {
                tmpIndex = i * width + j;
                convolveY[tmpIndex] = ((convolveX[tmpIndex - spanTimesWidth] + (convolveX[tmpIndex] << 1) + convolveX[tmpIndex + spanTimesWidth]) >> 2);
            }
        }

        return convolveY;
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

}
