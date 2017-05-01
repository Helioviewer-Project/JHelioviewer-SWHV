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
    private static final double GAMMA = 1 / 2.2;

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
            // get the minimum and maximum value from current data
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    minValue = data2D[h][w] < minValue ? data2D[h][w] : minValue;
                    maxValue = data2D[h][w] > maxValue ? data2D[h][w] : maxValue;
                }
            }

            double scale = 65535. / (maxValue == minValue ? 1 : maxValue - minValue);
            short[] data = new short[width * height];
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    data[width * (height - 1 - h) + w] = (short) (scale * (data2D[h][w] - minValue));
                }
            }
            imageData = new SingleChannelShortImageData(width, height, 16, GAMMA, ShortBuffer.wrap(data));
        } else if (bitsPerPixel == BasicHDU.BITPIX_INT) {
            // get image raw data
            int[][] data2D = (int[][]) hdu.getKernel();
            // get width and height of image
            int width = data2D[0].length;
            int height = data2D.length;
            // get the minimum and maximum value from current data
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    minValue = data2D[h][w] < minValue ? data2D[h][w] : minValue;
                    maxValue = data2D[h][w] > maxValue ? data2D[h][w] : maxValue;
                }
            }

            double scale = 65535. / (maxValue == minValue ? 1 : maxValue - minValue);
            short[] data = new short[width * height];
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    data[width * (height - 1 - h) + w] = (short) (scale * (data2D[h][w] - minValue));
                }
            }
            imageData = new SingleChannelShortImageData(width, height, 16, GAMMA, ShortBuffer.wrap(data));
        } else if (bitsPerPixel == BasicHDU.BITPIX_FLOAT) {
            // get image raw data
            float[][] data2D = (float[][]) hdu.getKernel();
            // get width and height of image
            int width = data2D[0].length;
            int height = data2D.length;
            // if it is an MDI magnetogram image use threshold when converting the data
            // otherwise set minimum value to zero and maximum value to 2^16 and scale values between
            Header header = hdu.getHeader();
            String instrument = header.getStringValue("INSTRUME");
            String measurement = header.getStringValue("DPC_OBSR");

            short[] data = new short[width * height];
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
                double minValue = Double.MAX_VALUE;
                double maxValue = Double.MIN_VALUE;
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        minValue = data2D[h][w] < minValue ? data2D[h][w] : minValue;
                        maxValue = data2D[h][w] > maxValue ? data2D[h][w] : maxValue;
                    }
                }

                double scale = 65535. / (maxValue == minValue ? 1 : maxValue - minValue);
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        data[width * (height - 1 - h) + w] = (short) (scale * (data2D[h][w] - minValue));
                    }
                }
            }
            imageData = new SingleChannelShortImageData(width, height, 16, GAMMA, ShortBuffer.wrap(data));
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
