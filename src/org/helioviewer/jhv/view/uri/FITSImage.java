package org.helioviewer.jhv.view.uri;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.math.MathUtils;

import com.google.common.primitives.Floats;

// essentially static; local or network cache
class FITSImage implements URIImageReader {

    @Nullable
    @Override
    public String readXML(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri);
             InputStream is = FileUtils.decompressStream(nc.getStream());
             Fits f = new Fits(is)) {
            BasicHDU<?>[] hdus = f.read();
            // this is cumbersome
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof CompressedImageHDU) {
                    return readHeader(((CompressedImageHDU) hdu).asImageHDU());
                }
            }
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof ImageHDU) {
                    return readHeader(hdu);
                }
            }
        }
        throw new Exception("No image found");
    }

    @Nullable
    @Override
    public ImageBuffer readImageBuffer(URI uri, float[] minMax) throws Exception {
        try (NetClient nc = NetClient.of(uri);
             InputStream is = FileUtils.decompressStream(nc.getStream());
             Fits f = new Fits(is)) {
            BasicHDU<?>[] hdus = f.read();
            // this is cumbersome
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof CompressedImageHDU) {
                    return readHDU(((CompressedImageHDU) hdu).asImageHDU(), minMax);
                }
            }
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof ImageHDU) {
                    return readHDU(hdu, minMax);
                }
            }
        }
        throw new Exception("No image found");
    }

    private static String readHeader(BasicHDU<?> hdu) {
        return getHeaderAsXML(hdu.getHeader());
    }

    private static final double GAMMA = 1 / 2.2;
    private static final long BLANK = 0; // in case it doesn't exist, very unlikely value

    private static float getValue(int bpp, Object lineData, int i, long blank, double bzero, double bscale) {
        double v = ImageBuffer.BAD_PIXEL;
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
        return (blank != BLANK && v == blank) || !Double.isFinite(v) ? ImageBuffer.BAD_PIXEL : (float) (bzero + v * bscale);
    }

    // private static final int SAMPLE = 8;
    private static final int SAMPLE = 4;

    private static float[] sampleImage(int bpp, int width, int height, Object[] pixelData, long blank, double bzero, double bscale) {
        int stepW = Math.max(SAMPLE * width / 1024, 1);
        int stepH = Math.max(SAMPLE * height / 1024, 1);
        ArrayList<Float> sampleData = new ArrayList<>((width / stepW) * (height / stepH));

        for (int j = 0; j < height; j += stepH) {
            Object lineData = pixelData[j];
            for (int i = 0; i < width; i += stepW) {
                float v = getValue(bpp, lineData, i, blank, bzero, bscale);
                if (v != ImageBuffer.BAD_PIXEL)
                    sampleData.add(v);
            }
        }
        return Floats.toArray(sampleData);
    }

    /*
        private static float[] getMinMax(int bpp, int width, int height, Object[] pixelData, long blank, double bzero, double bscale) {
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;

            for (int j = 0; j < height; j++) {
                Object lineData = pixelData[j];
                for (int i = 0; i < width; i++) {
                    float v = getValue(bpp, lineData, i, blank, bzero, bscale);
                    if (v != ImageBuffer.BAD_PIXEL) {
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

    // private static final double MIN_MULT = 0.0005;
    // private static final double MAX_MULT = 0.9995;
    private static final double MIN_MULT = 0.00001;
    private static final double MAX_MULT = 0.99999;

    private static ImageBuffer readHDU(BasicHDU<?> hdu, float[] minMax) throws Exception {
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
            return new ImageBuffer(width, height, ImageBuffer.Format.Gray8, ByteBuffer.wrap(outData));
        }

        double bzero = hdu.getBZero();
        double bscale = hdu.getBScale();

        if (minMax == null) {
            float[] sampleData = sampleImage(bpp, width, height, pixelData, blank, bzero, bscale);
            Arrays.sort(sampleData);

            // System.out.println(">>> " + sampleData.length + " " + (int) (MIN_MULT * sampleData.length) + " " + (int) (MAX_MULT * sampleData.length));
            minMax = new float[]{
                    sampleData[(int) (MIN_MULT * sampleData.length)],
                    sampleData[(int) (MAX_MULT * sampleData.length)]};

            // minMax = getMinMax(bpp, width, height, pixelData, blank, bzero, bscale);
            if (minMax[0] == minMax[1]) {
                minMax[1] = minMax[0] + 1;
            }
        }
        double range = minMax[1] - minMax[0];
        // System.out.println(">>> " + minMax[0] + ' ' + minMax[1]);

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
                        int p = (int) MathUtils.clip(scale * Math.pow(v - minMax[0], GAMMA) + .5, 0, 65535);
                        lut[p] = v;
                        outData[width * (height - 1 - j) + i] = v == ImageBuffer.BAD_PIXEL ? 0 : (short) p;
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
                        int p = (int) MathUtils.clip(scale * Math.log1p(v - minMax[0]) + .5, 0, 65535);
                        lut[p] = v;
                        outData[width * (height - 1 - j) + i] = v == ImageBuffer.BAD_PIXEL ? 0 : (short) p;
                    }
                }
                break;
            }
        }
        return new ImageBuffer(width, height, ImageBuffer.Format.Gray16, ShortBuffer.wrap(outData), lut);
    }

    private static final String nl = System.getProperty("line.separator");

    private static String getHeaderAsXML(Header header) {
        StringBuilder builder = new StringBuilder("<meta>" + nl + "<fits>" + nl);

        for (Cursor<String, HeaderCard> iter = header.iterator(); iter.hasNext(); ) {
            HeaderCard headerCard = iter.next();
            String key = headerCard.getKey();
            String value = headerCard.getValue();
            if (value != null) {
                builder.append('<').append(key).append('>').append(value).append("</").append(key).append('>').append(nl);
            }
        }
        builder.append("</fits>").append(nl).append("</meta>");
        return builder.toString().replace("&", "&amp;");
    }

}
