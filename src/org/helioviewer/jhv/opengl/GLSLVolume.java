package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.opengl.volume.VolumeData;

public final class GLSLVolume extends VAO1 {

    private static final ByteBuffer ALL_VALID = BufferUtils.newByteBuffer(1).put((byte) 0xff).flip();

    private static final float[] CUBE_VERTICES = {
            0, 0, 0,  0, 1, 0,  1, 1, 0,  0, 0, 0,  1, 1, 0,  1, 0, 0,
            0, 0, 1,  1, 0, 1,  1, 1, 1,  0, 0, 1,  1, 1, 1,  0, 1, 1,
            0, 0, 0,  0, 0, 1,  0, 1, 1,  0, 0, 0,  0, 1, 1,  0, 1, 0,
            1, 0, 0,  1, 1, 0,  1, 1, 1,  1, 0, 0,  1, 1, 1,  1, 0, 1,
            0, 0, 0,  1, 0, 0,  1, 0, 1,  0, 0, 0,  1, 0, 1,  0, 0, 1,
            0, 1, 0,  0, 1, 1,  1, 1, 1,  0, 1, 0,  1, 1, 1,  1, 1, 0
    };

    private final VolumeData data;
    private GLTexture texture;
    private GLTexture validityMask;

    public GLSLVolume(VolumeData _data) {
        super(false, new VAA[]{new VAA(0, 3, false, 0, 0, 0)});
        data = _data;
    }

    @Override
    public void init() {
        if (texture != null)
            return;
        try {
            super.init();
            FloatBuffer vertices = BufferUtils.newFloatBuffer(CUBE_VERTICES.length).put(CUBE_VERTICES).flip();
            vbo.setBufferData(CUBE_VERTICES.length * Float.BYTES, vertices);

            texture = new GLTexture(GL.TEXTURE_3D, GLTexture.Unit.THREE);
            texture.bind();
            switch (data.format()) {
                case R8 -> texture.copyByteVolume(data.width(), data.height(), data.depth(), (ByteBuffer) data.samples());
                case R16F -> texture.copyHalfFloatVolume(data.width(), data.height(), data.depth(), (ShortBuffer) data.samples());
            }

            validityMask = new GLTexture(GL.TEXTURE_3D, GLTexture.Unit.TWO);
            validityMask.bind();
            ByteBuffer mask = data.validityMask();
            if (mask == null)
                validityMask.copyByteVolume(1, 1, 1, ALL_VALID.duplicate());
            else
                validityMask.copyByteVolume(data.width(), data.height(), data.depth(), mask);
        } catch (RuntimeException | Error e) {
            dispose();
            throw e;
        }
    }

    public void render() {
        texture.bind();
        validityMask.bind();
        GLSLVolumeShader.volume.use();
        GLSLVolumeShader.volume.bind(data);
        bind();

        boolean mirrored = data.determinant() < 0;
        if (mirrored)
            GL.glFrontFace(GL.CW);
        GL.glDepthMask(false);
        try {
            GL.glDrawArrays(GL.TRIANGLES, 0, CUBE_VERTICES.length / 3);
        } finally {
            GL.glDepthMask(true);
            if (mirrored)
                GL.glFrontFace(GL.CCW);
        }
    }

    @Override
    public void dispose() {
        if (validityMask != null) {
            validityMask.delete();
            validityMask = null;
        }
        if (texture != null) {
            texture.delete();
            texture = null;
        }
        super.dispose();
    }

}
