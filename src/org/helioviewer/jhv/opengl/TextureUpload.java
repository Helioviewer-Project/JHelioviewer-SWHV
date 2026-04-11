package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;

import org.helioviewer.jhv.base.BufferUtils;

final class TextureUpload {

    private TextureUpload() {
    }

    static ByteBuffer packArgbToRgbaBytes(int[] argb) {
        ByteBuffer rgba = BufferUtils.newByteBuffer(argb.length * 4);
        for (int pixel : argb) {
            int alpha = pixel >>> 24;
            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = pixel & 0xff;
            rgba.put((byte) red);
            rgba.put((byte) green);
            rgba.put((byte) blue);
            rgba.put((byte) alpha);
        }
        rgba.flip();
        return rgba;
    }
}
