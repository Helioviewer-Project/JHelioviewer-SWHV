package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;

import org.helioviewer.jhv.Log;

import org.lwjgl.system.MemoryUtil;

final class GLFrameCapture {
    private static final int[] DEPTH_FORMATS = {GL.DEPTH_COMPONENT32, GL.DEPTH_COMPONENT24, GL.DEPTH_COMPONENT16};

    private final int width;
    private final int height;
    private final int samples;

    private final int resolveFramebuffer;
    private final int resolveTexture;
    private final int drawFramebuffer;
    private final int drawColorRenderbuffer;
    private final int drawDepthRenderbuffer;
    private final ByteBuffer rgbaReadback;

    GLFrameCapture(int captureW, int captureH) {
        int frameWidth = Math.max(1, captureW);
        int frameHeight = Math.max(1, captureH);
        int frameSamples = GL.SAMPLES > 1 ? Math.clamp(GL.SAMPLES, 0, GL.glGetInteger(GL.MAX_SAMPLES)) : 0;
        int colorInternalFormat = GL.RGB8;
        int colorPixelFormat = GL.RGB;
        int resolveFbo = 0;
        int resolveTex = 0;
        int drawFbo = 0;
        int drawColorRbo = 0;
        int drawDepthRbo = 0;
        int chosenDepthFormat;
        ByteBuffer readback = MemoryUtil.memAlloc(frameWidth * frameHeight * 4);

        try {
            resolveFbo = GL.glGenFramebuffer();
            GL.glBindFramebuffer(GL.FRAMEBUFFER, resolveFbo);

            resolveTex = GL.glGenTexture();
            GL.glBindTexture(GL.TEXTURE_2D, resolveTex);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE);
            GL.glTexImage2D(GL.TEXTURE_2D, 0, colorInternalFormat, frameWidth, frameHeight, 0, colorPixelFormat, GL.UNSIGNED_BYTE, (ByteBuffer) null);
            GL.glFramebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0, GL.TEXTURE_2D, resolveTex, 0);

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
            if (resolveTex != 0)
                GL.glDeleteTexture(resolveTex);
            if (resolveFbo != 0)
                GL.glDeleteFramebuffer(resolveFbo);
            if (readback != null)
                MemoryUtil.memFree(readback);
            throw e;
        } finally {
            GL.glBindRenderbuffer(GL.RENDERBUFFER, 0);
            GL.glBindTexture(GL.TEXTURE_2D, 0);
            GL.glBindFramebuffer(GL.FRAMEBUFFER, 0);
        }

        resolveFramebuffer = resolveFbo;
        resolveTexture = resolveTex;
        drawFramebuffer = drawFbo;
        drawColorRenderbuffer = drawColorRbo;
        drawDepthRenderbuffer = drawDepthRbo;
        width = frameWidth;
        height = frameHeight;
        samples = frameSamples;
        rgbaReadback = readback;
        int depthFormat = chosenDepthFormat;
        Log.info("GLFrameCapture config: size=" + width + "x" + height
                + " samples=" + samples
                + " depth=" + depthBits(depthFormat));
    }

    void bindForRender() {
        GL.glBindFramebuffer(GL.FRAMEBUFFER, drawFramebuffer);
    }

    void readPixels(ByteBuffer buffer) {
        if (samples > 0) {
            GL.glBindFramebuffer(GL.READ_FRAMEBUFFER, drawFramebuffer);
            GL.glBindFramebuffer(GL.DRAW_FRAMEBUFFER, resolveFramebuffer);
            GL.glBlitFramebuffer(0, 0, width, height,
                    0, 0, width, height,
                    GL.COLOR_BUFFER_BIT, GL.NEAREST);
        }

        GL.glBindFramebuffer(GL.READ_FRAMEBUFFER, resolveFramebuffer);
        GL.glPixelStorei(GL.PACK_ALIGNMENT, 1);
        rgbaReadback.clear();
        GL.glReadPixels(0, 0, width, height, GL.RGBA, GL.UNSIGNED_BYTE, rgbaReadback);
        rgbaReadback.limit(width * height * 4);
        buffer.clear();
        while (rgbaReadback.remaining() >= 4) {
            byte r = rgbaReadback.get();
            byte g = rgbaReadback.get();
            byte b = rgbaReadback.get();
            rgbaReadback.get();
            buffer.put(r).put(g).put(b);
        }
        buffer.flip();
        GL.glBindFramebuffer(GL.FRAMEBUFFER, 0);
    }

    void dispose() {
        if (drawDepthRenderbuffer != 0)
            GL.glDeleteRenderbuffer(drawDepthRenderbuffer);
        if (drawColorRenderbuffer != 0)
            GL.glDeleteRenderbuffer(drawColorRenderbuffer);
        if (drawFramebuffer != resolveFramebuffer)
            GL.glDeleteFramebuffer(drawFramebuffer);
        if (resolveTexture != 0)
            GL.glDeleteTexture(resolveTexture);
        if (resolveFramebuffer != 0)
            GL.glDeleteFramebuffer(resolveFramebuffer);
        if (rgbaReadback != null)
            MemoryUtil.memFree(rgbaReadback);
    }

    private static void checkFramebufferComplete(String label) {
        int status = GL.glCheckFramebufferStatus(GL.FRAMEBUFFER);
        if (status != GL.FRAMEBUFFER_COMPLETE)
            throw new JHVGLException("GLFrameCapture " + label + " framebuffer incomplete: 0x" + Integer.toHexString(status));
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

    private static int depthBits(int depthFormat) {
        return switch (depthFormat) {
            case GL.DEPTH_COMPONENT32 -> 32;
            case GL.DEPTH_COMPONENT24 -> 24;
            default -> 16;
        };
    }

}
