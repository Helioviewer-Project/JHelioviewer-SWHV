package org.helioviewer.jhv.view.fitsview;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.Single8ImageData;
import org.helioviewer.jhv.imagedata.Single16ImageData;
import org.helioviewer.jhv.log.Log;

class FITSImage {

    private static final double GAMMA = 1 / 2.2;

    String xml;
    ImageData imageData;

    FITSImage(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri.toURL());
             Fits f = new Fits(nc.getStream())) {
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
        if (hdu.getHeader().getIntValue("NAXIS", 0) != 2)
            throw new Exception("Only 2D FITS files supported");
        int bitsPerPixel = hdu.getBitPix();
        switch (bitsPerPixel) {
            case BasicHDU.BITPIX_BYTE: {
                byte[][] data2D = (byte[][]) hdu.getKernel();
                int width = data2D[0].length;
                int height = data2D.length;

                byte[] data = new byte[width * height];
                for (int j = 0; j < height; j++) {
                    System.arraycopy(data2D[j], 0, data, width * (height - 1 - j), width);
                }
                imageData = new Single8ImageData(width, height, ByteBuffer.wrap(data));
                break;
            }
            case BasicHDU.BITPIX_SHORT: {
                short[][] data2D = (short[][]) hdu.getKernel();
                int width = data2D[0].length;
                int height = data2D.length;

                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        int d = data2D[j][i];
                        min = d < min ? d : min;
                        max = d > max ? d : max;
                    }
                }

                PixScale scale = new PowScale(min, max, GAMMA);
                short[] data = new short[width * height];
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        data[width * (height - 1 - j) + i] = scale.get(data2D[j][i] - min);
                    }
                }
                imageData = new Single16ImageData(width, height, scale.getGamma(), ShortBuffer.wrap(data));
                break;
            }
            case BasicHDU.BITPIX_INT: {
                int[][] data2D = (int[][]) hdu.getKernel();
                int width = data2D[0].length;
                int height = data2D.length;

                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        int d = data2D[j][i];
                        min = d < min ? d : min;
                        max = d > max ? d : max;
                    }
                }

                PixScale scale = new PowScale(min, max, GAMMA);
                short[] data = new short[width * height];
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        data[width * (height - 1 - j) + i] = scale.get(data2D[j][i] - min);
                    }
                }
                imageData = new Single16ImageData(width, height, scale.getGamma(), ShortBuffer.wrap(data));
                break;
            }
            case BasicHDU.BITPIX_FLOAT: {
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
                        data[width * (height - 1 - j) + i] = (short) (scale * Math.pow(data2D[j][i] - min, GAMMA));
                    }
                }
                imageData = new Single16ImageData(width, height, 1, ShortBuffer.wrap(data));
                break;
            }
        }
    }

    private abstract static class PixScale {

        protected static final int MAX_LUT = 1024 * 1024;
        protected short[] lut;

        short get(int v) {
            if (v < 0)
                return lut[0];
            else if (v < lut.length)
                return lut[v];
            else
                return lut[lut.length - 1];
        }

        abstract double getGamma();

    }

    private static class LinScale extends PixScale {

        LinScale(int min, int max) {
            int diff = max > min ? max - min : 1;
            if (diff > MAX_LUT) {
                Log.debug("Pixel scaling LUT too big: " + diff);
                diff = MAX_LUT;
            }
            double scale = 65535. / diff;

            lut = new short[diff + 1];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * i + .5);
        }

        double getGamma() {
            return GAMMA;
        }

    }


    private static class LogScale extends PixScale {

        LogScale(int min, int max) {
            int diff = max > min ? max - min : 1;
            if (diff > MAX_LUT) {
                Log.debug("Pixel scaling LUT too big: " + diff);
                diff = MAX_LUT;
            }
            double scale = 65535. / Math.log1p(diff);

            lut = new short[diff + 1];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * Math.log1p(i) + .5);
        }

        double getGamma() {
            return 1;
        }

    }

    private static class PowScale extends PixScale {

        PowScale(int min, int max, double p) {
            int diff = max > min ? max - min : 1;
            if (diff > MAX_LUT) {
                Log.debug("Pixel scaling LUT too big: " + diff);
                diff = MAX_LUT;
            }
            double scale = 65535. / Math.pow(diff, p);

            lut = new short[diff + 1];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * Math.pow(i, p) + .5);
        }

        double getGamma() {
            return 1;
        }

    }

    private static String getHeaderAsXML(Header header) {
        String nl = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder("<meta>").append(nl).append("<fits>").append(nl);

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
