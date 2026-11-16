package org.helioviewer.jhv.plugins.swek;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.io.FileUtils;

import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;

public final class SWEKIconBank {

    // Top-to-bottom, premultiplied RGBA
    record Pixels(int width, int height, byte[] rgba) {}

    // The full icon, then the icon at each square size, box-filtered from it
    static Pixels[] decode(String iconName, int[] sizes) throws IOException {
        String resource = resource(iconName);
        try (InputStream input = FileUtils.getResource(resource)) {
            ByteBuffer encoded = BufferUtils.directByteBuffer(ByteBuffer.wrap(input.readAllBytes()));
            int[] width = new int[1];
            int[] height = new int[1];
            int[] channels = new int[1];
            ByteBuffer decoded = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
            if (decoded == null)
                throw new IOException("Failed to decode " + resource + ": " + STBImage.stbi_failure_reason());
            try {
                Pixels[] pixels = new Pixels[1 + sizes.length];
                pixels[0] = premultiplied(decoded, width[0], height[0]);
                for (int i = 0; i < sizes.length; i++) {
                    int size = sizes[i];
                    ByteBuffer resized = BufferUtils.newByteBuffer(4 * size * size);
                    if (STBImageResize.stbir_resize(decoded, width[0], height[0], 0, resized, size, size, 0, STBImageResize.STBIR_RGBA,
                            STBImageResize.STBIR_TYPE_UINT8_SRGB, STBImageResize.STBIR_EDGE_CLAMP, STBImageResize.STBIR_FILTER_BOX) == null)
                        throw new IOException("Failed to resize " + resource);
                    pixels[i + 1] = premultiplied(resized, size, size);
                }
                return pixels;
            } finally {
                STBImage.stbi_image_free(decoded);
            }
        }
    }

    private static Pixels premultiplied(ByteBuffer straight, int width, int height) {
        byte[] rgba = new byte[4 * width * height];
        // Absolute read keeps the position stbi_image_free needs
        straight.get(0, rgba);
        for (int i = 0; i < rgba.length; i += 4) {
            int alpha = rgba[i + 3] & 0xff;
            for (int channel = 0; channel < 3; channel++)
                rgba[i + channel] = (byte) (((rgba[i + channel] & 0xff) * alpha + 127) / 255);
        }
        return new Pixels(width, height, rgba);
    }

    public static String resource(String iconName) {
        String file = switch (iconName) {
            case "ActiveRegion" -> "ar_icon.png";
            case "CoronalDimming" -> "cd_icon.png";
            case "CME" -> "ce_icon.png";
            case "CoronalHole" -> "ch_icon.png";
            case "CoronalWave" -> "cw_icon.png";
            case "EmergingFlux" -> "ef_icon.png";
            case "Eruption" -> "er_icon.png";
            case "FilamentActivation" -> "fa_icon.png";
            case "FilamentEruption" -> "fe_icon.png";
            case "Filament" -> "fi_icon.png";
            case "Flare" -> "fl_icon.png";
            case "Loop" -> "lp_icon.png";
            case "NothingReported" -> "nr_icon.png";
            case "Oscillation" -> "os_icon.png";
            case "Plage" -> "pg_icon.png";
            case "Sigmoid" -> "sg_icon.png";
            case "SpraySurge" -> "sp_icon.png";
            case "SunSpot" -> "ss_icon.png";
            case "Comesep" -> "comesep.png";
            case "RHESSI" -> "rhessi.png";
            default -> "ot_icon.png";
        };
        return "/images/EventIcons/" + file;
    }

}
