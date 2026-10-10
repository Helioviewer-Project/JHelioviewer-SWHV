package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.base.BufferUtils;

public final class GLTexture {

    public enum Unit {
        ZERO, ONE, TWO, THREE
    }

    public enum Format {
        R8(GL.R8, GL.RED, GL.UNSIGNED_BYTE, 1),
        R16F(GL.R16F, GL.RED, GL.HALF_FLOAT, 2),
        RGBA8(GL.RGBA8, GL.RGBA, GL.UNSIGNED_BYTE, 4);

        final int internalFormat;
        final int inputFormat;
        final int inputType;
        final int alignment;

        Format(int _internalFormat, int _inputFormat, int _inputType, int _alignment) {
            internalFormat = _internalFormat;
            inputFormat = _inputFormat;
            inputType = _inputType;
            alignment = _alignment;
        }
    }

    private int texID;
    private final int unit;
    private final int target;

    public GLTexture(int textureTarget, Unit textureUnit) {
        texID = GL.glGenTexture();
        target = textureTarget;
        unit = GL.TEXTURE0 + textureUnit.ordinal();
    }

    public void bind() {
        GL.glActiveTexture(unit);
        GL.glBindTexture(target, texID);
    }

    public void delete() {
        if (texID == -1)
            return;
        GL.glDeleteTexture(texID);
        texID = -1;
    }

    public void upload2D(Format format, int width, int height, int filter, ByteBuffer source) {
        upload2D(format, width, height, filter, filter, GL.CLAMP_TO_EDGE, GL.CLAMP_TO_EDGE, source);
    }

    public void upload2D(Format format, int width, int height, int minFilter, int magFilter, int wrapS, int wrapT, ByteBuffer source) {
        requireTarget(GL.TEXTURE_2D);
        if (invalid2DSize(width, height))
            return;

        boolean mipmaps = usesMipmaps(minFilter);
        bind();
        GL.glBindBuffer(GL.PIXEL_UNPACK_BUFFER, 0);
        GL.glPixelStorei(GL.UNPACK_ALIGNMENT, format.alignment);
        GL.glPixelStorei(GL.UNPACK_ROW_LENGTH, width);
        GL.glTexParameteri(target, GL.TEXTURE_BASE_LEVEL, 0);
        GL.glTexParameteri(target, GL.TEXTURE_MAX_LEVEL, mipmaps ? maxMipmapLevel(width, height) : 0);
        GL.glTexImage2D(target, 0, format.internalFormat, width, height, 0, format.inputFormat, format.inputType, source == null ? null : BufferUtils.directByteBuffer(source));
        GL.glTexParameteri(target, GL.TEXTURE_MIN_FILTER, minFilter);
        GL.glTexParameteri(target, GL.TEXTURE_MAG_FILTER, magFilter);
        GL.glTexParameteri(target, GL.TEXTURE_WRAP_S, wrapS);
        GL.glTexParameteri(target, GL.TEXTURE_WRAP_T, wrapT);
        if (mipmaps)
            GL.glGenerateMipmap(target);
    }

    private void requireTarget(int expectedTarget) {
        if (target != expectedTarget)
            throw new IllegalStateException("Texture target " + target + " does not support this operation");
    }

    static boolean invalid2DSize(int width, int height) {
        if (width >= 1 && height >= 1 && width <= GL.maxTextureSize && height <= GL.maxTextureSize)
            return false;
        Log.warn("w= " + width + " h=" + height);
        return true;
    }

    private static int maxMipmapLevel(int width, int height) {
        return 31 - Integer.numberOfLeadingZeros(Math.max(width, height));
    }

    private static boolean usesMipmaps(int filter) {
        return filter == GL.NEAREST_MIPMAP_NEAREST || filter == GL.LINEAR_MIPMAP_NEAREST
                || filter == GL.NEAREST_MIPMAP_LINEAR || filter == GL.LINEAR_MIPMAP_LINEAR;
    }
}
