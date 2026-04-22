package org.helioviewer.jhv.opengl.text;

import java.nio.ByteBuffer;

import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLTexture;
import org.helioviewer.jhv.opengl.text.packrect.Rect;

import org.lwjgl.system.MemoryUtil;

class TextureRenderer {
    private ByteBuffer imageBuffer;
    private ByteBuffer copyScratch;

    private final GLTexture tex;
    private boolean dirty;
    private int dirtyX;
    private int dirtyY;
    private int dirtyWidth;
    private int dirtyHeight;

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

    void clear(int x, int y, int width, int height) {
        long base = MemoryUtil.memAddress(imageBuffer);
        int rowStride = imageWidth * 4;
        long clearBytes = (long) width * 4;
        for (int row = 0; row < height; row++) {
            long offset = (long) (y + row) * rowStride + (long) x * 4;
            MemoryUtil.memSet(base + offset, 0, clearBytes);
        }
    }

    void drawMask(int x, int y, int width, int height, ByteBuffer mask) {
        int rowStride = imageWidth * 4;
        int start = mask.position();
        for (int row = 0; row < height; row++) {
            int imageOffset = ((y + row) * rowStride) + (x * 4);
            for (int col = 0; col < width; col++) {
                int alpha = mask.get(start + row * width + col) & 0xFF;
                imageBuffer.putInt(imageOffset + (col * 4), alpha | (alpha << 8) | (alpha << 16) | (alpha << 24));
            }
        }
        imageBuffer.rewind();
    }

    void drawGlyphMask(Rect rect, int bitmapX, int bitmapY, int glyphWidth, int glyphHeight, ByteBuffer mask) {
        clear(rect.x(), rect.y(), rect.w(), rect.h());
        drawMask(bitmapX, bitmapY, glyphWidth, glyphHeight, mask);

        markDirty(rect.x(), rect.y(), rect.w(), rect.h());
    }

    void copyArea(int srcX, int srcY, int width, int height, int dstX, int dstY) {
        long base = MemoryUtil.memAddress(imageBuffer);
        int rowStride = imageWidth * 4;
        long rowBytes = (long) width * 4;
        ensureCopyScratch((int) rowBytes);
        for (int row = 0; row < height; row++) {
            long srcOffset = (long) (srcY + row) * rowStride + (long) srcX * 4;
            long dstOffset = (long) (dstY + row) * rowStride + (long) dstX * 4;
            long scratchAddress = MemoryUtil.memAddress(copyScratch);
            MemoryUtil.memCopy(base + srcOffset, scratchAddress, rowBytes);
            MemoryUtil.memCopy(scratchAddress, base + dstOffset, rowBytes);
        }
    }

    void copyFrom(TextureRenderer other, int srcX, int srcY, int width, int height, int dstX, int dstY) {
        long srcBase = MemoryUtil.memAddress(other.imageBuffer);
        long dstBase = MemoryUtil.memAddress(imageBuffer);
        int srcRowStride = other.imageWidth * 4;
        int dstRowStride = imageWidth * 4;
        long rowBytes = (long) width * 4;
        for (int row = 0; row < height; row++) {
            long srcOffset = (long) (srcY + row) * srcRowStride + (long) srcX * 4;
            long dstOffset = (long) (dstY + row) * dstRowStride + (long) dstX * 4;
            MemoryUtil.memCopy(srcBase + srcOffset, dstBase + dstOffset, rowBytes);
        }
    }

    void markDirty(int x, int y, int width, int height) {
        if (!dirty) {
            dirty = true;
            dirtyX = x;
            dirtyY = y;
            dirtyWidth = width;
            dirtyHeight = height;
        } else {
            int minX = Math.min(dirtyX, x);
            int minY = Math.min(dirtyY, y);
            int maxX = Math.max(dirtyX + dirtyWidth, x + width);
            int maxY = Math.max(dirtyY + dirtyHeight, y + height);
            dirtyX = minX;
            dirtyY = minY;
            dirtyWidth = maxX - minX;
            dirtyHeight = maxY - minY;
        }
    }

    void bind() {
        tex.bind();
        if (dirty) {
            upload(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
            dirty = false;
        }
    }

    void dispose() {
        tex.delete();
        MemoryUtil.memFree(copyScratch);
        copyScratch = null;
        MemoryUtil.memFree(imageBuffer);
        imageBuffer = null;
    }

    private void ensureCopyScratch(int size) {
        if (copyScratch == null || copyScratch.capacity() < size) {
            MemoryUtil.memFree(copyScratch);
            copyScratch = MemoryUtil.memAlloc(size);
        }
    }

    private void upload(int x, int y, int width, int height) {
        GL.glPixelStorei(GL.UNPACK_ALIGNMENT, 4);
        GL.glPixelStorei(GL.UNPACK_ROW_LENGTH, imageWidth);
        GL.glTexSubImage2D(GL.TEXTURE_2D, 0, x, y, width, height, GL.RGBA, GL.UNSIGNED_BYTE, imageBuffer.position(4 * (y * imageWidth + x)));
        GL.glGenerateMipmap(GL.TEXTURE_2D);
        imageBuffer.rewind();
    }
}
