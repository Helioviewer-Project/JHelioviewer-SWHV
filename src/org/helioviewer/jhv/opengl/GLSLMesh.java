package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.opengl.model.ModelMaterial;
import org.helioviewer.jhv.opengl.model.ModelMesh;

final class GLSLMesh extends VAO1 {

    private static final int POSITION_BYTES = 3 * Float.BYTES;
    private static final int COLOR_BYTES = 4;
    private static final int TEX_COORD_BYTES = 2 * Float.BYTES;
    private static final int STRIDE = POSITION_BYTES + COLOR_BYTES + TEX_COORD_BYTES;

    private final ModelMesh data;
    private final ModelMaterial material;
    private final int indexCount;
    private final float centerX;
    private final float centerY;
    private final float centerZ;

    private GLBO elementBuffer;

    GLSLMesh(ModelMesh _data, ModelMaterial _material) {
        super(false, new VAA[]{
                new VAA(0, 3, false, STRIDE, 0, 0),
                new VAA(1, 4, true, STRIDE, POSITION_BYTES, 0),
                new VAA(2, 2, false, STRIDE, POSITION_BYTES + COLOR_BYTES, 0)
        });
        if (_data.primitive() != ModelMesh.Primitive.TRIANGLES)
            throw new IllegalArgumentException("GLSLMesh requires triangle geometry");
        data = _data;
        material = _material;
        indexCount = data.indices().remaining();

        FloatBuffer positions = data.positions();
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < data.vertexCount(); i++) {
            float x = positions.get(3 * i);
            float y = positions.get(3 * i + 1);
            float z = positions.get(3 * i + 2);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        centerX = 0.5f * (minX + maxX);
        centerY = 0.5f * (minY + maxY);
        centerZ = 0.5f * (minZ + maxZ);
    }

    @Override
    public void init() {
        super.init();
        vbo.setBufferData(Math.multiplyExact(data.vertexCount(), STRIDE), interleaveVertices());

        if (indexCount > 0) {
            bind();
            elementBuffer = new GLBO(GL.ELEMENT_ARRAY_BUFFER, GL.STATIC_DRAW);
            elementBuffer.setBufferData(Math.multiplyExact(indexCount, Integer.BYTES), data.indices());
        }
    }

    private ByteBuffer interleaveVertices() {
        int vertexCount = data.vertexCount();
        ByteBuffer buffer = BufferUtils.newByteBuffer(Math.multiplyExact(vertexCount, STRIDE));
        FloatBuffer positions = data.positions();
        ByteBuffer colors = data.colors();
        FloatBuffer texCoords = data.texCoords();

        for (int i = 0; i < vertexCount; i++) {
            buffer.putFloat(positions.get(3 * i));
            buffer.putFloat(positions.get(3 * i + 1));
            buffer.putFloat(positions.get(3 * i + 2));
            buffer.put(colors.get(4 * i));
            buffer.put(colors.get(4 * i + 1));
            buffer.put(colors.get(4 * i + 2));
            buffer.put(colors.get(4 * i + 3));
            buffer.putFloat(texCoords == null ? 0 : texCoords.get(2 * i));
            buffer.putFloat(texCoords == null ? 0 : texCoords.get(2 * i + 1));
        }
        return buffer.flip();
    }

    void render(GLTexture[] textures) {
        int textureIndex = material.baseColorTexture();
        boolean hasTexture = textureIndex != ModelMaterial.NO_TEXTURE;
        if (hasTexture)
            textures[textureIndex].bind();

        GLSLMeshShader.mesh.use();
        GLSLMeshShader.mesh.bind(material, hasTexture);
        bind();
        if (elementBuffer == null)
            GL.glDrawArrays(GL.TRIANGLES, 0, data.vertexCount());
        else
            GL.glDrawElements(GL.TRIANGLES, indexCount, GL.UNSIGNED_INT, 0);
    }

    @Override
    public void dispose() {
        if (elementBuffer != null) {
            elementBuffer.delete();
            elementBuffer = null;
        }
        super.dispose();
    }

    ModelMaterial material() {
        return material;
    }

    float centerX() {
        return centerX;
    }

    float centerY() {
        return centerY;
    }

    float centerZ() {
        return centerZ;
    }

}
