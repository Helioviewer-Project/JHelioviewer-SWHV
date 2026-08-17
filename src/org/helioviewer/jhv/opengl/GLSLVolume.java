package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.image.lut.LUT;
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
    private GLTexture volumeTexture;
    private GLTexture validityMaskTexture;
    private GLTexture lutTexture;
    private LUT uploadedLut;

    public GLSLVolume(VolumeData _data) {
        super(false, new VAA[]{new VAA(0, 3, false, 0, 0, 0)});
        data = _data;
    }

    @Override
    public void init() {
        if (volumeTexture != null)
            return;
        try {
            super.init();
            FloatBuffer vertices = BufferUtils.newFloatBuffer(CUBE_VERTICES.length).put(CUBE_VERTICES).flip();
            vbo.setBufferData(CUBE_VERTICES.length * Float.BYTES, vertices);

            volumeTexture = new GLTexture(GL.TEXTURE_3D, GLTexture.Unit.THREE);
            volumeTexture.bind();
            switch (data.format()) {
                case R8 -> volumeTexture.copyByteVolume(data.width(), data.height(), data.depth(), (ByteBuffer) data.samples());
                case R16F -> volumeTexture.copyHalfFloatVolume(data.width(), data.height(), data.depth(), (ShortBuffer) data.samples());
            }

            validityMaskTexture = new GLTexture(GL.TEXTURE_3D, GLTexture.Unit.TWO);
            validityMaskTexture.bind();
            ByteBuffer mask = data.validityMask();
            if (mask == null)
                validityMaskTexture.copyByteVolume(1, 1, 1, ALL_VALID.duplicate());
            else
                validityMaskTexture.copyByteVolume(data.width(), data.height(), data.depth(), mask);

            lutTexture = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.ONE);
        } catch (RuntimeException | Error e) {
            dispose();
            throw e;
        }
    }

    public void render(LUT lut, double opacity, float[] cropMin, float[] cropMax) {
        volumeTexture.bind();
        validityMaskTexture.bind();
        lutTexture.bind();
        if (uploadedLut != lut) {
            ByteBuffer rgba = lut.rgba();
            GLTexture.copyByteImage(rgba.remaining() / 4, 1, GL.NEAREST, rgba);
            uploadedLut = lut;
        }
        GLSLVolumeShader.volume.use();
        GLSLVolumeShader.volume.bind(data, opacity, cropMin, cropMax);
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
        if (lutTexture != null) {
            lutTexture.delete();
            lutTexture = null;
            uploadedLut = null;
        }
        if (validityMaskTexture != null) {
            validityMaskTexture.delete();
            validityMaskTexture = null;
        }
        if (volumeTexture != null) {
            volumeTexture.delete();
            volumeTexture = null;
        }
        super.dispose();
    }

}
