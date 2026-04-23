package org.helioviewer.jhv.opengl.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.io.FileUtils;

import org.lwjgl.system.MemoryUtil;

public final class TextFonts {
    private static final String FONT_RESOURCE = "/fonts/DejaVuSansCondensed.ttf";
    private static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 +-.,:;/()[]|?☉";
    private static final char FALLBACK_GLYPH = '?';
    private static final boolean[] supportedGlyphLookup = new boolean[Character.MAX_VALUE + 1];
    private static ByteBuffer cachedFontData;

    static {
        for (int i = 0; i < GLYPHS.length(); i++)
            supportedGlyphLookup[GLYPHS.charAt(i)] = true;
    }

    private TextFonts() {}

    public static ByteBuffer fontData() {
        return loadFontData().duplicate();
    }

    static String glyphs() {
        return GLYPHS;
    }

    static char fallbackGlyph() {
        return FALLBACK_GLYPH;
    }

    static char mapGlyph(char ch) {
        return supportedGlyphLookup[ch] ? ch : FALLBACK_GLYPH;
    }

    private static ByteBuffer loadFontData() {
        if (cachedFontData != null)
            return cachedFontData;

        try (InputStream is = FileUtils.getResource(FONT_RESOURCE)) {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            cachedFontData = buffer;
            return cachedFontData;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load font " + FONT_RESOURCE, e);
        }
    }

    public static void dispose() {
        MemoryUtil.memFree(cachedFontData);
        cachedFontData = null;
    }
}
