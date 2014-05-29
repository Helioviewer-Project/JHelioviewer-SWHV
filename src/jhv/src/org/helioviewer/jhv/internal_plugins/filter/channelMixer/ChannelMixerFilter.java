package org.helioviewer.jhv.internal_plugins.filter.channelMixer;

import javax.media.opengl.GL;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.filter.AbstractFilter;
import org.helioviewer.viewmodel.filter.GLPostFilter;
import org.helioviewer.viewmodel.filter.StandardFilter;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.RGBInt24ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.viewmodel.imageformat.SingleChannelImageFormat;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Short16ImageTransport;

/**
 * Filter for modifying the color mask of an image.
 * 
 * <p>
 * The output of the filter always has the same image format as the input.
 * 
 * <p>
 * This filter supports software rendering as well as rendering in OpenGL.
 * 
 * <p>
 * To learn more about color masks, see
 * {@link org.helioviewer.viewmodel.imagedata.ColorMask}
 * 
 * @author Markus Langenberg
 */
public class ChannelMixerFilter extends AbstractFilter implements StandardFilter, GLPostFilter {

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
        Displayer.getSingletonInstance().display();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageData apply(ImageData data) {

        if (data == null) {
            return null;
        }

        if (data.getColorMask() == colorMask && !forceRefilter) {
            return data;
        }

        if (data instanceof SingleChannelByte8ImageData)
            return new SingleChannelByte8ImageData(data.getWidth(), data.getHeight(), ((Byte8ImageTransport) data.getImageTransport()).getByte8PixelData(), colorMask);
        else if (data instanceof ARGBInt32ImageData)
            return new ARGBInt32ImageData(false, data.getWidth(), data.getHeight(), ((Int32ImageTransport) data.getImageTransport()).getInt32PixelData(), colorMask);
        else if (data instanceof RGBInt24ImageData)
            return new RGBInt24ImageData(data.getWidth(), data.getHeight(), ((Int32ImageTransport) data.getImageTransport()).getInt32PixelData(), colorMask);
        else if (data instanceof SingleChannelShortImageData)
            return new SingleChannelShortImageData(data.getWidth(), data.getHeight(), ((SingleChannelImageFormat) data.getImageFormat()).getBitDepth(), ((Short16ImageTransport) data.getImageTransport()).getShort16PixelData(), colorMask);

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * In this case, sets the color mask by calling the corresponding
     * OpenGL-function.
     */
    @Override
    public void applyGL(GL gl) {
        gl.glColorMask(colorMask.showRed(), colorMask.showGreen(), colorMask.showBlue(), true);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * In this case, the color mask is set back to the default value.
     */
    @Override
    public void postApplyGL(GL gl) {
        gl.glColorMask(true, true, true, true);
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
