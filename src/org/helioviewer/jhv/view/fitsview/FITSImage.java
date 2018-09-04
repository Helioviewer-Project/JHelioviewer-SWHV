package org.helioviewer.jhv.view.fitsview;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageData.ImageFormat;
import org.helioviewer.jhv.log.Log;

class FITSImage {

    private static final double GAMMA = 1 / 2.2;

    private static final long BLANK = 0; // in case it doesn't exist, very unlikely value
    private static final int MARKER = -2147483648;
    private static final int MAX_LUT = 1024 * 1024;

    String xml;
    ImageData imageData;

    FITSImage(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri); Fits f = new Fits(nc.getStream())) {
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

    private static int getValue(short v, long blank) {
        return blank != BLANK && v == blank ? MARKER : v;
    }

    private static int getValue(int v, long blank) {
        return blank != BLANK && v == blank ? MARKER : v;
    }

    private static float getValue(float v, long blank) {
        return blank != BLANK && v == blank || Float.isNaN(v) ? MARKER : v;
    }

    private static float[] sampleImage(int bpp, int width, int height, Object data, long blank) throws Exception {
        int stepW = (width / 1024) * 8;
        int stepH = (height / 1024) * 8;
        float[] sampleData = new float[(width / stepW) * (height / stepH)];

        int k = 0;
        switch (bpp) {
            case BasicHDU.BITPIX_SHORT: {
                short[][] data2D = (short[][]) data;
                for (int j = 0; j < height; j += stepH) {
                    for (int i = 0; i < width; i += stepW) {
                        int v = getValue(data2D[j][i], blank);
                        if (v != MARKER)
                            sampleData[k++] = v;
                    }
                }
                break;
            }
            case BasicHDU.BITPIX_INT: {
                int[][] data2D = (int[][]) data;
                for (int j = 0; j < height; j += stepH) {
                    for (int i = 0; i < width; i += stepW) {
                        int v = getValue(data2D[j][i], blank);
                        if (v != MARKER)
                            sampleData[k++] = v;
                    }
                }
                break;
            }
            case BasicHDU.BITPIX_FLOAT: {
                float[][] data2D = (float[][]) data;
                for (int j = 0; j < height; j += stepH) {
                    for (int i = 0; i < width; i += stepW) {
                        float v = getValue(data2D[j][i], blank);
                        if (v != MARKER)
                            sampleData[k++] = v;
                    }
                }
                break;
            }
            default:
                throw new Exception("Bits per pixel not supported: " + bpp);
        }
        return Arrays.copyOf(sampleData, k);
    }

    private void readHDU(BasicHDU<?> hdu) throws Exception {
        int[] axes = hdu.getAxes();
        if (axes.length != 2)
            throw new Exception("Only 2D FITS files supported");
        int height = axes[0];
        int width = axes[1];

        Object pixelData = hdu.getKernel();
        int bpp = hdu.getBitPix();
        if (bpp == BasicHDU.BITPIX_BYTE) {
            byte[][] data2D = (byte[][]) pixelData;
            byte[] byteData = new byte[width * height];
            for (int j = 0; j < height; j++) {
                System.arraycopy(data2D[j], 0, byteData, width * (height - 1 - j), width);
            }
            imageData = new ImageData(width, height, ImageFormat.Gray8, ByteBuffer.wrap(byteData));
        } else {
            long blank = BLANK;
            try {
                blank = hdu.getBlankValue();
            } catch (Exception ignore) {
            }

            float[] sampleData = sampleImage(bpp, width, height, pixelData, blank);
            float[] zLow = {0};
            float[] zHigh = {0};
            float[] zMax = {0};
            ZScale.zscale(sampleData, sampleData.length, zLow, zHigh, zMax);

            long min = (long) zLow[0];
            long max = (long) zMax[0];
            if (min >= max) {
                Log.debug("min > max :" + min + ' ' + max);
                max = min + 1;
            }
            long lutSize = max - min;
            if (lutSize > MAX_LUT) {
                Log.debug("Pixel scaling LUT too big: " + min + ' ' + max);
                lutSize = MAX_LUT;
            }
            // System.out.println(">>> " + min + " " + max);

            switch (bpp) {
                case BasicHDU.BITPIX_SHORT: {
                    short[][] data2D = (short[][]) pixelData;

                    PixScale scale = new PowScale(lutSize, GAMMA);
                    short[] data = new short[width * height];
                    for (int j = 0; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            int v = getValue(data2D[j][i], blank);
                            data[width * (height - 1 - j) + i] = v == MARKER ? scale.get(0) : scale.get(v - min);
                        }
                    }
                    imageData = new ImageData(width, height, ImageFormat.Gray16, ShortBuffer.wrap(data));
                    imageData.setGamma(scale.getGamma());
                    break;
                }
                case BasicHDU.BITPIX_INT: {
                    int[][] data2D = (int[][]) pixelData;

                    PixScale scale = new PowScale(lutSize, GAMMA);
                    short[] data = new short[width * height];
                    for (int j = 0; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            int v = getValue(data2D[j][i], blank);
                            data[width * (height - 1 - j) + i] = v == MARKER ? scale.get(0) : scale.get(v - min);
                        }
                    }
                    imageData = new ImageData(width, height, ImageFormat.Gray16, ShortBuffer.wrap(data));
                    imageData.setGamma(scale.getGamma());
                    break;
                }
                case BasicHDU.BITPIX_FLOAT: {
                    float[][] data2D = (float[][]) pixelData;

                    double scale = 65535. / lutSize;
                    short[] data = new short[width * height];
                    for (int j = 0; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            float v = getValue(data2D[j][i], blank);
                            data[width * (height - 1 - j) + i] = v == MARKER ? 0 : (short) (scale * Math.pow(v - min, GAMMA));
                        }
                    }
                    imageData = new ImageData(width, height, ImageFormat.Gray16, ShortBuffer.wrap(data));
                    break;
                }
                default:
                    throw new Exception("Bits per pixel not supported: " + bpp);
            }
        }
    }

    private abstract static class PixScale {

        protected short[] lut;

        short get(long v) {
            if (v < 0)
                return lut[0];
            else if (v < lut.length)
                return lut[(int) v];
            else
                return lut[lut.length - 1];
        }

        abstract double getGamma();

    }

    private static class LinScale extends PixScale {

        LinScale(long size) {
            double scale = 65535. / size;

            lut = new short[(int) (size + 1)];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * i + .5);
        }

        @Override
        double getGamma() {
            return GAMMA;
        }

    }

    private static class LogScale extends PixScale {

        LogScale(long size) {
            double scale = 65535. / Math.log1p(size);

            lut = new short[(int) (size + 1)];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * Math.log1p(i) + .5);
        }

        @Override
        double getGamma() {
            return 1;
        }

    }

    private static class PowScale extends PixScale {

        PowScale(long size, double p) {
            double scale = 65535. / Math.pow(size, p);

            lut = new short[(int) (size + 1)];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * Math.pow(i, p) + .5);
        }

        @Override
        double getGamma() {
            return 1;
        }

    }

    private static String getHeaderAsXML(Header header) {
        String nl = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder("<meta>").append(nl).append("<fits>").append(nl);

        for (Cursor<String, HeaderCard> iter = header.iterator(); iter.hasNext(); ) {
            HeaderCard headerCard = iter.next();
            if (headerCard.getValue() != null) {
                builder.append('<').append(headerCard.getKey()).append('>').append(headerCard.getValue()).append("</").append(headerCard.getKey()).append('>').append(nl);
            }
        }
        builder.append("</fits>").append(nl).append("</meta>");
        return builder.toString().replace("&", "&amp;");
    }

}
