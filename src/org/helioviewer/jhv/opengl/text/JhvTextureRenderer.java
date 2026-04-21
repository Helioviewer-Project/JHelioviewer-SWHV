package org.helioviewer.jhv.opengl.text;

import java.awt.Rectangle;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLTexture;

import org.lwjgl.system.MemoryUtil;

class JhvTextureRenderer {
    private ByteBuffer imageBuffer;
    private byte[] clearRow;

    private final GLTexture tex;
    private Rectangle dirtyRegion;

    private final int imageWidth;
    private final int imageHeight;

    JhvTextureRenderer(int width, int height) {
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

    void clear(int x, int y, int width, int height) {
        if (clearRow == null || clearRow.length < width * 4)
            clearRow = new byte[width * 4];
        int rowStride = imageWidth * 4;
        for (int row = 0; row < height; row++) {
            int offset = ((y + row) * rowStride) + (x * 4);
            imageBuffer.position(offset);
            imageBuffer.put(clearRow, 0, width * 4);
        }
        imageBuffer.rewind();
    }

    void drawMask(int x, int y, int width, int height, ByteBuffer mask) {
        int rowStride = imageWidth * 4;
        int start = mask.position();
        for (int row = 0; row < height; row++) {
            int imageOffset = ((y + row) * rowStride) + (x * 4);
            for (int col = 0; col < width; col++) {
                int alpha = mask.get(start + row * width + col) & 0xFF;
                imageBuffer.put(imageOffset + (col * 4), (byte) alpha);
                imageBuffer.put(imageOffset + (col * 4) + 1, (byte) alpha);
                imageBuffer.put(imageOffset + (col * 4) + 2, (byte) alpha);
                imageBuffer.put(imageOffset + (col * 4) + 3, (byte) alpha);
            }
        }
        imageBuffer.rewind();
    }

    void drawRgba(int x, int y, int width, int height, ByteBuffer rgba, int rowStride) {
        ByteBuffer src = rgba.duplicate();
        int dstRowStride = imageWidth * 4;
        for (int row = 0; row < height; row++) {
            int srcOffset = row * rowStride;
            int dstOffset = ((y + row) * dstRowStride) + (x * 4);
            src.position(srcOffset).limit(srcOffset + width * 4);
            imageBuffer.position(dstOffset);
            imageBuffer.put(src);
            src.clear();
        }
        imageBuffer.rewind();
    }

    void copyArea(int srcX, int srcY, int width, int height, int dstX, int dstY) {
        byte[] tmp = new byte[width * height * 4];
        int rowStride = imageWidth * 4;
        for (int row = 0; row < height; row++) {
            int srcOffset = ((srcY + row) * rowStride) + (srcX * 4);
            imageBuffer.position(srcOffset);
            imageBuffer.get(tmp, row * width * 4, width * 4);
        }
        for (int row = 0; row < height; row++) {
            int dstOffset = ((dstY + row) * rowStride) + (dstX * 4);
            imageBuffer.position(dstOffset);
            imageBuffer.put(tmp, row * width * 4, width * 4);
        }
        imageBuffer.rewind();
    }

    void copyFrom(JhvTextureRenderer other, int srcX, int srcY, int width, int height, int dstX, int dstY) {
        byte[] tmp = new byte[width * height * 4];
        int srcRowStride = other.imageWidth * 4;
        for (int row = 0; row < height; row++) {
            int srcOffset = ((srcY + row) * srcRowStride) + (srcX * 4);
            other.imageBuffer.position(srcOffset);
            other.imageBuffer.get(tmp, row * width * 4, width * 4);
        }
        drawRgba(dstX, dstY, width, height, ByteBuffer.wrap(tmp), width * 4);
    }

    void markDirty(int x, int y, int width, int height) {
        Rectangle curRegion = new Rectangle(x, y, width, height);
        if (dirtyRegion == null) {
            dirtyRegion = curRegion;
        } else {
            dirtyRegion.add(curRegion);
        }
    }

    void bind() {
        tex.bind();
        if (dirtyRegion != null) {
            upload(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
            dirtyRegion = null;
        }
    }

    void dispose() {
        tex.delete();
        MemoryUtil.memFree(imageBuffer);
        imageBuffer = null;
        clearRow = null;
    }

    private void upload(int x, int y, int width, int height) {
        GL.glPixelStorei(GL.UNPACK_ALIGNMENT, 4);
        GL.glPixelStorei(GL.UNPACK_ROW_LENGTH, imageWidth);
        GL.glTexSubImage2D(GL.TEXTURE_2D, 0, x, y, width, height, GL.RGBA, GL.UNSIGNED_BYTE, imageBuffer.position(4 * (y * imageWidth + x)));
        GL.glGenerateMipmap(GL.TEXTURE_2D);
        imageBuffer.rewind();
    }
}
