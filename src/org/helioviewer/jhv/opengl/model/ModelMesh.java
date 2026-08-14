package org.helioviewer.jhv.opengl.model;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

public record ModelMesh(String name, Primitive primitive, FloatBuffer positions, ByteBuffer colors, @Nullable FloatBuffer texCoords,
                        IntBuffer indices, IntBuffer lineOffsets, int materialIndex) {

    public enum Primitive {
        POINTS,
        LINES,
        TRIANGLES
    }

    public ModelMesh {
        positions = positions.slice().asReadOnlyBuffer();
        colors = colors.slice().asReadOnlyBuffer();
        if (texCoords != null)
            texCoords = texCoords.slice().asReadOnlyBuffer();
        indices = indices.slice().asReadOnlyBuffer();
        lineOffsets = lineOffsets.slice().asReadOnlyBuffer();

        int vertexCount = positions.remaining() / 3;
        if (positions.remaining() != vertexCount * 3)
            throw new IllegalArgumentException("Position buffer is not a sequence of vec3 values");
        if (colors.remaining() != vertexCount * 4)
            throw new IllegalArgumentException("Color buffer does not contain one RGBA value per vertex");
        if (texCoords != null && texCoords.remaining() != vertexCount * 2)
            throw new IllegalArgumentException("Texture-coordinate buffer does not contain one vec2 value per vertex");
        if (primitive != Primitive.LINES && lineOffsets.hasRemaining())
            throw new IllegalArgumentException("Only line meshes can have line offsets");
    }

    public int vertexCount() {
        return positions.remaining() / 3;
    }

    @Override
    public FloatBuffer positions() {
        return positions.duplicate();
    }

    @Override
    public ByteBuffer colors() {
        return colors.duplicate();
    }

    @Nullable
    @Override
    public FloatBuffer texCoords() {
        return texCoords == null ? null : texCoords.duplicate();
    }

    @Override
    public IntBuffer indices() {
        return indices.duplicate();
    }

    @Override
    public IntBuffer lineOffsets() {
        return lineOffsets.duplicate();
    }

}
