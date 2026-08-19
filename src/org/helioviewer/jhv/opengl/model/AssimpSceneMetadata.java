package org.helioviewer.jhv.opengl.model;

import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

import org.joml.Matrix4f;
import org.lwjgl.assimp.AIMetaData;
import org.lwjgl.assimp.AIMetaDataEntry;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;

final class AssimpSceneMetadata {

    private final Path source;
    private final @Nullable AIMetaData metadata;
    private final @Nullable JHVTime time;
    private final Matrix4f position;

    static AssimpSceneMetadata read(Path source, @Nullable AIMetaData metadata) throws IOException {
        return new AssimpSceneMetadata(source, metadata);
    }

    private AssimpSceneMetadata(Path _source, @Nullable AIMetaData _metadata) throws IOException {
        source = _source;
        metadata = _metadata;
        time = readTime();
        position = hasCoordinateMetadata() ? readPosition() : new Matrix4f();
    }

    @Nullable JHVTime time() {
        return time;
    }

    Matrix4f position() {
        return position;
    }

    private @Nullable JHVTime readTime() throws IOException {
        for (String key : new String[]{"DATE-AVG", "DATE_AVG", "DATE_OBS", "DATE-OBS"}) {
            if (find(key) == null)
                continue;
            String value = string(key).strip();
            if (value.endsWith("Z"))
                value = value.substring(0, value.length() - 1);
            if (value.length() == 10)
                value += "T00:00:00";
            try {
                return new JHVTime(value);
            } catch (RuntimeException e) {
                throw error("invalid " + key + ": " + value);
            }
        }
        return null;
    }

    private boolean hasCoordinateMetadata() {
        for (String key : new String[]{"CTYPE1", "CTYPE2", "CTYPE3", "CUNIT1", "CUNIT2", "CUNIT3"}) {
            if (find(key) != null)
                return true;
        }
        return false;
    }

    private Matrix4f readPosition() throws IOException {
        double carringtonLongitude = Math.toRadians(number("CRLN_OBS"));
        double observerLatitude = Math.toRadians(number("CRLT_OBS"));
        if (Math.abs(observerLatitude) > Math.PI / 2)
            throw error("CRLT_OBS must be between -90 and 90 degrees");
        Quat worldToObserver = Quat.createXY(observerLatitude, -carringtonLongitude);

        Matrix4f result = new Matrix4f();
        boolean[] components = new boolean[3];
        for (int column = 0; column < 3; column++) {
            String ctype = string("CTYPE" + (column + 1));
            int component;
            Vec3 axis;
            switch (ctype) {
                case "SOLX" -> {
                    component = 0;
                    axis = Vec3.XAxis;
                }
                case "SOLY" -> {
                    component = 1;
                    axis = Vec3.YAxis;
                }
                case "SOLZ" -> {
                    component = 2;
                    axis = Vec3.ZAxis;
                }
                default -> throw error("CTYPE1, CTYPE2, and CTYPE3 must contain SOLX, SOLY, and SOLZ");
            }
            if (components[component])
                throw error("CTYPE1, CTYPE2, and CTYPE3 must contain SOLX, SOLY, and SOLZ exactly once");
            components[component] = true;

            double scale = unitScale("CUNIT" + (column + 1));
            setColumn(result, column, worldToObserver.rotateInverseVector(axis), scale);
        }
        return result;
    }

    private double unitScale(String key) throws IOException {
        return switch (string(key)) {
            case "solRad" -> 1;
            case "m" -> 1 / Sun.RadiusMeter;
            case "km" -> 1_000 / Sun.RadiusMeter;
            case "Mm" -> 1_000_000 / Sun.RadiusMeter;
            default -> throw error(key + " must be solRad, m, km, or Mm");
        };
    }

    private static void setColumn(Matrix4f matrix, int column, Vec3 axis, double scale) {
        float x = (float) (scale * axis.x);
        float y = (float) (scale * axis.y);
        float z = (float) (scale * axis.z);
        switch (column) {
            case 0 -> matrix.m00(x).m01(y).m02(z);
            case 1 -> matrix.m10(x).m11(y).m12(z);
            case 2 -> matrix.m20(x).m21(y).m22(z);
            default -> throw new AssertionError();
        }
    }

    private double number(String key) throws IOException {
        AIMetaDataEntry entry = required(key);
        double value = switch (entry.mType()) {
            case Assimp.AI_INT32 -> entry.mData(Integer.BYTES).getInt();
            case Assimp.AI_UINT64 -> unsignedLong(entry.mData(Long.BYTES).getLong());
            case Assimp.AI_FLOAT -> entry.mData(Float.BYTES).getFloat();
            case Assimp.AI_DOUBLE -> entry.mData(Double.BYTES).getDouble();
            default -> throw error(key + " must be numeric");
        };
        if (!Double.isFinite(value))
            throw error(key + " must be finite");
        return value;
    }

    private String string(String key) throws IOException {
        AIMetaDataEntry entry = required(key);
        if (entry.mType() != Assimp.AI_AISTRING)
            throw error(key + " must be a string");
        return AIString.create(MemoryUtil.memAddress(entry.mData(AIString.SIZEOF))).dataString();
    }

    private AIMetaDataEntry required(String key) throws IOException {
        AIMetaDataEntry entry = find(key);
        if (entry == null)
            throw error("missing " + key + " in positional metadata");
        return entry;
    }

    private @Nullable AIMetaDataEntry find(String key) {
        if (metadata == null)
            return null;
        for (int i = 0; i < metadata.mNumProperties(); i++) {
            if (metadata.mKeys().get(i).dataString().equals(key))
                return metadata.mValues().get(i);
        }
        return null;
    }

    private static double unsignedLong(long value) {
        return value >= 0 ? value : (value & Long.MAX_VALUE) + 0x1p63;
    }

    private IOException error(String message) {
        return new IOException(source + ": " + message);
    }
}
