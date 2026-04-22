package org.helioviewer.jhv.opengl.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.io.FileUtils;

import org.lwjgl.system.MemoryUtil;

public final class TextFonts {
    private static final String CANVAS_FONT_RESOURCE = "/fonts/DejaVuSansCondensed.ttf";
    private static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 +-.,:;/()[]|?☉";
    private static final char FALLBACK_GLYPH = '?';
    private static final boolean[] supportedGlyphs = new boolean[Character.MAX_VALUE + 1];
    private static final Map<String, ByteBuffer> fontDataByPath = new HashMap<>();

    static {
        for (int i = 0; i < GLYPHS.length(); i++)
            supportedGlyphs[GLYPHS.charAt(i)] = true;
    }

    private TextFonts() {}

    public static ByteBuffer loadCanvasFontData() {
        return fontData(CANVAS_FONT_RESOURCE).duplicate();
    }

    static String glyphs() {
        return GLYPHS;
    }

    static char mapGlyph(char ch) {
        return supportedGlyphs[ch] ? ch : FALLBACK_GLYPH;
    }

    private static ByteBuffer fontData(String resourcePath) {
        ByteBuffer fontData = fontDataByPath.get(resourcePath);
        if (fontData != null)
            return fontData;

        try (InputStream is = FileUtils.getResource(resourcePath)) {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            fontDataByPath.put(resourcePath, buffer);
            return buffer;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load font " + resourcePath, e);
        }
    }

    public static void dispose() {
        for (ByteBuffer fontData : fontDataByPath.values())
            MemoryUtil.memFree(fontData);
        fontDataByPath.clear();
    }
}
