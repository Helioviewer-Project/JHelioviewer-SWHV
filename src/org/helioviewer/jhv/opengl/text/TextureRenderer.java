package org.helioviewer.jhv.opengl.text;

import java.nio.ByteBuffer;

import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLTexture;

import org.lwjgl.system.MemoryUtil;

final class TextureRenderer {
    private ByteBuffer imageBuffer;

    private final GLTexture tex;
    private boolean dirty;

    private final int imageWidth;
    private final int imageHeight;

    TextureRenderer(int width, int height) {
        imageWidth = width;
        imageHeight = height;
        imageBuffer = MemoryUtil.memCalloc(imageWidth * imageHeight * 4);

        tex = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);
        tex.bind();
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_BASE_LEVEL, 0);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAX_LEVEL, 15);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR_MIPMAP_LINEAR);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE);
        GL.glTexImage2D(GL.TEXTURE_2D, 0, GL.RGBA, imageWidth, imageHeight, 0, GL.RGBA, GL.UNSIGNED_BYTE, (ByteBuffer) null);
    }

    int getWidth() {
        return imageWidth;
    }

    int getHeight() {
        return imageHeight;
    }

    void drawMask(int x, int y, int width, int height, ByteBuffer mask) {
        int rowStride = imageWidth * 4;
        int start = mask.position();
        for (int row = 0; row < height; row++) {
            int maskOffset = start + row * width;
            int imageOffset = ((y + row) * rowStride) + (x * 4);
            for (int col = 0; col < width; col++) {
                int alpha = mask.get(maskOffset++) & 0xFF;
                imageBuffer.putInt(imageOffset, alpha | (alpha << 8) | (alpha << 16) | (alpha << 24));
                imageOffset += 4;
            }
        }
    }

    void markDirty() {
        dirty = true;
    }

    void bind() {
        tex.bind();
        if (dirty) {
            upload();
            dirty = false;
        }
    }

    void dispose() {
        tex.delete();
        MemoryUtil.memFree(imageBuffer);
        imageBuffer = null;
    }

    private void upload() {
        GL.glPixelStorei(GL.UNPACK_ALIGNMENT, 4);
        GL.glPixelStorei(GL.UNPACK_ROW_LENGTH, imageWidth);
        GL.glTexSubImage2D(GL.TEXTURE_2D, 0, 0, 0, imageWidth, imageHeight, GL.RGBA, GL.UNSIGNED_BYTE, imageBuffer);
        GL.glGenerateMipmap(GL.TEXTURE_2D);
    }
}
