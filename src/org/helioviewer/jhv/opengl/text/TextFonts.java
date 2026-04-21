package org.helioviewer.jhv.opengl.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.io.FileUtils;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class TextFonts {
    private static final String CANVAS_FONT_RESOURCE = "/fonts/DejaVuSansCondensed.ttf";
    private static final Map<String, ByteBuffer> fontDataByPath = new HashMap<>();
    private static final Map<String, STBTTFontinfo> fontInfoByPath = new HashMap<>();

    private TextFonts() {
    }

    public static ByteBuffer loadCanvasFontData() {
        return loadFontData(CANVAS_FONT_RESOURCE);
    }

    public static ByteBuffer loadFontData(String resourcePath) {
        ByteBuffer fontData = fontDataByPath.get(resourcePath);
        if (fontData != null)
            return fontData.duplicate();

        try (InputStream is = FileUtils.getResource(resourcePath)) {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            fontDataByPath.put(resourcePath, buffer);
            return buffer.duplicate();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load font " + resourcePath, e);
        }
    }

    public static float measureCanvasWidth(float pixelHeight, String str) {
        return measureWidth(CANVAS_FONT_RESOURCE, pixelHeight, str);
    }

    public static float measureWidth(String resourcePath, float pixelHeight, String str) {
        STBTTFontinfo fontInfo = fontInfo(resourcePath);
        float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, pixelHeight);
        float width = 0;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer ignoredLeftBearing = stack.mallocInt(1);
            int len = str.length();
            for (int i = 0; i < len; i++) {
                int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, str.charAt(i));
                if (glyphIndex == 0)
                    continue;
                STBTruetype.stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advanceWidth, ignoredLeftBearing);
                width += advanceWidth.get(0) * scale;
            }
        }

        return width;
    }

    public static void dispose() {
        for (STBTTFontinfo fontInfo : fontInfoByPath.values())
            fontInfo.free();
        fontInfoByPath.clear();

        for (ByteBuffer fontData : fontDataByPath.values())
            MemoryUtil.memFree(fontData);
        fontDataByPath.clear();
    }

    private static STBTTFontinfo fontInfo(String resourcePath) {
        STBTTFontinfo fontInfo = fontInfoByPath.get(resourcePath);
        if (fontInfo != null)
            return fontInfo;

        fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, loadFontData(resourcePath))) {
            fontInfo.free();
            throw new IllegalStateException("Failed to initialize font " + resourcePath);
        }

        fontInfoByPath.put(resourcePath, fontInfo);
        return fontInfo;
    }
}
