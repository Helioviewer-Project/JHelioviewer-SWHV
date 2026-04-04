package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL33;

final class GLFrameCapture {

    private final int width;
    private final int height;
    private final int samples;

    private final int resolveFramebuffer;
    private final int resolveTexture;
    private final int drawFramebuffer;
    private final int drawColorRenderbuffer;
    private final int drawDepthRenderbuffer;

    GLFrameCapture(int captureW, int captureH, int requestedSamples) {
        int frameWidth = Math.max(1, captureW);
        int frameHeight = Math.max(1, captureH);
        int frameSamples = Math.clamp(requestedSamples, 0, GL33.glGetInteger(GL33.GL_MAX_SAMPLES));
        int colorInternalFormat = chooseColorInternalFormat();
        int colorPixelFormat = chooseColorPixelFormat();
        int resolveFbo = 0;
        int resolveTex = 0;
        int drawFbo = 0;
        int drawColorRbo = 0;
        int drawDepthRbo = 0;
        int[] ids = new int[1];

        try {
            GL33.glGenFramebuffers(ids);
            resolveFbo = ids[0];
            bindFramebuffer(GL33.GL_FRAMEBUFFER, resolveFbo);

            GL33.glGenTextures(ids);
            resolveTex = ids[0];
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, resolveTex);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_LINEAR);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_LINEAR);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_EDGE);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_CLAMP_TO_EDGE);
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, colorInternalFormat, frameWidth, frameHeight, 0, colorPixelFormat, GL33.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT0, GL33.GL_TEXTURE_2D, resolveTex, 0);

            if (frameSamples > 0) {
                GL33.glGenFramebuffers(ids);
                drawFbo = ids[0];
                bindFramebuffer(GL33.GL_FRAMEBUFFER, drawFbo);

                GL33.glGenRenderbuffers(ids);
                drawColorRbo = ids[0];
                GL33.glBindRenderbuffer(GL33.GL_RENDERBUFFER, drawColorRbo);
                GL33.glRenderbufferStorageMultisample(GL33.GL_RENDERBUFFER, frameSamples, colorInternalFormat, frameWidth, frameHeight);
                GL33.glFramebufferRenderbuffer(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT0, GL33.GL_RENDERBUFFER, drawColorRbo);

                GL33.glGenRenderbuffers(ids);
                drawDepthRbo = ids[0];
                GL33.glBindRenderbuffer(GL33.GL_RENDERBUFFER, drawDepthRbo);
                GL33.glRenderbufferStorageMultisample(GL33.GL_RENDERBUFFER, frameSamples, chooseDepthFormat(), frameWidth, frameHeight);
                GL33.glFramebufferRenderbuffer(GL33.GL_FRAMEBUFFER, GL33.GL_DEPTH_ATTACHMENT, GL33.GL_RENDERBUFFER, drawDepthRbo);

                checkFramebufferComplete("draw");

                bindFramebuffer(GL33.GL_FRAMEBUFFER, resolveFbo);
                checkFramebufferComplete("resolve");
            } else {
                drawFbo = resolveFbo;

                GL33.glGenRenderbuffers(ids);
                drawDepthRbo = ids[0];
                GL33.glBindRenderbuffer(GL33.GL_RENDERBUFFER, drawDepthRbo);
                GL33.glRenderbufferStorage(GL33.GL_RENDERBUFFER, chooseDepthFormat(), frameWidth, frameHeight);
                GL33.glFramebufferRenderbuffer(GL33.GL_FRAMEBUFFER, GL33.GL_DEPTH_ATTACHMENT, GL33.GL_RENDERBUFFER, drawDepthRbo);

                checkFramebufferComplete("draw");
            }
        } catch (RuntimeException e) {
            deleteRenderbuffer(drawDepthRbo);
            deleteRenderbuffer(drawColorRbo);
            if (drawFbo != resolveFbo)
                deleteFramebuffer(drawFbo);
            deleteTexture(resolveTex);
            deleteFramebuffer(resolveFbo);
            throw e;
        } finally {
            GL33.glBindRenderbuffer(GL33.GL_RENDERBUFFER, 0);
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, 0);
            bindFramebuffer(GL33.GL_FRAMEBUFFER, 0);
        }

        resolveFramebuffer = resolveFbo;
        resolveTexture = resolveTex;
        drawFramebuffer = drawFbo;
        drawColorRenderbuffer = drawColorRbo;
        drawDepthRenderbuffer = drawDepthRbo;
        width = frameWidth;
        height = frameHeight;
        samples = frameSamples;
    }

    void bindForRender() {
        bindFramebuffer(GL33.GL_FRAMEBUFFER, drawFramebuffer);
    }

    void readPixels(ByteBuffer buffer) {
        if (samples > 0) {
            bindFramebuffer(GL33.GL_READ_FRAMEBUFFER, drawFramebuffer);
            bindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER, resolveFramebuffer);
            GL33.glBlitFramebuffer(0, 0, width, height,
                    0, 0, width, height,
                    GL33.GL_COLOR_BUFFER_BIT, GL33.GL_NEAREST);
        }

        bindFramebuffer(GL33.GL_READ_FRAMEBUFFER, resolveFramebuffer);
        GL33.glPixelStorei(GL33.GL_PACK_ALIGNMENT, 1);
        GL33.glReadPixels(0, 0, width, height, GL33.GL_BGR, GL33.GL_UNSIGNED_BYTE, buffer);
        bindFramebuffer(GL33.GL_FRAMEBUFFER, 0);
    }

    void dispose() {
        deleteRenderbuffer(drawDepthRenderbuffer);
        deleteRenderbuffer(drawColorRenderbuffer);
        if (drawFramebuffer != resolveFramebuffer)
            deleteFramebuffer(drawFramebuffer);
        deleteTexture(resolveTexture);
        deleteFramebuffer(resolveFramebuffer);
    }

    private static void bindFramebuffer(int target, int framebuffer) {
        GL33.glBindFramebuffer(target, framebuffer);
    }

    private static void checkFramebufferComplete(String label) {
        int status = GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER);
        if (status != GL33.GL_FRAMEBUFFER_COMPLETE)
            throw new JHVGLException("GLFrameCapture " + label + " framebuffer incomplete: 0x" + Integer.toHexString(status));
    }

    private static void deleteFramebuffer(int framebuffer) {
        if (framebuffer == 0)
            return;
        GL33.glDeleteFramebuffers(framebuffer);
    }

    private static void deleteRenderbuffer(int renderbuffer) {
        if (renderbuffer == 0)
            return;
        GL33.glDeleteRenderbuffers(renderbuffer);
    }

    private static void deleteTexture(int texture) {
        if (texture == 0)
            return;
        GL33.glDeleteTextures(texture);
    }

    private static int chooseDepthFormat() {
        int depthBits = getInteger(GL33.GL_DEPTH_BITS);
        if (depthBits >= 32)
            return GL33.GL_DEPTH_COMPONENT32;
        if (depthBits >= 24)
            return GL33.GL_DEPTH_COMPONENT24;
        return GL33.GL_DEPTH_COMPONENT16;
    }

    private static int chooseColorInternalFormat() {
        return getInteger(GL33.GL_ALPHA_BITS) > 0 ? GL33.GL_RGBA8 : GL33.GL_RGB8;
    }

    private static int chooseColorPixelFormat() {
        return getInteger(GL33.GL_ALPHA_BITS) > 0 ? GL33.GL_RGBA : GL33.GL_RGB;
    }

    private static int getInteger(int name) {
        return GL33.glGetInteger(name);
    }
}
