package org.helioviewer.jhv.viewmodel.view.fitsview;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelShortImageData;

class FITSImage {

    private static final double GAMMA = 1 / 2.2;

    String xml;
    ImageData imageData;

    public FITSImage(String url) throws Exception {
        try (Fits f = new Fits(url)) {
            BasicHDU<?>[] hdus = f.read();
            // this is cumbersome
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof CompressedImageHDU) {
                    xml = getHeaderAsXML(hdu.getHeader());
                    readHDU(((CompressedImageHDU) hdu).asImageHDU());
                    return;
                }
            }
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof ImageHDU) {
                    xml = getHeaderAsXML(hdu.getHeader());
                    readHDU(hdu);
                    return;
                }
            }
        }
    }

    private void readHDU(BasicHDU<?> hdu) throws Exception {
        int bitsPerPixel = hdu.getBitPix();
        if (bitsPerPixel == BasicHDU.BITPIX_BYTE) {
            byte[][] data2D = (byte[][]) hdu.getKernel();
            int width = data2D[0].length;
            int height = data2D.length;

            byte[] data = new byte[width * height];
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    data[width * (height - 1 - j) + i] = data2D[j][i];
                }
            }
            imageData = new SingleChannelByte8ImageData(width, height, ByteBuffer.wrap(data));
        } else if (bitsPerPixel == BasicHDU.BITPIX_SHORT) {
            short[][] data2D = (short[][]) hdu.getKernel();
            int width = data2D[0].length;
            int height = data2D.length;

            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    double d = data2D[j][i];
                    min = d < min ? d : min;
                    max = d > max ? d : max;
                }
            }

            double scale = 65535. / (max == min ? 1 : max - min);
            short[] data = new short[width * height];
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    data[width * (height - 1 - j) + i] = (short) (scale * (data2D[j][i] - min));
                }
            }
            imageData = new SingleChannelShortImageData(width, height, 16, GAMMA, ShortBuffer.wrap(data));
        } else if (bitsPerPixel == BasicHDU.BITPIX_INT) {
            int[][] data2D = (int[][]) hdu.getKernel();
            int width = data2D[0].length;
            int height = data2D.length;

            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    double d = data2D[j][i];
                    min = d < min ? d : min;
                    max = d > max ? d : max;
                }
            }

            double scale = 65535. / (max == min ? 1 : max - min);
            short[] data = new short[width * height];
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    data[width * (height - 1 - j) + i] = (short) (scale * (data2D[j][i] - min));
                }
            }
            imageData = new SingleChannelShortImageData(width, height, 16, GAMMA, ShortBuffer.wrap(data));
        } else if (bitsPerPixel == BasicHDU.BITPIX_FLOAT) {
            float[][] data2D = (float[][]) hdu.getKernel();
            int width = data2D[0].length;
            int height = data2D.length;

            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    double d = data2D[j][i];
                    min = d < min ? d : min;
                    max = d > max ? d : max;
                }
            }

            double scale = 65535. / (max == min ? 1 : max - min);
            short[] data = new short[width * height];
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    data[width * (height - 1 - j) + i] = (short) (scale * (data2D[j][i] - min));
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
