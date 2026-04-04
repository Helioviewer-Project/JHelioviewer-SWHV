package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL3;

final class GLFrameCapture {

    private final int width;
    private final int height;
    private final int samples;

    private final int resolveFramebuffer;
    private final int resolveTexture;
    private final int drawFramebuffer;
    private final int drawColorRenderbuffer;
    private final int drawDepthRenderbuffer;

    GLFrameCapture(GL3 gl, int captureW, int captureH, int requestedSamples) {
        int frameWidth = Math.max(1, captureW);
        int frameHeight = Math.max(1, captureH);
        int frameSamples = Math.clamp(requestedSamples, 0, gl.getMaxRenderbufferSamples());
        int colorInternalFormat = chooseColorInternalFormat(gl);
        int colorPixelFormat = chooseColorPixelFormat(gl);
        int resolveFbo = 0;
        int resolveTex = 0;
        int drawFbo = 0;
        int drawColorRbo = 0;
        int drawDepthRbo = 0;
        int[] ids = new int[1];

        try {
            gl.glGenFramebuffers(1, ids, 0);
            resolveFbo = ids[0];
            bindFramebuffer(gl, GL3.GL_FRAMEBUFFER, resolveFbo);

            gl.glGenTextures(1, ids, 0);
            resolveTex = ids[0];
            gl.glBindTexture(GL3.GL_TEXTURE_2D, resolveTex);
            gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
            gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
            gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
            gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, colorInternalFormat, frameWidth, frameHeight, 0, colorPixelFormat, GL3.GL_UNSIGNED_BYTE, null);
            gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_2D, resolveTex, 0);

            if (frameSamples > 0) {
                gl.glGenFramebuffers(1, ids, 0);
                drawFbo = ids[0];
                bindFramebuffer(gl, GL3.GL_FRAMEBUFFER, drawFbo);

                gl.glGenRenderbuffers(1, ids, 0);
                drawColorRbo = ids[0];
                gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, drawColorRbo);
                gl.glRenderbufferStorageMultisample(GL3.GL_RENDERBUFFER, frameSamples, colorInternalFormat, frameWidth, frameHeight);
                gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, GL3.GL_RENDERBUFFER, drawColorRbo);

                gl.glGenRenderbuffers(1, ids, 0);
                drawDepthRbo = ids[0];
                gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, drawDepthRbo);
                gl.glRenderbufferStorageMultisample(GL3.GL_RENDERBUFFER, frameSamples, chooseDepthFormat(gl), frameWidth, frameHeight);
                gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT, GL3.GL_RENDERBUFFER, drawDepthRbo);

                checkFramebufferComplete(gl, "draw");

                bindFramebuffer(gl, GL3.GL_FRAMEBUFFER, resolveFbo);
                checkFramebufferComplete(gl, "resolve");
            } else {
                drawFbo = resolveFbo;

                gl.glGenRenderbuffers(1, ids, 0);
                drawDepthRbo = ids[0];
                gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, drawDepthRbo);
                gl.glRenderbufferStorage(GL3.GL_RENDERBUFFER, chooseDepthFormat(gl), frameWidth, frameHeight);
                gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT, GL3.GL_RENDERBUFFER, drawDepthRbo);

                checkFramebufferComplete(gl, "draw");
            }
        } catch (RuntimeException e) {
            deleteRenderbuffer(gl, drawDepthRbo);
            deleteRenderbuffer(gl, drawColorRbo);
            if (drawFbo != resolveFbo)
                deleteFramebuffer(gl, drawFbo);
            deleteTexture(gl, resolveTex);
            deleteFramebuffer(gl, resolveFbo);
            throw e;
        } finally {
            gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, 0);
            gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
            bindFramebuffer(gl, GL3.GL_FRAMEBUFFER, 0);
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

    void bindForRender(GL3 gl) {
        bindFramebuffer(gl, GL3.GL_FRAMEBUFFER, drawFramebuffer);
    }

    void readPixels(GL3 gl, Buffer buffer) {
        if (samples > 0) {
            bindFramebuffer(gl, GL3.GL_READ_FRAMEBUFFER, drawFramebuffer);
            bindFramebuffer(gl, GL3.GL_DRAW_FRAMEBUFFER, resolveFramebuffer);
            gl.glBlitFramebuffer(0, 0, width, height,
                    0, 0, width, height,
                    GL3.GL_COLOR_BUFFER_BIT, GL3.GL_NEAREST);
        }

        bindFramebuffer(gl, GL3.GL_READ_FRAMEBUFFER, resolveFramebuffer);
        gl.glPixelStorei(GL3.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, width, height, GL3.GL_BGR, GL3.GL_UNSIGNED_BYTE, buffer);
        bindFramebuffer(gl, GL3.GL_FRAMEBUFFER, 0);
    }

    void dispose(GL3 gl) {
        deleteRenderbuffer(gl, drawDepthRenderbuffer);
        deleteRenderbuffer(gl, drawColorRenderbuffer);
        if (drawFramebuffer != resolveFramebuffer)
            deleteFramebuffer(gl, drawFramebuffer);
        deleteTexture(gl, resolveTexture);
        deleteFramebuffer(gl, resolveFramebuffer);
    }

    private static void bindFramebuffer(GL3 gl, int target, int framebuffer) {
        gl.glBindFramebuffer(target, framebuffer);
    }

    private static void checkFramebufferComplete(GL3 gl, String label) {
        int status = gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER);
        if (status != GL3.GL_FRAMEBUFFER_COMPLETE)
            throw new JHVGLException("GLFrameCapture " + label + " framebuffer incomplete: 0x" + Integer.toHexString(status));
    }

    private static void deleteFramebuffer(GL3 gl, int framebuffer) {
        if (framebuffer == 0)
            return;
        gl.glDeleteFramebuffers(1, new int[]{framebuffer}, 0);
    }

    private static void deleteRenderbuffer(GL3 gl, int renderbuffer) {
        if (renderbuffer == 0)
            return;
        gl.glDeleteRenderbuffers(1, new int[]{renderbuffer}, 0);
    }

    private static void deleteTexture(GL3 gl, int texture) {
        if (texture == 0)
            return;
        gl.glDeleteTextures(1, new int[]{texture}, 0);
    }

    private static int chooseDepthFormat(GL3 gl) {
        int depthBits = getInteger(gl, GL3.GL_DEPTH_BITS);
        if (depthBits >= 32)
            return GL3.GL_DEPTH_COMPONENT32;
        if (depthBits >= 24)
            return GL3.GL_DEPTH_COMPONENT24;
        return GL3.GL_DEPTH_COMPONENT16;
    }

    private static int chooseColorInternalFormat(GL3 gl) {
        return getInteger(gl, GL3.GL_ALPHA_BITS) > 0 ? GL3.GL_RGBA8 : GL3.GL_RGB8;
    }

    private static int chooseColorPixelFormat(GL3 gl) {
        return getInteger(gl, GL3.GL_ALPHA_BITS) > 0 ? GL3.GL_RGBA : GL3.GL_RGB;
    }

    private static int getInteger(GL3 gl, int name) {
        int[] value = {0};
        gl.glGetIntegerv(name, value, 0);
        return value[0];
    }
}
