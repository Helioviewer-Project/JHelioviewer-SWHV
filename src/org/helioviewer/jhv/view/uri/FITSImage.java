package org.helioviewer.jhv.view.uri;

import java.io.File;
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
import nom.tam.fits.header.Bitpix;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.math.MathUtils;

import com.google.common.primitives.Floats;
import com.google.common.xml.XmlEscapers;

import okio.BufferedSource;

// essentially static; local or network cache
class FITSImage implements URIImageReader {

    private static final String funpack = new File(JHVGlobals.libCacheDir, "fits_imcopy").getAbsolutePath();

    private static InputStream unpackFits(BufferedSource source) throws Exception {
        Process p = new ProcessBuilder(funpack, "-", "-").start();
        FileUtils.copySource(source, p.getOutputStream());
        return p.getInputStream();
    }

    @Nullable
    @Override
    public String readXML(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri);
             BufferedSource source = nc.getSource();
             Fits f = new Fits(unpackFits(source))) {
            return getHeaderAsXML(findHDU(f).getHeader());
        }
    }

    @Nullable
    @Override
    public ImageBuffer readImageBuffer(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri);
             BufferedSource source = nc.getSource();
             Fits f = new Fits(unpackFits(source))) {
            return readHDU(findHDU(f));
        }
    }

    private static ImageHDU findHDU(Fits fits) throws Exception {
        BasicHDU<?>[] hdus = fits.read();
        // this is cumbersome
        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof CompressedImageHDU chdu) {
                return chdu.asImageHDU();
            }
        }
        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof ImageHDU ihdu && ihdu.getAxes() != null /* might be an extension */) {
                return ihdu;
            }
        }
        throw new Exception("No image found");
    }

    private static final double GAMMA = 1 / 2.2;
    private static final long BLANK = 0; // in case it doesn't exist, very unlikely value

    private static float getValue(Bitpix bitpix, Object lineData, int i, long blank, double bzero, double bscale) {
        double v = switch (bitpix) {
            case SHORT -> ((short[]) lineData)[i];
            case INTEGER -> ((int[]) lineData)[i];
            case LONG -> ((long[]) lineData)[i];
            case FLOAT -> ((float[]) lineData)[i];
            case DOUBLE -> ((double[]) lineData)[i];
            default -> ImageBuffer.BAD_PIXEL;
        };
        return (blank != BLANK && v == blank) || !Double.isFinite(v) ? ImageBuffer.BAD_PIXEL : (float) (bzero + v * bscale);
    }

    // private static final int SAMPLE = 8;
    private static final int SAMPLE = 4;

    private static float[] sampleImage(Bitpix bitpix, int width, int height, Object[] pixelData, long blank, double bzero, double bscale) {
        int stepW = Math.max(SAMPLE * width / 1024, 1);
        int stepH = Math.max(SAMPLE * height / 1024, 1);
        ArrayList<Float> sampleData = new ArrayList<>((width / stepW) * (height / stepH));

        for (int j = 0; j < height; j += stepH) {
            Object lineData = pixelData[j];
            for (int i = 0; i < width; i += stepW) {
                float v = getValue(bitpix, lineData, i, blank, bzero, bscale);
                if (v != ImageBuffer.BAD_PIXEL)
                    sampleData.add(v);
            }
        }
        return Floats.toArray(sampleData);
    }

    /*
        private static float[] getMinMax(Bitpix bitpix, int width, int height, Object[] pixelData, long blank, double bzero, double bscale) {
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;

            for (int j = 0; j < height; j++) {
                Object lineData = pixelData[j];
                for (int i = 0; i < width; i++) {
                    float v = getValue(bitpix, lineData, i, blank, bzero, bscale);
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

    private static ImageBuffer readHDU(BasicHDU<?> hdu) throws Exception {
        int[] axes = hdu.getAxes();
        if (axes == null || axes.length != 2)
            throw new Exception("Only 2D FITS files supported");
        int height = axes[0];
        int width = axes[1];

        Bitpix bitpix = hdu.getBitpix();
        Object kernel = hdu.getKernel();
        if (!(kernel instanceof Object[] pixelData))
            throw new Exception("Cannot retrieve pixel data");

        long blank = BLANK;
        try {
            blank = hdu.getBlankValue();
        } catch (Exception ignore) {
        }

        if (bitpix == Bitpix.BYTE) {
            byte[][] inData = (byte[][]) pixelData;
            byte[] outData = new byte[width * height];
            for (int j = 0; j < height; j++) {
                System.arraycopy(inData[j], 0, outData, width * (height - 1 - j), width);
            }
            return new ImageBuffer(width, height, ImageBuffer.Format.Gray8, ByteBuffer.wrap(outData));
        }

        double bzero = hdu.getBZero();
        double bscale = hdu.getBScale();

        Header header = hdu.getHeader();
        float[] minMax = new float[]{header.getFloatValue("HV_DMIN", Float.MAX_VALUE), header.getFloatValue("HV_DMAX", Float.MAX_VALUE)};
        if (minMax[0] == Float.MAX_VALUE || minMax[1] == Float.MAX_VALUE) {
            float[] sampleData = sampleImage(bitpix, width, height, pixelData, blank, bzero, bscale);
            Arrays.sort(sampleData);

            // System.out.println(">>> " + sampleData.length + " " + (int) (MIN_MULT * sampleData.length) + " " + (int) (MAX_MULT * sampleData.length));
            minMax = new float[]{
                    sampleData[(int) (MIN_MULT * sampleData.length)],
                    sampleData[(int) (MAX_MULT * sampleData.length)]};

            // minMax = getMinMax(bitpix, width, height, pixelData, blank, bzero, bscale);
            if (minMax[0] == minMax[1]) {
                minMax[1] = minMax[0] + 1;
            }
        }
        double range = minMax[1] - minMax[0];
        // System.out.println(">>> " + minMax[0] + ' ' + minMax[1]);

        short[] outData = new short[width * height];
        float[] lut = new float[65536];
        switch (bitpix) {
            case SHORT, INTEGER, LONG, FLOAT -> {
                double scale = 65535. / Math.pow(range, GAMMA);
                for (int j = 0; j < height; j++) {
                    Object lineData = pixelData[j];
                    for (int i = 0; i < width; i++) {
                        float v = getValue(bitpix, lineData, i, blank, bzero, bscale);
                        int p = (int) MathUtils.clip(scale * Math.pow(v - minMax[0], GAMMA) + .5, 0, 65535);
                        lut[p] = v;
                        outData[width * (height - 1 - j) + i] = v == ImageBuffer.BAD_PIXEL ? 0 : (short) p;
                    }
                }
            }
            case DOUBLE -> {
                double scale = 65535. / Math.log1p(range);
                for (int j = 0; j < height; j++) {
                    Object lineData = pixelData[j];
                    for (int i = 0; i < width; i++) {
                        float v = getValue(bitpix, lineData, i, blank, bzero, bscale);
                        int p = (int) MathUtils.clip(scale * Math.log1p(v - minMax[0]) + .5, 0, 65535);
                        lut[p] = v;
                        outData[width * (height - 1 - j) + i] = v == ImageBuffer.BAD_PIXEL ? 0 : (short) p;
                    }
                }
            }
        }
        return new ImageBuffer(width, height, ImageBuffer.Format.Gray16, ShortBuffer.wrap(outData), lut);
    }

    private static final String nl = System.getProperty("line.separator");

    private static String getHeaderAsXML(Header header) {
        StringBuilder builder = new StringBuilder("<meta>" + nl + "<fits>" + nl);

        for (Cursor<String, HeaderCard> iter = header.iterator(); iter.hasNext(); ) {
            HeaderCard headerCard = iter.next();
            String key = headerCard.getKey().trim();
            if ("END".equals(key))
                continue;
            if (key.isEmpty())
                key = "COMMENT";

            String value = headerCard.getValue();
            String val = value == null ? "" : XmlEscapers.xmlContentEscaper().escape(value);
            String comment = headerCard.getComment();
            String com = comment == null ? "" : " comment=\"" + XmlEscapers.xmlAttributeEscaper().escape(comment) + "\"";

            builder.append('<').append(key).append(com).append('>').append(val).append("</").append(key).append('>').append(nl);
        }
        builder.append("</fits>").append(nl).append("</meta>");
        return builder.toString();
    }

}
