package org.helioviewer.jhv.view.uri;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.Standard;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.base.ArrayUtils;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.math.MathUtils;

//import com.google.common.base.Stopwatch;
import com.google.common.xml.XmlEscapers;

// essentially static; local or network cache
class FITSImage implements URIImageReader {

    @Override
    public URIImageReader.Image readImage(File file) throws Exception {
        try (Fits f = new Fits(file)) {
            ImageHDU hdu = findHDU(f);
            return new URIImageReader.Image(getHeaderAsXML(hdu), readHDU(hdu), null);
        }
    }

    @Override
    public ImageBuffer readImageBuffer(File file) throws Exception {
        try (Fits f = new Fits(file)) {
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

    private enum PixType {BYTE, SHORT, INT, LONG, FLOAT, DOUBLE}

    private static final long BLANK = 0; // in case it doesn't exist, very unlikely value

    @FunctionalInterface
    private interface PixelAccessor {
        float get(Object rowData, int x);
    }

    private static PixelAccessor accessor(PixType pixType, long blank, double bzero, double bscale) {
        return switch (pixType) {
            case SHORT -> (rowData, x) -> {
                short v = ((short[]) rowData)[x];
                return (blank != BLANK && v == blank) ? ImageBuffer.BAD_PIXEL : (float) (bzero + v * bscale);
            };
            case INT -> (rowData, x) -> {
                int v = ((int[]) rowData)[x];
                return (blank != BLANK && v == blank) ? ImageBuffer.BAD_PIXEL : (float) (bzero + v * bscale);
            };
            case LONG -> (rowData, x) -> {
                long v = ((long[]) rowData)[x];
                return (blank != BLANK && v == blank) ? ImageBuffer.BAD_PIXEL : (float) (bzero + v * bscale);
            };
            case FLOAT -> (rowData, x) -> floatPixel(((float[]) rowData)[x], bzero, bscale);
            case DOUBLE -> (rowData, x) -> floatPixel(((double[]) rowData)[x], bzero, bscale);
            case BYTE -> (rowData, x) -> ImageBuffer.BAD_PIXEL; // byte path handled separately
        };
    }

    private static float floatPixel(double v, double bzero, double bscale) {
        if (Double.isNaN(v)) {
            return ImageBuffer.BAD_PIXEL;
        } else if (Double.isInfinite(v)) {
            return Float.MAX_VALUE;
        } else {
            return (float) (bzero + v * bscale);
        }
    }

    // private static final int SAMPLE = 8;
    private static final int SAMPLE = 4;
    private static final int MIN_SAMPLES = 10;

    private record SampleBuffer(float[] values, int length) {
    }

    private static SampleBuffer sampleImage(PixelAccessor px, int width, int height, Object[] pixData) {
        int stepW = Math.max(SAMPLE * width / 1024, 1);
        int stepH = Math.max(SAMPLE * height / 1024, 1);
        int sampleRows = (height + stepH - 1) / stepH;
        int sampleCols = (width + stepW - 1) / stepW;
        float[] samples = new float[sampleRows * sampleCols];
        int sampleLen = 0;

        for (int j = 0; j < height; j += stepH) {
            Object lineData = pixData[j];
            for (int i = 0; i < width; i += stepW) {
                float v = px.get(lineData, i);
                if (v != ImageBuffer.BAD_PIXEL && v != Float.MAX_VALUE) {
                    samples[sampleLen++] = v;
                }
            }
        }
        return new SampleBuffer(samples, sampleLen);
    }

    // private static final double MIN_MULT = 0.0005;
    // private static final double MAX_MULT = 0.9995;
    private static final double MIN_MULT = 0.00001;
    private static final double MAX_MULT = 0.99999;

    private static ImageBuffer readHDU(ImageHDU hdu) throws Exception {
        int[] axes = hdu.getAxes();
        if (axes == null || axes.length != 2)
            throw new Exception("Only 2D FITS files supported");
        int height = axes[0];
        int width = axes[1];

        Object[] pixData = (Object[]) hdu.getData().getData();
        PixType pixType = getPixType(pixData);

        if (pixType == PixType.BYTE) {
            byte[][] inData = (byte[][]) pixData;
            byte[] outData = new byte[width * height];
            for (int j = 0; j < height; j++) {
                System.arraycopy(inData[j], 0, outData, width * (height - 1 - j), width);
            }
            return new ImageBuffer(width, height, ImageBuffer.Format.Gray8, ByteBuffer.wrap(outData));
        }

        Header header = hdu.getHeader();
        long blank = header.getLongValue(Standard.BLANK, BLANK);
        double bzero = header.getDoubleValue(Standard.BZERO, 0);
        double bscale = header.getDoubleValue(Standard.BSCALE, 1);
        PixelAccessor px = accessor(pixType, blank, bzero, bscale);

        float min = header.getFloatValue("HV_DMIN", Float.MAX_VALUE);
        float max = header.getFloatValue("HV_DMAX", Float.MAX_VALUE);
        if (min == Float.MAX_VALUE || max == Float.MAX_VALUE) {
            if (FITSSettings.clippingMode == FITSSettings.ClippingMode.Range) {
                min = (float) FITSSettings.clippingMin;
                max = (float) FITSSettings.clippingMax;
            } else {
                boolean autoMode = FITSSettings.clippingMode == FITSSettings.ClippingMode.Auto;
                SampleBuffer sampleData = sampleImage(px, width, height, pixData);
                int sampleLen = sampleData.length();
                if (sampleLen < MIN_SAMPLES) // couldn't find enough acceptable samples, return blank image
                    return new ImageBuffer(width, height, ImageBuffer.Format.Gray8, ByteBuffer.wrap(new byte[width * height]));

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
                    ZScale.zscale(sampleData.values(), sampleLen, zLow, zHigh, zMax, FITSSettings.zContrast);
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
        float[] lut = new float[65536];

        double scale = switch (FITSSettings.scalingMode) {
            case Gamma -> 65535. / fn_gamma(max - min);
            case Beta -> 65535. / fn_beta(max - min);
            case Alpha -> 65535. / fn_alpha(1);
        };
        float[] minMax = {min, max};

        //Stopwatch sw = Stopwatch.createStarted();
        switch (FITSSettings.scalingMode) {
            case Gamma -> IntStream.range(0, height).parallel().forEach(j -> {
                Object lineData = pixData[j];
                int outLine = width * (height - 1 - j);

                for (int i = 0; i < width; i++) {
                    float v = px.get(lineData, i);
                    if (v == ImageBuffer.BAD_PIXEL) {
                        outData[outLine + i] = 0;
                    } else {
                        v = Math.clamp(v, minMax[0], minMax[1]); // sampling may have missed extremes
                        int p = (int) Math.clamp(scale * fn_gamma(v - minMax[0]) + .5, 0, 65535);
                        lut[p] = v;
                        outData[outLine + i] = (short) p;
                    }
                }
            });
            case Beta -> IntStream.range(0, height).parallel().forEach(j -> {
                Object lineData = pixData[j];
                int outLine = width * (height - 1 - j);

                for (int i = 0; i < width; i++) {
                    float v = px.get(lineData, i);
                    if (v == ImageBuffer.BAD_PIXEL) {
                        outData[outLine + i] = 0;
                    } else {
                        v = Math.clamp(v, minMax[0], minMax[1]); // sampling may have missed extremes
                        int p = (int) Math.clamp(scale * fn_beta(v - minMax[0]) + .5, 0, 65535);
                        lut[p] = v;
                        outData[outLine + i] = (short) p;
                    }
                }
            });
            case Alpha -> IntStream.range(0, height).parallel().forEach(j -> {
                Object lineData = pixData[j];
                int outLine = width * (height - 1 - j);

                for (int i = 0; i < width; i++) {
                    float v = px.get(lineData, i);
                    if (v == ImageBuffer.BAD_PIXEL) {
                        outData[outLine + i] = 0;
                    } else {
                        v = Math.clamp(v, minMax[0], minMax[1]); // sampling may have missed extremes
                        int p = (int) Math.clamp(scale * fn_alpha((v - minMax[0]) / (minMax[1] - minMax[0])) + .5, 0, 65535);
                        lut[p] = v;
                        outData[outLine + i] = (short) p;
                    }
                }
            });
        }
        //System.out.println(">>> " + sw.elapsed().toNanos() / 1e9);
        return new ImageBuffer(width, height, ImageBuffer.Format.Gray16, ShortBuffer.wrap(outData), lut);
    }

    private static PixType getPixType(Object[] pixData) throws Exception {
        if (pixData instanceof byte[][])
            return PixType.BYTE;
        else if (pixData instanceof short[][])
            return PixType.SHORT;
        else if (pixData instanceof int[][])
            return PixType.INT;
        else if (pixData instanceof long[][])
            return PixType.LONG;
        else if (pixData instanceof float[][])
            return PixType.FLOAT;
        else if (pixData instanceof double[][])
            return PixType.DOUBLE;
        else
            throw new Exception("Unknown pixel type: " + pixData.getClass().getSimpleName());
    }

    private static double fn_gamma(double x) {
        return Math.pow(x, FITSSettings.GAMMA);
    }

    private static double fn_beta(double x) {
        return MathUtils.asinh(x * FITSSettings.BETA);
    }

    private static double fn_alpha(double x) {
        return Math.log1p(x * FITSSettings.ALPHA);
    }

    private static final String nl = System.lineSeparator();

    private static String getHeaderAsXML(ImageHDU hdu) {
        StringBuilder builder = new StringBuilder("<meta>" + nl + "<fits>" + nl);

        for (Cursor<String, HeaderCard> iter = hdu.getHeader().iterator(); iter.hasNext(); ) {
            HeaderCard headerCard = iter.next();
            String key = headerCard.getKey().trim();
            if ("END".equals(key))
                continue;
            key = key.isEmpty() ? "COMMENT" : key.replace("$", "-"); // allow illegal keyword character in FITS saved by IDL

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
