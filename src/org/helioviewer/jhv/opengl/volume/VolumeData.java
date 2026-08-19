package org.helioviewer.jhv.opengl.volume;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

public record VolumeData(String name, JHVTime time, int width, int height, int depth, Vec3 corner, Vec3 axisX, Vec3 axisY, Vec3 axisZ,
                         String sampleUnits, float minimum, float maximum, Format format, Buffer samples, @Nullable ByteBuffer validityMask) {

    public enum Format {R8, R16F}

    public VolumeData {
        if (width <= 0 || height <= 0 || depth <= 0)
            throw new IllegalArgumentException("Volume dimensions must be positive");
        int count = Math.multiplyExact(Math.multiplyExact(width, height), depth);
        if (samples.remaining() != count)
            throw new IllegalArgumentException("Volume sample count does not match its dimensions");
        samples = switch (format) {
            case R8 -> {
                if (!(samples instanceof ByteBuffer bytes))
                    throw new IllegalArgumentException("R8 volume samples must use a byte buffer");
                yield bytes.slice().asReadOnlyBuffer();
            }
            case R16F -> {
                if (!(samples instanceof ShortBuffer shorts))
                    throw new IllegalArgumentException("R16F volume samples must use a short buffer");
                yield shorts.slice().asReadOnlyBuffer();
            }
        };
        if (validityMask != null) {
            if (validityMask.remaining() != count)
                throw new IllegalArgumentException("Volume validity-mask size does not match its dimensions");
            validityMask = validityMask.slice().asReadOnlyBuffer();
        }
        if (!isFinite(corner) || !isFinite(axisX) || !isFinite(axisY) || !isFinite(axisZ))
            throw new IllegalArgumentException("Volume coordinate transform must be finite");
        double determinant = determinant(axisX, axisY, axisZ);
        if (!Double.isFinite(determinant) || determinant == 0)
            throw new IllegalArgumentException("Volume coordinate transform must be finite and nonsingular");
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || minimum > maximum)
            throw new IllegalArgumentException("Volume scalar range must be finite");
    }

    @Override
    public Buffer samples() {
        return switch (samples) {
            case ByteBuffer bytes -> bytes.duplicate();
            case ShortBuffer shorts -> shorts.duplicate();
            default -> throw new IllegalStateException("Unsupported volume sample buffer");
        };
    }

    @Override
    public @Nullable ByteBuffer validityMask() {
        return validityMask == null ? null : validityMask.duplicate();
    }

    public double determinant() {
        return determinant(axisX, axisY, axisZ);
    }

    private static double determinant(Vec3 axisX, Vec3 axisY, Vec3 axisZ) {
        return axisX.x * (axisY.y * axisZ.z - axisY.z * axisZ.y) -
                axisY.x * (axisX.y * axisZ.z - axisX.z * axisZ.y) +
                axisZ.x * (axisX.y * axisY.z - axisX.z * axisY.y);
    }

    private static boolean isFinite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

}
