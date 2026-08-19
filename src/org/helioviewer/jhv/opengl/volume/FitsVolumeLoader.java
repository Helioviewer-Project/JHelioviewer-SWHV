package org.helioviewer.jhv.opengl.volume;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.HeliocentricCartesianMetaData;
import org.helioviewer.jhv.time.JHVTime;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.Bitpix;
import nom.tam.fits.header.Standard;
import nom.tam.image.compression.hdu.CompressedImageHDU;

public final class FitsVolumeLoader {

    // Deliberately narrow cube profile: cell-centred heliocentric Cartesian SOLX/SOLY/SOLZ,
    // a linear three-dimensional WCS, and integer samples scaled through FITS BSCALE/BZERO.
    public static VolumeData load(Path path) throws IOException {
        Path source = path.toAbsolutePath().normalize();
        try (Fits fits = new Fits(source.toFile())) {
            ImageHDU hdu = findVolumeHDU(source, fits);
            Header header = hdu.getHeader();
            FitsMetadata metadata = new FitsMetadata(source, header);
            int[] dimensions = dimensions(source, header);
            Coordinates coordinates = readCoordinates(metadata, dimensions);
            Samples samples = readSamples(source, hdu, dimensions);
            String name = header.getStringValue("EXTNAME", header.getStringValue("OBJECT", source.getFileName().toString()));
            String sampleUnits = header.getStringValue(Standard.BUNIT, "");
            return new VolumeData(name, coordinates.time, dimensions[0], dimensions[1], dimensions[2], coordinates.corner, coordinates.axisX,
                    coordinates.axisY, coordinates.axisZ, sampleUnits, samples.minimum, samples.maximum, samples.format, samples.values,
                    samples.validityMask);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(source + ": " + e.getMessage(), e);
        }
    }

    private static ImageHDU findVolumeHDU(Path source, Fits fits) throws Exception {
        for (BasicHDU<?> hdu : fits.read()) {
            ImageHDU image = switch (hdu) {
                case CompressedImageHDU compressed -> compressed.asImageHDU();
                case ImageHDU uncompressed -> uncompressed;
                default -> null;
            };
            if (image != null && image.getHeader().getIntValue(Standard.NAXIS, 0) == 3)
                return image;
        }
        throw error(source, "no three-dimensional image HDU found");
    }

    private static int[] dimensions(Path source, Header header) throws IOException {
        int[] dimensions = {
                header.getIntValue("NAXIS1", 0),
                header.getIntValue("NAXIS2", 0),
                header.getIntValue("NAXIS3", 0)
        };
        if (dimensions[0] <= 0 || dimensions[1] <= 0 || dimensions[2] <= 0)
            throw error(source, "NAXIS1, NAXIS2, and NAXIS3 must be positive");
        return dimensions;
    }

    private static Coordinates readCoordinates(FitsMetadata metadata, int[] dimensions) throws IOException {
        HeliocentricCartesianMetaData.CartesianAxes axes = HeliocentricCartesianMetaData.cartesianAxes(metadata);
        double[] referencePixel = new double[3];
        double[] referenceValue = new double[3];
        for (int row = 0; row < 3; row++) {
            referencePixel[row] = metadata.number("CRPIX" + (row + 1));
            referenceValue[row] = metadata.number("CRVAL" + (row + 1)) * axes.scale(row);
        }

        double[][] matrix = readLinearTransform(metadata, axes);
        JHVTime time = HeliocentricCartesianMetaData.requiredObservationTime(metadata);
        if (!(metadata.number("DSUN_OBS") > 0))
            throw metadata.error("DSUN_OBS must be positive");
        // The observer rotation maps JHV's Carrington world into the observer-aligned SOL frame;
        // its inverse therefore places the FITS heliocentric coordinates in JHV's world.
        Quat observerRotation = HeliocentricCartesianMetaData.observerRotation(metadata, time);
        Vec3 reference = observerRotation.rotateInverseVector(new Vec3(
                referenceValue[axes.sourceAxis(0)], referenceValue[axes.sourceAxis(1)], referenceValue[axes.sourceAxis(2)]));
        Vec3[] step = new Vec3[3];
        for (int pixelAxis = 0; pixelAxis < 3; pixelAxis++) {
            step[pixelAxis] = observerRotation.rotateInverseVector(new Vec3(
                    matrix[axes.sourceAxis(0)][pixelAxis],
                    matrix[axes.sourceAxis(1)][pixelAxis],
                    matrix[axes.sourceAxis(2)][pixelAxis]));
        }

        Vec3 firstSample = new Vec3(
                reference.x + (1 - referencePixel[0]) * step[0].x + (1 - referencePixel[1]) * step[1].x +
                        (1 - referencePixel[2]) * step[2].x,
                reference.y + (1 - referencePixel[0]) * step[0].y + (1 - referencePixel[1]) * step[1].y +
                        (1 - referencePixel[2]) * step[2].y,
                reference.z + (1 - referencePixel[0]) * step[0].z + (1 - referencePixel[1]) * step[1].z +
                        (1 - referencePixel[2]) * step[2].z);
        Vec3 corner = new Vec3(
                firstSample.x - 0.5 * (step[0].x + step[1].x + step[2].x),
                firstSample.y - 0.5 * (step[0].y + step[1].y + step[2].y),
                firstSample.z - 0.5 * (step[0].z + step[1].z + step[2].z));
        return new Coordinates(
                time,
                corner,
                scale(step[0], dimensions[0]),
                scale(step[1], dimensions[1]),
                scale(step[2], dimensions[2]));
    }

    private static double[][] readLinearTransform(FitsMetadata metadata, HeliocentricCartesianMetaData.CartesianAxes axes) throws IOException {
        MatrixKeywords pc = readMatrix(metadata, "PC", 1);
        double[][] matrix;
        if (pc.present) {
            matrix = pc.values;
            for (int row = 0; row < 3; row++) {
                double rowScale = metadata.number("CDELT" + (row + 1)) * axes.scale(row);
                for (int column = 0; column < 3; column++)
                    matrix[row][column] *= rowScale;
            }
        } else {
            MatrixKeywords cd = readMatrix(metadata, "CD", 0);
            if (cd.present) {
                matrix = cd.values;
                for (int row = 0; row < 3; row++)
                    for (int column = 0; column < 3; column++)
                        matrix[row][column] *= axes.scale(row);
            } else {
                matrix = new double[3][3];
                for (int axis = 0; axis < 3; axis++)
                    matrix[axis][axis] = metadata.number("CDELT" + (axis + 1)) * axes.scale(axis);
            }
        }
        return matrix;
    }

    private static MatrixKeywords readMatrix(FitsMetadata metadata, String prefix, double diagonalDefault) throws IOException {
        boolean present = false;
        double[][] matrix = new double[3][3];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                String key = prefix + (row + 1) + '_' + (column + 1);
                boolean defined = metadata.contains(key);
                present |= defined;
                matrix[row][column] = defined ? metadata.number(key) : row == column ? diagonalDefault : 0;
            }
        }
        return new MatrixKeywords(present, matrix);
    }

    private static Vec3 scale(Vec3 v, double factor) {
        return new Vec3(factor * v.x, factor * v.y, factor * v.z);
    }

    private static Samples readSamples(Path source, ImageHDU hdu, int[] dimensions) throws Exception {
        Header header = hdu.getHeader();
        int bitpix = header.getIntValue(Standard.BITPIX, 0);
        if (bitpix != Bitpix.VALUE_FOR_BYTE && bitpix != Bitpix.VALUE_FOR_SHORT)
            throw error(source, "volume BITPIX must be 8 or 16");

        int sampleCount;
        try {
            sampleCount = Math.multiplyExact(Math.multiplyExact(dimensions[0], dimensions[1]), dimensions[2]);
        } catch (ArithmeticException e) {
            throw error(source, "volume dimensions are too large");
        }

        int[] axes = {dimensions[2], dimensions[1], dimensions[0]};
        Object pixels = hdu.getData().getTiler().getTile(new int[]{0, 0, 0}, axes);
        int pixelCount = switch (pixels) {
            case byte[] bytes -> bytes.length;
            case short[] shorts -> shorts.length;
            default -> throw error(source, "unexpected BITPIX=" + bitpix + " pixel buffer: " + pixels.getClass().getSimpleName());
        };
        if (pixelCount != sampleCount)
            throw error(source, "pixel buffer size does not match NAXIS1, NAXIS2, and NAXIS3");

        double bzero = header.getDoubleValue(Standard.BZERO, 0);
        double bscale = header.getDoubleValue(Standard.BSCALE, 1);
        if (!Double.isFinite(bzero) || !Double.isFinite(bscale) || bscale == 0)
            throw error(source, "invalid FITS BZERO/BSCALE");

        boolean hasBlank = header.containsKey(Standard.BLANK);
        long blank = hasBlank ? header.getLongValue(Standard.BLANK) : 0;
        long rawMinimum = bitpix == Bitpix.VALUE_FOR_BYTE ? 0 : Short.MIN_VALUE;
        long rawMaximum = bitpix == Bitpix.VALUE_FOR_BYTE ? 0xff : Short.MAX_VALUE;
        if (hasBlank && (blank < rawMinimum || blank > rawMaximum))
            throw error(source, "FITS BLANK is outside the BITPIX=" + bitpix + " stored-value range");
        return switch (pixels) {
            case byte[] bytes when bitpix == Bitpix.VALUE_FOR_BYTE ->
                    byteSamples(source, bytes, hasBlank, blank, bzero, bscale);
            case short[] shorts when bitpix == Bitpix.VALUE_FOR_SHORT ->
                    shortSamples(source, shorts, hasBlank, blank, bzero, bscale);
            default -> throw error(source, "unexpected BITPIX=" + bitpix + " pixel buffer: " + pixels.getClass().getSimpleName());
        };
    }

    private static Samples byteSamples(Path source, byte[] pixels, boolean hasBlank, long blank, double bzero, double bscale)
            throws IOException {
        ByteBuffer values = BufferUtils.newByteBuffer(pixels.length);
        ByteBuffer validityMask = hasBlank ? BufferUtils.newByteBuffer(pixels.length) : null;
        boolean hasDefinedSample = false;
        for (int i = 0; i < pixels.length; i++) {
            int raw = pixels[i] & 0xff;
            boolean defined = !hasBlank || raw != blank;
            int normalized = 0;
            if (defined)
                normalized = bscale > 0 ? raw : 0xff - raw;
            values.put(i, (byte) normalized);
            if (validityMask != null)
                validityMask.put(i, defined ? (byte) 0xff : 0);
            hasDefinedSample |= defined;
        }
        if (!hasDefinedSample)
            throw error(source, "volume contains no defined samples");
        return samples(source, values, validityMask, VolumeData.Format.R8, 0, 0xff, bzero, bscale);
    }

    private static Samples shortSamples(Path source, short[] pixels, boolean hasBlank, long blank, double bzero, double bscale)
            throws IOException {
        ShortBuffer values = BufferUtils.newShortBuffer(pixels.length);
        ByteBuffer validityMask = hasBlank ? BufferUtils.newByteBuffer(pixels.length) : null;
        boolean hasDefinedSample = false;
        for (int i = 0; i < pixels.length; i++) {
            int raw = pixels[i];
            boolean defined = !hasBlank || raw != blank;
            int normalized = 0;
            if (defined)
                normalized = bscale > 0 ? raw - Short.MIN_VALUE : Short.MAX_VALUE - raw;
            float value = normalized / 65535f;
            values.put(i, Float.floatToFloat16(value));
            if (validityMask != null)
                validityMask.put(i, defined ? (byte) 0xff : 0);
            hasDefinedSample |= defined;
        }
        if (!hasDefinedSample)
            throw error(source, "volume contains no defined samples");
        return samples(source, values, validityMask, VolumeData.Format.R16F, Short.MIN_VALUE, Short.MAX_VALUE, bzero, bscale);
    }

    private static Samples samples(Path source, Buffer values, ByteBuffer validityMask, VolumeData.Format format, long rawMinimum,
                                   long rawMaximum, double bzero, double bscale)
            throws IOException {
        double endpoint0 = bzero + rawMinimum * bscale;
        double endpoint1 = bzero + rawMaximum * bscale;
        float minimum = (float) Math.min(endpoint0, endpoint1);
        float maximum = (float) Math.max(endpoint0, endpoint1);
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum))
            throw error(source, "physical sample range is not representable as float");
        return new Samples(values, validityMask, minimum, maximum, format);
    }

    private static IOException error(Path source, String message) {
        return new IOException(source + ": " + message);
    }

    private record FitsMetadata(Path source, Header header) implements HeliocentricCartesianMetaData.Source {

        @Override
        public boolean contains(String key) {
            return header.containsKey(key);
        }

        @Override
        public String string(String key) throws IOException {
            if (!contains(key))
                throw error("missing required keyword: " + key);
            String value = header.getStringValue(key);
            if (value == null)
                throw error("invalid " + key);
            return value;
        }

        @Override
        public double number(String key) throws IOException {
            if (!contains(key))
                throw error("missing required keyword: " + key);
            double value = header.getDoubleValue(key, Double.NaN);
            if (!Double.isFinite(value))
                throw error("invalid " + key);
            return value;
        }

        @Override
        public IOException error(String message) {
            return FitsVolumeLoader.error(source, message);
        }
    }

    private record Samples(Buffer values, ByteBuffer validityMask, float minimum, float maximum, VolumeData.Format format) {}

    private record Coordinates(JHVTime time, Vec3 corner, Vec3 axisX, Vec3 axisY, Vec3 axisZ) {}

    private record MatrixKeywords(boolean present, double[][] values) {}

    private FitsVolumeLoader() {}
}
