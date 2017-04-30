package org.helioviewer.jhv.viewmodel.view.fitsview;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelShortImageData;

class FITSImage {

    private static final float MDI_THRESHOLD = 2000f;

    final String xml;
    ImageData imageData;

    public FITSImage(String url) throws Exception {
        try (Fits f = new Fits(url)) {
            // get basic information from file
            BasicHDU<?> hdu = f.readHDU();
            if (hdu == null)
                throw new Exception("Could not read FITS: " + url);
            xml = getHeaderAsXML(hdu.getHeader());
            readHDU(hdu);
        }
    }

    private void readHDU(BasicHDU<?> hdu) throws Exception {
        int bitsPerPixel = hdu.getBitPix();
        if (bitsPerPixel == BasicHDU.BITPIX_BYTE) {
            // get image raw data
            byte[][] data2D = (byte[][]) hdu.getKernel();
            // get width and height of image
            int width = data2D[0].length;
            int height = data2D.length;
            // transform image raw data into 1D image
            byte[] data = new byte[width * height];

            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    data[width * (height - 1 - h) + w] = data2D[h][w];
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
            boolean hasNegativValue = false;
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    if (!hasNegativValue)
                        hasNegativValue = data2D[h][w] < 0;
                    highestValue = data2D[h][w] > highestValue ? data2D[h][w] : highestValue;
                    data[width * (height - 1 - h) + w] = data2D[h][w];
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
            short[] data = new short[width * height];
            // get the minimum and maximum value from current data
            int minValue = Integer.MAX_VALUE;
            int maxValue = Integer.MIN_VALUE;
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    minValue = data2D[h][w] < minValue ? data2D[h][w] : minValue;
                    maxValue = data2D[h][w] > maxValue ? data2D[h][w] : maxValue;
                }
            }
            // transform image raw data into 1D image
            float difference = maxValue - minValue;
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    data[width * (height - 1 - h) + w] = (short) (((data2D[h][w] - minValue) / difference) * 65535f);
                }
            }
            imageData = new SingleChannelShortImageData(width, height, 16, ShortBuffer.wrap(data));
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
            Header header = hdu.getHeader();
            String instrument = header.getStringValue("INSTRUME");
            String measurement = header.getStringValue("DPC_OBSR");

            if (instrument != null && measurement != null && instrument.equals("MDI") && measurement.equals("FD_Magnetogram_Sum")) {
                float doubleThreshold = MDI_THRESHOLD * 2.0f;
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        float value = data2D[h][w] + MDI_THRESHOLD;
                        value = value < 0.0f ? 0.0f : (value > doubleThreshold ? doubleThreshold : value);
                        data[width * (height - 1 - h) + w] = (short) ((value * 65535) / doubleThreshold);
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
                float difference = maxValue - minValue;
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        data[width * (height - 1 - h) + w] = (short) (((data2D[h][w] - minValue) / difference) * 65535f);
                    }
                }
            }
            imageData = new SingleChannelShortImageData(width, height, 16, ShortBuffer.wrap(data));
        }
    }

    private String getHeaderAsXML(Header header) {
        String nl = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder("<meta>" + nl + "<fits>" + nl);

        for (Cursor<String, HeaderCard> iter = header.iterator(); iter.hasNext();) {
            HeaderCard headerCard = iter.next();
            if (headerCard.getValue() != null) {
                builder.append('<').append(headerCard.getKey()).append('>').append(headerCard.getValue()).append("</").append(headerCard.getKey()).append('>').append(nl);
            }
        }
        builder.append("</fits>").append(nl).append("</meta>");
        return builder.toString().replace("&", "&amp;");
    }

}
