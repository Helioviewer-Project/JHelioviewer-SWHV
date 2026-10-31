package org.helioviewer.jhv.base;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.stb.STBImage;

// A decoded image, top-to-bottom, premultiplied RGBA, owned by the Java heap.
public record PngPixels(int width, int height, byte[] rgba) {

    public static PngPixels decode(byte[] png) throws IOException {
        ByteBuffer encoded = BufferUtils.directByteBuffer(ByteBuffer.wrap(png));
        int[] width = new int[1];
        int[] height = new int[1];
        int[] channels = new int[1];
        ByteBuffer decoded = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
        if (decoded == null)
            throw new IOException("Failed to decode image: " + STBImage.stbi_failure_reason());
        try {
            byte[] rgba = new byte[decoded.remaining()];
            // stbi_image_free needs the original buffer position.
            decoded.get(0, rgba);
            for (int i = 0; i < rgba.length; i += 4) {
                int alpha = rgba[i + 3] & 0xff;
                for (int channel = 0; channel < 3; channel++)
                    rgba[i + channel] = (byte) (((rgba[i + channel] & 0xff) * alpha + 127) / 255);
            }
            return new PngPixels(width[0], height[0], rgba);
        } finally {
            STBImage.stbi_image_free(decoded);
        }
    }

    public BufferedImage image() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        // Raster elements are RGBA; the standard image stores them as ABGR.
        image.getRaster().setDataElements(0, 0, width, height, rgba);
        return image;
    }

}
