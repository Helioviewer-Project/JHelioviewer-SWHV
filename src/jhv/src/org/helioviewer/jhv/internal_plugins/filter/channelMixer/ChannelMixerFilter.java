package org.helioviewer.jhv.internal_plugins.filter.channelMixer;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.filter.GLFilter;
import org.helioviewer.viewmodel.imagedata.ColorMask;

/**
 * Filter for modifying the color mask of an image.
 *
 * <p>
 * The output of the filter always has the same image format as the input.
 *
 * <p>
 * This filter supports software rendering as well as rendering in OpenGL2.
 *
 * <p>
 * To learn more about color masks, see
 * {@link org.helioviewer.viewmodel.imagedata.ColorMask}
 *
 * @author Markus Langenberg
 */
public class ChannelMixerFilter extends AbstractFilter implements GLFilter {

    private ColorMask colorMask = new ColorMask();
    private ChannelMixerPanel panel;
    private boolean forceRefilter = false;

    /**
     * Sets the corresponding channel mixer panel.
     *
     * @param panel
     *            Corresponding panel.
     */
    void setPanel(ChannelMixerPanel panel) {
        this.panel = panel;
        panel.setValue(colorMask);
    }

    /**
     * Sets the color mask.
     *
     * @param showRed
     *            if true, the red channel will be shown
     * @param showGreen
     *            if true, the green channel will be shown
     * @param showBlue
     *            if true, the blue channel will be shown
     */
    void setColorMask(boolean showRed, boolean showGreen, boolean showBlue) {
        ColorMask newColorMask = new ColorMask(showRed, showGreen, showBlue);
        if (colorMask == newColorMask) {
            return;
        }
        colorMask = newColorMask;
        notifyAllListeners();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * In this case, sets the color mask by calling the corresponding
     * OpenGL-function.
     */
    @Override
    public void applyGL(GL2 gl) {
        gl.glColorMask(colorMask.showRed(), colorMask.showGreen(), colorMask.showBlue(), true);
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
        String[] values = state.trim().split(" ");
        if (values.length != 3) {
            return;
        }

        setColorMask(Boolean.parseBoolean(values[0]), Boolean.parseBoolean(values[1]), Boolean.parseBoolean(values[2]));
        panel.setValue(colorMask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return colorMask.showRed() + " " + colorMask.showGreen() + " " + colorMask.showBlue();
    }

}
