package org.helioviewer.jhv.view.uri;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.helioviewer.jhv.base.ArrayUtils;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageFilter;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.Standard;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

//import com.google.common.base.Stopwatch;
import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;

// essentially static; local or network cache
public class FITSImage implements URIImageReader {

    private static final int BAD_PIXEL = Integer.MIN_VALUE;

    @Override
    public URIImageReader.Image readImage(File file) throws Exception {
        try (Fits f = new Fits(file)) {
            ImageHDU hdu = findHDU(f);
            return new URIImageReader.Image(getHeaderAsXML(hdu), readHDU(hdu, ImageFilter.Type.None), null);
        }
    }

    @Override
    public ImageBuffer readImageBuffer(File file, ImageFilter.Type filterType) throws Exception {
        try (Fits f = new Fits(file)) {
            return readHDU(findHDU(f), filterType);
        }
    }

    public ImageBuffer readImageBuffer(InputStream input) throws Exception {
        try (Fits f = new Fits(input)) {
            return readHDU(findHDU(f), ImageFilter.Type.None);
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

    private static final long BLANK = 0; // in case it doesn't exist, very unlikely value

    private static float floatPixel(double v, double bzero, double bscale) {
        if (Double.isNaN(v)) {
            return BAD_PIXEL;
        } else if (Double.isInfinite(v)) {
            return Float.MAX_VALUE;
        } else {
            return (float) (bzero + v * bscale);
        }
    }

    // private static final int SAMPLE = 8;
    private static final int SAMPLE = 4;
    private static final int MIN_SAMPLES = 10;

    private record SampleBuffer(float[] values, int length) {}

    private static SampleBuffer sampleImage(long blank, double bzero, double bscale, int width, int height, Object[] pixData) throws Exception {
        int stepW = Math.max(SAMPLE * width / 1024, 1);
        int stepH = Math.max(SAMPLE * height / 1024, 1);
        int sampleRows = (height + stepH - 1) / stepH;
        int sampleCols = (width + stepW - 1) / stepW;
        float[] samples = new float[sampleRows * sampleCols];
        int sampleLen = 0;
        boolean hasBlank = blank != BLANK;

        switch (pixData) {
            case short[][] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    short[] lineData = inData[j];
                    for (int i = 0; i < width; i += stepW) {
                        short raw = lineData[i];
                        float v = (hasBlank && raw == blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            case int[][] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    int[] lineData = inData[j];
                    for (int i = 0; i < width; i += stepW) {
                        int raw = lineData[i];
                        float v = (hasBlank && raw == blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            case long[][] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    long[] lineData = inData[j];
                    for (int i = 0; i < width; i += stepW) {
                        long raw = lineData[i];
                        float v = (hasBlank && raw == blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            case float[][] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    float[] lineData = inData[j];
                    for (int i = 0; i < width; i += stepW) {
                        float v = floatPixel(lineData[i], bzero, bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            case double[][] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    double[] lineData = inData[j];
                    for (int i = 0; i < width; i += stepW) {
                        float v = floatPixel(lineData[i], bzero, bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            default -> throw new Exception("Unknown pixel type: " + pixData.getClass().getSimpleName());
        }
        return new SampleBuffer(samples, sampleLen);
    }

    private static void processPixel(short[] outData, int outIdx, float v, float minV, float maxV, float range,
                                     FITSViewState.Data state, double scale) {
        if (v == BAD_PIXEL) {
            outData[outIdx] = 0;
        } else {
            v = Math.clamp(v, minV, maxV); // sampling may have missed extremes
            float d = v - minV;
            double mapped = state.mapScaled(d, range);
            outData[outIdx] = (short) Math.clamp((int) (scale * mapped + .5), 0, 65535);
        }
    }

    // private static final double MIN_MULT = 0.0005;
    // private static final double MAX_MULT = 0.9995;
    private static final double MIN_MULT = 0.00001;
    private static final double MAX_MULT = 0.99999;

    private static ImageBuffer readHDU(ImageHDU hdu, ImageFilter.Type filterType) throws Exception {
        int[] axes = hdu.getAxes();
        if (axes == null || axes.length != 2)
            throw new Exception("Only 2D FITS files supported");
        int height = axes[0];
        int width = axes[1];

        Object[] pixData = (Object[]) hdu.getData().getData();

        if (pixData instanceof byte[][] inData) {
            byte[] outData = new byte[width * height];
            for (int j = 0; j < height; j++) {
                System.arraycopy(inData[j], 0, outData, width * (height - 1 - j), width);
            }
            return new ImageBuffer(width, height, ImageBuffer.Format.Gray8, outData, filterType);
        }

        Header header = hdu.getHeader();
        long blank = header.getLongValue(Standard.BLANK, BLANK);
        double bzero = header.getDoubleValue(Standard.BZERO, 0);
        double bscale = header.getDoubleValue(Standard.BSCALE, 1);
        FITSViewState.Data state = FITSViewState.data();

        float min = header.getFloatValue("HV_DMIN", Float.MAX_VALUE);
        float max = header.getFloatValue("HV_DMAX", Float.MAX_VALUE);
        if (min == Float.MAX_VALUE || max == Float.MAX_VALUE) {
            if (state.clippingMode() == FITSViewState.ClippingMode.Range) {
                min = (float) state.clippingMin();
                max = (float) state.clippingMax();
            } else {
                boolean autoMode = state.clippingMode() == FITSViewState.ClippingMode.Auto;
                SampleBuffer sampleData = sampleImage(blank, bzero, bscale, width, height, pixData);
                int sampleLen = sampleData.length();
                if (sampleLen < MIN_SAMPLES) // couldn't find enough acceptable samples, return blank image
                    return new ImageBuffer(width, height, ImageBuffer.Format.Gray8, new byte[width * height], filterType);

                if (autoMode) {
                    int kMin = Math.clamp((int) (MIN_MULT * sampleLen), 0, sampleLen - 1);
                    int kMax = Math.clamp((int) (MAX_MULT * sampleLen), 0, sampleLen - 1);
                    float[] values = sampleData.values();
                    min = ArrayUtils.selectKth(values, 0, sampleLen - 1, kMin);
                    max = ArrayUtils.selectKth(values, 0, sampleLen - 1, kMax);
                } else {
                    Arrays.sort(sampleData.values(), 0, sampleLen);
                    float[] zLow = {0};
                    float[] zHigh = {0};
                    float[] zMax = {0};
                    ZScale.zscale(sampleData.values(), sampleLen, zLow, zHigh, zMax, state.zContrast());
                    min = zLow[0];
                    max = zHigh[0];
                }
            }
        }
        if (min >= max) {
            max = min + 1;
        }
        // System.out.println(">>> " + min + ' ' + max);

        short[] outData = new short[width * height];
        double scale = state.scaleFactor(min, max);
        float minV = min;
        float maxV = max;
        float range = maxV - minV;
        boolean hasBlank = blank != BLANK;

        //Stopwatch sw = Stopwatch.createStarted();
        switch (pixData) {
            case short[][] inData -> IntStream.range(0, height).parallel().forEach(j -> {
                short[] lineData = inData[j];
                int outLine = width * (height - 1 - j);

                for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                    short raw = lineData[i];
                    float v = (hasBlank && raw == blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
                    processPixel(outData, outIdx, v, minV, maxV, range, state, scale);
                }
            });
            case int[][] inData -> IntStream.range(0, height).parallel().forEach(j -> {
                int[] lineData = inData[j];
                int outLine = width * (height - 1 - j);

                for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                    int raw = lineData[i];
                    float v = (hasBlank && raw == blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
                    processPixel(outData, outIdx, v, minV, maxV, range, state, scale);
                }
            });
            case long[][] inData -> IntStream.range(0, height).parallel().forEach(j -> {
                long[] lineData = inData[j];
                int outLine = width * (height - 1 - j);

                for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                    long raw = lineData[i];
                    float v = (hasBlank && raw == blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
                    processPixel(outData, outIdx, v, minV, maxV, range, state, scale);
                }
            });
            case float[][] inData -> IntStream.range(0, height).parallel().forEach(j -> {
                float[] lineData = inData[j];
                int outLine = width * (height - 1 - j);

                for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                    float v = floatPixel(lineData[i], bzero, bscale);
                    processPixel(outData, outIdx, v, minV, maxV, range, state, scale);
                }
            });
            case double[][] inData -> IntStream.range(0, height).parallel().forEach(j -> {
                double[] lineData = inData[j];
                int outLine = width * (height - 1 - j);

                for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                    float v = floatPixel(lineData[i], bzero, bscale);
                    processPixel(outData, outIdx, v, minV, maxV, range, state, scale);
                }
            });
            default -> throw new Exception("Unknown pixel type: " + pixData.getClass().getSimpleName());
        }
        //System.out.println(">>> " + sw.elapsed().toNanos() / 1e9);
        return new ImageBuffer(width, height, ImageBuffer.Format.Gray16, outData, filterType);
    }

    private static final String nl = System.lineSeparator();
    private static final Escaper XML_CONTENT_ESCAPER = XmlEscapers.xmlContentEscaper();
    private static final Escaper XML_ATTRIBUTE_ESCAPER = XmlEscapers.xmlAttributeEscaper();

    private static String getHeaderAsXML(ImageHDU hdu) {
        StringBuilder builder = new StringBuilder("<meta>" + nl + "<fits>" + nl);

        for (Cursor<String, HeaderCard> iter = hdu.getHeader().iterator(); iter.hasNext(); ) {
            HeaderCard headerCard = iter.next();
            String key = headerCard.getKey().trim();
            if ("END".equals(key))
                continue;
            key = key.isEmpty() ? "COMMENT" : key.replace("$", "-"); // allow illegal keyword character in FITS saved by IDL

            String value = headerCard.getValue();
            String val = value == null ? "" : XML_CONTENT_ESCAPER.escape(value);
            String comment = headerCard.getComment();
            String com = comment == null ? "" : " comment=\"" + XML_ATTRIBUTE_ESCAPER.escape(comment) + "\"";

            builder.append('<').append(key).append(com).append('>').append(val).append("</").append(key).append('>').append(nl);
        }
        builder.append("</fits>").append(nl).append("</meta>");
        return builder.toString();
    }

}
