package org.helioviewer.jhv.opengl.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.io.FileUtils;

import org.lwjgl.system.MemoryUtil;

public final class TextFonts {
    private static final String CANVAS_FONT_RESOURCE = "/fonts/DejaVuSansCondensed.ttf";
    private static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 +-.,:;/()[]|?☉";
    private static final char FALLBACK_GLYPH = '?';
    private static final boolean[] supportedGlyphs = new boolean[Character.MAX_VALUE + 1];
    private static ByteBuffer fontData;

    static {
        for (int i = 0; i < GLYPHS.length(); i++)
            supportedGlyphs[GLYPHS.charAt(i)] = true;
    }

    private TextFonts() {}

    public static ByteBuffer loadCanvasFontData() {
        return fontData().duplicate();
    }

    static String glyphs() {
        return GLYPHS;
    }

    static char fallbackGlyph() {
        return FALLBACK_GLYPH;
    }

    static char mapGlyph(char ch) {
        return supportedGlyphs[ch] ? ch : FALLBACK_GLYPH;
    }

    private static ByteBuffer fontData() {
        if (fontData != null)
            return fontData;

        try (InputStream is = FileUtils.getResource(CANVAS_FONT_RESOURCE)) {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            fontData = buffer;
            return fontData;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load font " + CANVAS_FONT_RESOURCE, e);
        }
    }

    public static void dispose() {
        MemoryUtil.memFree(fontData);
        fontData = null;
    }
}
