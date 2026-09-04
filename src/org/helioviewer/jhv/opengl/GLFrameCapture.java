package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;

import org.helioviewer.jhv.app.Log;

import org.lwjgl.system.MemoryUtil;

final class GLFrameCapture {
    private static final int[] DEPTH_FORMATS = {GL.DEPTH_COMPONENT32F, GL.DEPTH_COMPONENT24, GL.DEPTH_COMPONENT16};
    private static final int EXPORT_SAMPLES = 4;

    private final int width;
    private final int height;
    private final int samples;

    private final int resolveFramebuffer;
    private final int resolveColorRenderbuffer;
    private final int drawFramebuffer;
    private final int drawColorRenderbuffer;
    private final int drawDepthRenderbuffer;
    private final ByteBuffer rgbaReadback;
    private final byte[] rgbaRow;
    private final byte[] rgbRow;

    GLFrameCapture(int captureW, int captureH) {
        int frameWidth = Math.max(1, captureW);
        int frameHeight = Math.max(1, captureH);
        int frameSamples = Math.clamp(EXPORT_SAMPLES, 0, GL.glGetInteger(GL.MAX_SAMPLES));
        int colorInternalFormat = GL.RGB8;
        int resolveFbo = 0;
        int resolveColorRbo = 0;
        int drawFbo = 0;
        int drawColorRbo = 0;
        int drawDepthRbo = 0;
        int chosenDepthFormat;
        ByteBuffer readback = MemoryUtil.memAlloc(frameWidth * frameHeight * 4);
        byte[] readbackRow = new byte[frameWidth * 4];
        byte[] outputRow = new byte[frameWidth * 3];

        try {
            resolveFbo = GL.glGenFramebuffer();
            GL.glBindFramebuffer(GL.FRAMEBUFFER, resolveFbo);

            resolveColorRbo = GL.glGenRenderbuffer();
            GL.glBindRenderbuffer(GL.RENDERBUFFER, resolveColorRbo);
            GL.glRenderbufferStorage(GL.RENDERBUFFER, colorInternalFormat, frameWidth, frameHeight);
            GL.glFramebufferRenderbuffer(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0, GL.RENDERBUFFER, resolveColorRbo);

            if (frameSamples > 0) {
                drawFbo = GL.glGenFramebuffer();
                GL.glBindFramebuffer(GL.FRAMEBUFFER, drawFbo);

                drawColorRbo = GL.glGenRenderbuffer();
                GL.glBindRenderbuffer(GL.RENDERBUFFER, drawColorRbo);
                GL.glRenderbufferStorageMultisample(GL.RENDERBUFFER, frameSamples, colorInternalFormat, frameWidth, frameHeight);
                GL.glFramebufferRenderbuffer(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0, GL.RENDERBUFFER, drawColorRbo);

                drawDepthRbo = GL.glGenRenderbuffer();
                chosenDepthFormat = attachDepthRenderbuffer(frameWidth, frameHeight, frameSamples, drawDepthRbo);

                GL.glBindFramebuffer(GL.FRAMEBUFFER, resolveFbo);
                checkFramebufferComplete("resolve");
            } else {
                drawFbo = resolveFbo;

                drawDepthRbo = GL.glGenRenderbuffer();
                chosenDepthFormat = attachDepthRenderbuffer(frameWidth, frameHeight, 0, drawDepthRbo);
            }
        } catch (RuntimeException e) {
            if (drawDepthRbo != 0)
                GL.glDeleteRenderbuffer(drawDepthRbo);
            if (drawColorRbo != 0)
                GL.glDeleteRenderbuffer(drawColorRbo);
            if (drawFbo != resolveFbo)
                GL.glDeleteFramebuffer(drawFbo);
            if (resolveColorRbo != 0)
                GL.glDeleteRenderbuffer(resolveColorRbo);
            if (resolveFbo != 0)
                GL.glDeleteFramebuffer(resolveFbo);
            if (readback != null)
                MemoryUtil.memFree(readback);
            throw e;
        } finally {
            GL.glBindRenderbuffer(GL.RENDERBUFFER, 0);
            GL.glBindFramebuffer(GL.FRAMEBUFFER, 0);
        }

        resolveFramebuffer = resolveFbo;
        resolveColorRenderbuffer = resolveColorRbo;
        drawFramebuffer = drawFbo;
        drawColorRenderbuffer = drawColorRbo;
        drawDepthRenderbuffer = drawDepthRbo;
        width = frameWidth;
        height = frameHeight;
        samples = frameSamples;
        rgbaReadback = readback;
        rgbaRow = readbackRow;
        rgbRow = outputRow;
        int depthFormat = chosenDepthFormat;
        Log.info("GLFrameCapture config: size=" + width + "x" + height
                + " samples=" + samples
                + " depth=" + depthFormatName(depthFormat));
    }

    void bindForRender() {
        GL.glBindFramebuffer(GL.FRAMEBUFFER, drawFramebuffer);
    }

    void readPixels(ByteBuffer buffer) {
        int outputSize = width * height * 3;
        if (buffer.capacity() < outputSize)
            throw new IllegalArgumentException("Buffer capacity " + buffer.capacity() + " is less than " + outputSize);

        if (samples > 0) {
            GL.glBindFramebuffer(GL.READ_FRAMEBUFFER, drawFramebuffer);
            GL.glBindFramebuffer(GL.DRAW_FRAMEBUFFER, resolveFramebuffer);
            GL.glBlitFramebuffer(0, 0, width, height,
                    0, 0, width, height,
                    GL.COLOR_BUFFER_BIT, GL.NEAREST);
        }

        GL.glBindFramebuffer(GL.READ_FRAMEBUFFER, resolveFramebuffer);
        rgbaReadback.clear();
        GL.glReadPixels(0, 0, width, height, GL.RGBA, GL.UNSIGNED_BYTE, rgbaReadback);
        rgbaReadback.limit(width * height * 4);

        buffer.clear();
        for (int y = 0; y < height; y++) {
            rgbaReadback.get(rgbaRow);

            int src = 0;
            int dst = 0;
            for (int x = 0; x < width; x++) {
                rgbRow[dst++] = rgbaRow[src++];
                rgbRow[dst++] = rgbaRow[src++];
                rgbRow[dst++] = rgbaRow[src++];
                src++;
            }

            buffer.put(rgbRow);
        }
        buffer.flip();
    }

    void dispose() {
        if (drawDepthRenderbuffer != 0)
            GL.glDeleteRenderbuffer(drawDepthRenderbuffer);
        if (drawColorRenderbuffer != 0)
            GL.glDeleteRenderbuffer(drawColorRenderbuffer);
        if (drawFramebuffer != resolveFramebuffer)
            GL.glDeleteFramebuffer(drawFramebuffer);
        if (resolveColorRenderbuffer != 0)
            GL.glDeleteRenderbuffer(resolveColorRenderbuffer);
        if (resolveFramebuffer != 0)
            GL.glDeleteFramebuffer(resolveFramebuffer);
        if (rgbaReadback != null)
            MemoryUtil.memFree(rgbaReadback);
    }

    private static void checkFramebufferComplete(String label) {
        int status = GL.glCheckFramebufferStatus(GL.FRAMEBUFFER);
        if (status != GL.FRAMEBUFFER_COMPLETE)
            throw new GLException("GLFrameCapture " + label + " framebuffer incomplete: 0x" + Integer.toHexString(status));
    }

    private static int attachDepthRenderbuffer(int width, int height, int samples, int renderbuffer) {
        GL.glBindRenderbuffer(GL.RENDERBUFFER, renderbuffer);
        for (int depthFormat : DEPTH_FORMATS) {
            if (samples > 0)
                GL.glRenderbufferStorageMultisample(GL.RENDERBUFFER, samples, depthFormat, width, height);
            else
                GL.glRenderbufferStorage(GL.RENDERBUFFER, depthFormat, width, height);
            GL.glFramebufferRenderbuffer(GL.FRAMEBUFFER, GL.DEPTH_ATTACHMENT, GL.RENDERBUFFER, renderbuffer);
            if (GL.glCheckFramebufferStatus(GL.FRAMEBUFFER) == GL.FRAMEBUFFER_COMPLETE)
                return depthFormat;
        }

        checkFramebufferComplete("draw");
        return 0;
    }

    private static String depthFormatName(int depthFormat) {
        return switch (depthFormat) {
            case GL.DEPTH_COMPONENT32F -> "32F";
            case GL.DEPTH_COMPONENT24 -> "24";
            default -> "16";
        };
    }

}
