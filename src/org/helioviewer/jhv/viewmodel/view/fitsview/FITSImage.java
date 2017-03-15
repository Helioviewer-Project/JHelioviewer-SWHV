package org.helioviewer.jhv.viewmodel.view.fitsview;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataContainer;

/**
 * This class provides access to any FITS file and makes the image data
 * available.
 *
 * @author Andreas Hoelzl
 * @author Stephan Pagel
 * */
public class FITSImage implements MetaDataContainer {

    private static final float MDI_THRESHOLD = 2000f;

    private Header header;
    private BufferedImage image = null;

    public FITSImage(String url) throws Exception {
        try (Fits f = new Fits(url)) {
            // get basic information from file
            BasicHDU<?> hdu = f.readHDU();
            if (hdu == null)
                throw new Exception("Could not read FITS: " + url);
            readHDU(hdu);
        }
    }

    private void readHDU(BasicHDU<?> hdu) throws Exception {
        header = hdu.getHeader();
        ImageData imageData = null;

        int bitsPerPixel = hdu.getBitPix();
        if (bitsPerPixel == BasicHDU.BITPIX_BYTE) {
            // get image raw data
            byte[][] data2D = (byte[][]) hdu.getKernel();
            // get width and height of image
            int width = data2D[0].length;
            int height = data2D.length;
            // transform image raw data into 1D image
            byte[] data = new byte[width * height];

            int counter = 0;
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    data[counter] = data2D[h][w];
                    counter++;
                }
            }
            imageData = new SingleChannelByte8ImageData(width, height, ByteBuffer.wrap(data));
        } else if (bitsPerPixel == BasicHDU.BITPIX_SHORT) {
            // get image raw data
            short[][] data2D = (short[][]) hdu.getKernel();
            // get width and height of image
            int width = data2D[0].length;
            int height = data2D.length;
            // transform image raw data into 1D image and
            // analyze the highest value when transfering the data
            short[] data = new short[width * height];
            int highestValue = Integer.MIN_VALUE;
            int counter = 0;
            boolean hasNegativValue = false;
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    if (!hasNegativValue)
                        hasNegativValue = data2D[h][w] < 0;
                    highestValue = data2D[h][w] > highestValue ? data2D[h][w] : highestValue;
                    data[counter] = data2D[h][w];
                    counter++;
                }
            }

            // if first bit is not set, shift bits
            if (!hasNegativValue) {
                // compute number of bits to shift
                int shiftBits = BasicHDU.BITPIX_SHORT - ((int) Math.ceil(Math.log(highestValue) / Math.log(2)));
                // shift bits of all values
                for (int i = 0; i < data.length; i++) {
                    data[i] <<= shiftBits;
                }
            }
            imageData = new SingleChannelShortImageData(width, height, bitsPerPixel, ShortBuffer.wrap(data));
        } else if (bitsPerPixel == BasicHDU.BITPIX_INT) {
            // get image raw data
            int[][] data2D = (int[][]) hdu.getKernel();
            // get width and height of image
            int width = data2D[0].length;
            int height = data2D.length;
            // transform image raw data into 1D image
            int[] data = new int[width * height];
            int counter = 0;
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    data[counter] = data2D[h][w];
                    counter++;
                }
            }
            imageData = new ARGBInt32ImageData(false, width, height, IntBuffer.wrap(data));
        } else if (bitsPerPixel == BasicHDU.BITPIX_FLOAT) {
            // get image raw data
            float[][] data2D = (float[][]) hdu.getKernel();
            // get width and height of image
            int width = data2D[0].length;
            int height = data2D.length;
            // transform image raw data into 1D image
            short[] data = new short[width * height];
            // if it is an MDI magnetogram image use threshold when converting the data
            // otherwise set minimum value to zero and maximum value to 2^16 and scale values between
            String instrument = header.getStringValue("INSTRUME");
            String measurement = header.getStringValue("DPC_OBSR");

            if (instrument != null && measurement != null && instrument.equals("MDI") && measurement.equals("FD_Magnetogram_Sum")) {
                int counter = 0;
                float doubleThreshold = MDI_THRESHOLD * 2.0f;
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        float value = data2D[h][w] + MDI_THRESHOLD;
                        value = value < 0.0f ? 0.0f : (value > doubleThreshold ? doubleThreshold : value);
                        data[counter] = (short) ((value * 65535) / doubleThreshold);
                        counter++;
                    }
                }
            } else {
                // get the minimum and maximum value from current data
                float minValue = Float.MAX_VALUE;
                float maxValue = Float.MIN_VALUE;
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        minValue = data2D[h][w] < minValue ? data2D[h][w] : minValue;
                        maxValue = data2D[h][w] > maxValue ? data2D[h][w] : maxValue;
                    }
                }
                // transform image raw data into 1D image
                int counter = 0;
                float difference = maxValue - minValue;
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        data[counter] = (short) (((data2D[h][w] - minValue) / difference) * 65536.0f);
                        counter++;
                    }
                }
            }
            imageData = new SingleChannelShortImageData(width, height, 16, ShortBuffer.wrap(data));
        }
        if (imageData != null)
            image = imageData.getBufferedImage();
    }

    /**
     * Returns the image data of the specified area as an image object.
     *
     * @param x
     *            X pixel coordinate of the top left point of the region.
     * @param y
     *            Y pixel coordinate of the top left point of the region.
     * @param height
     *            Height in pixel of the region.
     * @param width
     *            Width in pixel of the region.
     * @return an BufferedImage which represents the image data of the specified
     *         area or null of no image data is available or the width or height
     *         parameter is less than 1 pixel.
     * */
    public BufferedImage getImage(int x, int y, int height, int width) {
        // check parameters and image source
        if (image == null || width <= 0 || height <= 0) {
            return null;
        }
        // create new buffered image with requested region
        BufferedImage subImage = new BufferedImage(width, height, image.getType());
        subImage.getGraphics().drawImage(image.getSubimage(x, y, width, height), 0, 0, null);
        // return new buffered image
        return subImage;
    }

    /**
     * Returns the FITS header information as XML string.
     *
     * @return XML string including all FITS header information.
     * */
    public String getHeaderAsXML() {
        String sep = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder("<meta>" + sep + "<fits>" + sep);

        for (Cursor<String, HeaderCard> iter = header.iterator(); iter.hasNext();) {
            HeaderCard headerCard = iter.next();
            if (headerCard.getValue() != null) {
                builder.append('<').append(headerCard.getKey()).append('>').append(headerCard.getValue()).append("</").append(headerCard.getKey()).append('>').append(sep);
            }
        }
        builder.append("</fits>").append(sep).append("</meta>");

        return builder.toString().replace("&", "&amp;");
    }

    @Override
    public String get(String key) {
        return header.getStringValue(key);
    }

    @Override
    public double tryGetDouble(String key) {
        return header.getDoubleValue(key);
    }

    @Override
    public int tryGetInt(String key) {
        return header.getIntValue(key);
    }

}
