package org.helioviewer.jhv.opengl.model;

import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.Nullable;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.HeliocentricCartesianMetaData;
import org.helioviewer.jhv.time.JHVTime;

import org.joml.Matrix4f;
import org.lwjgl.assimp.AIMetaData;
import org.lwjgl.assimp.AIMetaDataEntry;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;

final class AssimpSceneMetadata implements HeliocentricCartesianMetaData.Source {

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
        time = HeliocentricCartesianMetaData.observationTime(this);
        position = HeliocentricCartesianMetaData.hasCartesianAxes(this) ? readPosition() : new Matrix4f();
    }

    @Nullable JHVTime time() {
        return time;
    }

    Matrix4f position() {
        return position;
    }

    private Matrix4f readPosition() throws IOException {
        HeliocentricCartesianMetaData.CartesianAxes axes = HeliocentricCartesianMetaData.cartesianAxes(this);
        Quat worldToObserver = HeliocentricCartesianMetaData.observerRotation(this, time);
        Matrix4f result = new Matrix4f();
        for (int column = 0; column < 3; column++) {
            Vec3 axis = worldToObserver.rotateInverseVector(axes.vector(column));
            setColumn(result, column, axis);
        }
        return result;
    }

    private static void setColumn(Matrix4f matrix, int column, Vec3 axis) {
        float x = (float) axis.x;
        float y = (float) axis.y;
        float z = (float) axis.z;
        switch (column) {
            case 0 -> matrix.m00(x).m01(y).m02(z);
            case 1 -> matrix.m10(x).m11(y).m12(z);
            case 2 -> matrix.m20(x).m21(y).m22(z);
            default -> throw new AssertionError();
        }
    }

    @Override
    public double number(String key) throws IOException {
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

    @Override
    public String string(String key) throws IOException {
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

    @Override
    public boolean contains(String key) {
        return find(key) != null;
    }

    private static double unsignedLong(long value) {
        return value >= 0 ? value : (value & Long.MAX_VALUE) + 0x1p63;
    }

    @Override
    public IOException error(String message) {
        return new IOException(source + ": " + message);
    }
}
