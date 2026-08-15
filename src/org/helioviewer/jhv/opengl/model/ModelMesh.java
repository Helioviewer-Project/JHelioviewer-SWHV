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
        if (vertexCount == 0)
            throw new IllegalArgumentException("Mesh has no vertices");
        if (colors.remaining() != vertexCount * 4)
            throw new IllegalArgumentException("Color buffer does not contain one RGBA value per vertex");
        if (texCoords != null && texCoords.remaining() != vertexCount * 2)
            throw new IllegalArgumentException("Texture-coordinate buffer does not contain one vec2 value per vertex");
        if (primitive != Primitive.LINES && lineOffsets.hasRemaining())
            throw new IllegalArgumentException("Only line meshes can have line offsets");

        for (int i = 0; i < indices.remaining(); i++) {
            int index = indices.get(i);
            if (index < 0 || index >= vertexCount)
                throw new IllegalArgumentException("Vertex index out of range: " + index);
        }

        switch (primitive) {
            case TRIANGLES -> {
                int count = indices.hasRemaining() ? indices.remaining() : vertexCount;
                if (count % 3 != 0)
                    throw new IllegalArgumentException("Triangle mesh does not contain a whole number of triangles");
            }
            case LINES -> validateLines(indices, lineOffsets);
            case POINTS -> {
                if (!indices.hasRemaining())
                    throw new IllegalArgumentException("Point mesh has no indices");
            }
        }
    }

    private static void validateLines(IntBuffer indices, IntBuffer offsets) {
        if (!indices.hasRemaining())
            throw new IllegalArgumentException("Line mesh has no indices");
        if (offsets.remaining() < 2 || offsets.get(0) != 0 || offsets.get(offsets.remaining() - 1) != indices.remaining())
            throw new IllegalArgumentException("Line offsets must span the index buffer");
        for (int i = 1; i < offsets.remaining(); i++) {
            if (offsets.get(i) - offsets.get(i - 1) < 2)
                throw new IllegalArgumentException("Each line must contain at least two vertices");
        }
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
