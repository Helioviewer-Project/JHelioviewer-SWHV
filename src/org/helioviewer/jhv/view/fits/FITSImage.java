package org.helioviewer.jhv.view.fits;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import nom.tam.fits.BasicHDU;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.log.Log;

class FITSImage {

    private static final double GAMMA = 1 / 2.2;
    private static final long BLANK = 0; // in case it doesn't exist, very unlikely value

    private static float getValue(int bpp, Object lineData, int i, long blank, double bzero, double bscale) {
        double v = ImageData.BAD_PIXEL;
        switch (bpp) {
            case BasicHDU.BITPIX_SHORT:
                v = ((short[]) lineData)[i];
                break;
            case BasicHDU.BITPIX_INT:
                v = ((int[]) lineData)[i];
                break;
            case BasicHDU.BITPIX_LONG:
                v = ((long[]) lineData)[i];
                break;
            case BasicHDU.BITPIX_FLOAT:
                v = ((float[]) lineData)[i];
                break;
            case BasicHDU.BITPIX_DOUBLE:
                v = ((double[]) lineData)[i];
                break;
        }
        return (blank != BLANK && v == blank) || !Double.isFinite(v) ? ImageData.BAD_PIXEL : (float) (bzero + v * bscale);
    }

    private static float[] sampleImage(int bpp, int width, int height, Object[] pixelData, long blank, double bzero, double bscale, int[] npix) {
        int stepW = Math.max(4 * width / 1024, 1);
        int stepH = Math.max(4 * height / 1024, 1);
        float[] sampleData = new float[(width / stepW) * (height / stepH)];

        int k = 0;
        for (int j = 0; j < height; j += stepH) {
            Object lineData = pixelData[j];
            for (int i = 0; i < width; i += stepW) {
                float v = getValue(bpp, lineData, i, blank, bzero, bscale);
                if (v != ImageData.BAD_PIXEL)
                    sampleData[k++] = v;
            }
        }
        npix[0] = k;
        return sampleData;
    }

    /*
        private static float[] getMinMax(int bpp, int width, int height, Object[] pixelData, long blank, double bzero, double bscale) {
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;

            for (int j = 0; j < height; j++) {
                Object lineData = pixelData[j];
                for (int i = 0; i < width; i++) {
                    float v = getValue(bpp, lineData, i, blank, bzero, bscale);
                    if (v != ImageData.BAD_PIXEL) {
                        if (v > max)
                            max = v;
                        if (v < min)
                            min = v;
                    }
                }
            }
            return new float[]{min, max};
        }
    */

    static ImageData readHDU(BasicHDU<?> hdu) throws Exception {
        int[] axes = hdu.getAxes();
        if (axes == null || axes.length != 2)
            throw new Exception("Only 2D FITS files supported");
        int height = axes[0];
        int width = axes[1];

        int bpp = hdu.getBitPix();
        Object kernel = hdu.getKernel();
        if (!(kernel instanceof Object[]))
            throw new Exception("Cannot retrieve pixel data");
        Object[] pixelData = (Object[]) kernel;

        long blank = BLANK;
        try {
            blank = hdu.getBlankValue();
        } catch (Exception ignore) {
        }

        if (bpp == BasicHDU.BITPIX_BYTE) {
            byte[][] inData = (byte[][]) pixelData;
            byte[] outData = new byte[width * height];
            for (int j = 0; j < height; j++) {
                System.arraycopy(inData[j], 0, outData, width * (height - 1 - j), width);
            }
            return new ImageData(new ImageBuffer(width, height, ImageBuffer.Format.Gray8, ByteBuffer.wrap(outData)));
        }

        double bzero = hdu.getBZero();
        double bscale = hdu.getBScale();

        int[] npix = {0};
        float[] sampleData = sampleImage(bpp, width, height, pixelData, blank, bzero, bscale, npix);

        float[] zLow = {0};
        float[] zHigh = {0};
        float[] zMax = {0};
        ZScale.zscale(sampleData, npix[0], zLow, zHigh, zMax);
        // System.out.println(">>> " + npix[0] + " " + zLow[0] + " " + zMax[0]);

        float[] minmax = {zLow[0], zMax[0]};
        // float[] minmax = getMinMax(bpp, width, height, pixelData, blank, bzero, bscale);
        if (minmax[0] >= minmax[1]) {
            Log.debug("min >= max :" + minmax[0] + ' ' + minmax[1]);
            minmax[1] = minmax[0] + 1;
        }
        double range = minmax[1] - minmax[0];
        // System.out.println(">>> " + minmax[0] + ' ' + minmax[1]);

        short[] outData = new short[width * height];
        float[] lut = new float[65536];
        switch (bpp) {
            case BasicHDU.BITPIX_SHORT:
            case BasicHDU.BITPIX_INT:
            case BasicHDU.BITPIX_LONG:
            case BasicHDU.BITPIX_FLOAT: {
                double scale = 65535. / Math.pow(range, GAMMA);
                for (int j = 0; j < height; j++) {
                    Object lineData = pixelData[j];
                    for (int i = 0; i < width; i++) {
                        float v = getValue(bpp, lineData, i, blank, bzero, bscale);
                        int p = (int) MathUtils.clip(scale * Math.pow(v - minmax[0], GAMMA) + .5, 0, 65535);
                        lut[p] = v;
                        outData[width * (height - 1 - j) + i] = v == ImageData.BAD_PIXEL ? 0 : (short) p;
                    }
                }
                break;
            }
            case BasicHDU.BITPIX_DOUBLE: {
                double scale = 65535. / Math.log1p(range);
                for (int j = 0; j < height; j++) {
                    Object lineData = pixelData[j];
                    for (int i = 0; i < width; i++) {
                        float v = getValue(bpp, lineData, i, blank, bzero, bscale);
                        int p = (int) MathUtils.clip(scale * Math.log1p(v - minmax[0]) + .5, 0, 65535);
                        lut[p] = v;
                        outData[width * (height - 1 - j) + i] = v == ImageData.BAD_PIXEL ? 0 : (short) p;
                    }
                }
                break;
            }
        }
        ImageData imageData = new ImageData(new ImageBuffer(width, height, ImageBuffer.Format.Gray16, ShortBuffer.wrap(outData)));
        imageData.setPhysicalLUT(lut);
        return imageData;
    }

}
