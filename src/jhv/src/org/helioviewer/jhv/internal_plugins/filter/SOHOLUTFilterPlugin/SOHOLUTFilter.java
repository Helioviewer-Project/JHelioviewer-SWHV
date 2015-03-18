package org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin;

import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imageformat.SingleChannelImageFormat;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Short16ImageTransport;

/**
 * Filter for applying a color table to a single channel image.
 *
 * <p>
 * If the input image is not a single channel image, the filter does nothing and
 * returns the input data.
 *
 * <p>
 * This filter supports software rendering as well as rendering in OpenGL2.
 *
 * mostly rewritten
 *
 * @author Helge Dietert
 */
public class SOHOLUTFilter {
    /**
     * Used lut
     */
    private LUT lut;
    private final LUT gray = LUT.getStandardList().get("Gray");
    private boolean invertLUT = false;

    /**
     * LUT is set to Gray as default table.
     */
    public SOHOLUTFilter() {
        lut = gray;
    }

    /**
     * Constructor setting the color table.
     *
     * @param startWithLut
     *            Color table to apply to the image
     */
    public SOHOLUTFilter(LUT startWithLut) {
        lut = startWithLut;
    }

    /**
     * Sets a new color table to use from now on.
     *
     * @param newLUT
     *            New color table
     */
    void setLUT(LUT newLUT, boolean invert) {
        if (newLUT == null || (lut == newLUT && invertLUT == invert)) {
            return;
        }
        lut = newLUT;
        invertLUT = invert;
    }

    /**
     * {@inheritDoc}
     */
    public ImageData apply(ImageData data) {
        // Skip over gray for performance as before
        if (data == null || !(data.getImageFormat() instanceof SingleChannelImageFormat) || (lut.getName() == "Gray" && !invertLUT)) {
            return data;
        }

        if (data.getImageTransport() instanceof Byte8ImageTransport) {
            byte[] pixelData = ((Byte8ImageTransport) data.getImageTransport()).getByte8PixelData();
            int[] resultPixelData = new int[pixelData.length];
            lut.lookup8(pixelData, resultPixelData, invertLUT);
            return new ARGBInt32ImageData(data, resultPixelData);
        } else if (data.getImageTransport() instanceof Short16ImageTransport) {
            short[] pixelData = ((Short16ImageTransport) data.getImageTransport()).getShort16PixelData();
            int[] resultPixelData = new int[pixelData.length];
            lut.lookup16(pixelData, resultPixelData, invertLUT);
            return new ARGBInt32ImageData(data, resultPixelData);
        }
        return null;
    }

}
