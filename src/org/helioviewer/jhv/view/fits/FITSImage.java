package org.helioviewer.jhv.view.fits;

import java.net.URI;
import java.nio.ShortBuffer;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.log.Log;

class FITSImage {

    private static final double GAMMA = 1 / 2.2;
    private static final long BLANK = 0; // in case it doesn't exist, very unlikely value
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

    private static float getValue(int bpp, int i, int j, Object pixelData, long blank, double bzero, double bscale) {
        double v = 0;
        switch (bpp) {
            case BasicHDU.BITPIX_BYTE:
                v = ((byte[][]) pixelData)[j][i];
                break;
            case BasicHDU.BITPIX_SHORT:
                v = ((short[][]) pixelData)[j][i];
                break;
            case BasicHDU.BITPIX_INT:
                v = ((int[][]) pixelData)[j][i];
                break;
            case BasicHDU.BITPIX_LONG:
                v = ((long[][]) pixelData)[j][i];
                break;
            case BasicHDU.BITPIX_FLOAT:
                v = ((float[][]) pixelData)[j][i];
                break;
            case BasicHDU.BITPIX_DOUBLE:
                v = ((double[][]) pixelData)[j][i];
                break;
        }
        return (blank != BLANK && v == blank) || !Double.isFinite(v) ? ImageData.BAD_PIXEL : (float) (bzero + v * bscale);
    }

    private static float[] sampleImage(int bpp, int width, int height, Object pixelData, long blank, double bzero, double bscale, int[] npix) {
        int stepW = 4 * width / 1024;
        int stepH = 4 * height / 1024;
        float[] sampleData = new float[(width / stepW) * (height / stepH)];

        int k = 0;
        for (int j = 0; j < height; j += stepH) {
            for (int i = 0; i < width; i += stepW) {
                float v = getValue(bpp, i, j, pixelData, blank, bzero, bscale);
                if (v != ImageData.BAD_PIXEL)
                    sampleData[k++] = v;
            }
        }
        npix[0] = k;
        return sampleData;
    }

    private static float[] getMinMax(int bpp, int width, int height, Object pixelData, long blank, double bzero, double bscale) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float v = getValue(bpp, i, j, pixelData, blank, bzero, bscale);
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

    private String parseUnit(String unit) {
        return unit == null ? "" : unit.replace(" m-2", "/m\u00B2").replace(" sr-1", "/sr");
    }

    private void readHDU(BasicHDU<?> hdu) throws Exception {
        int[] axes = hdu.getAxes();
        if (axes == null || axes.length != 2)
            throw new Exception("Only 2D FITS files supported");
        int height = axes[0];
        int width = axes[1];

        int bpp = hdu.getBitPix();
        Object pixelData = hdu.getKernel();
        if (pixelData == null)
            throw new Exception("Cannot retrieve pixel data");

        long blank = BLANK;
        try {
            blank = hdu.getBlankValue();
        } catch (Exception ignore) {
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
        //float[] minmax = getMinMax(bpp, width, height, pixelData, blank, bzero, bscale);
        if (minmax[0] >= minmax[1]) {
            Log.debug("min >= max :" + minmax[0] + ' ' + minmax[1]);
            minmax[1] = minmax[0] + 1;
        }
        long lutSize = (long) (minmax[1] - minmax[0]);
        if (lutSize > MAX_LUT) {
            Log.debug("Pixel scaling LUT too big: " + minmax[0] + ' ' + minmax[1]);
            lutSize = MAX_LUT;
        }
        // System.out.println(">>> " + minmax[0] + ' ' + minmax[1]);

        short[] outData = new short[width * height];
        float[] lut = new float[65536];
        switch (bpp) {
            case BasicHDU.BITPIX_BYTE: {
                double scale = 65535. / lutSize;
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        float v = getValue(bpp, i, j, pixelData, blank, bzero, bscale);
                        int p = (int) MathUtils.clip(scale * (v - minmax[0]) + .5, 0, 65535);
                        lut[p] = v;
                        outData[width * (height - 1 - j) + i] = v == ImageData.BAD_PIXEL ? 0 : (short) p;
                    }
                }
                break;
            }
            case BasicHDU.BITPIX_SHORT:
            case BasicHDU.BITPIX_INT:
            case BasicHDU.BITPIX_LONG:
            case BasicHDU.BITPIX_FLOAT: {
                double scale = 65535. / Math.pow(lutSize, GAMMA);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        float v = getValue(bpp, i, j, pixelData, blank, bzero, bscale);
                        int p = (int) MathUtils.clip(scale * Math.pow(v - minmax[0], GAMMA) + .5, 0, 65535);
                        lut[p] = v;
                        outData[width * (height - 1 - j) + i] = v == ImageData.BAD_PIXEL ? 0 : (short) p;
                    }
                }
                break;
            }
            case BasicHDU.BITPIX_DOUBLE: {
                double scale = 65535. / Math.log1p(lutSize);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        float v = getValue(bpp, i, j, pixelData, blank, bzero, bscale);
                        int p = (int) MathUtils.clip(scale * Math.log1p(v - minmax[0]) + .5, 0, 65535);
                        lut[p] = v;
                        outData[width * (height - 1 - j) + i] = v == ImageData.BAD_PIXEL ? 0 : (short) p;
                    }
                }
                break;
            }
        }
        imageData = new ImageData(new ImageBuffer(width, height, ImageBuffer.Format.Gray16, ShortBuffer.wrap(outData)));
        imageData.setPhysical(lut, parseUnit(hdu.getBUnit()));
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
