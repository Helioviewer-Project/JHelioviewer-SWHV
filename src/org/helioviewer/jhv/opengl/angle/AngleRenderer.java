package org.helioviewer.jhv.opengl.angle;

import java.nio.IntBuffer;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.opengl.JHVCanvas;

import org.lwjgl.PointerBuffer;
import org.lwjgl.egl.EGL;
import org.lwjgl.egl.EGL10;
import org.lwjgl.egl.EGL12;
import org.lwjgl.egl.EGL13;
import org.lwjgl.opengles.GLES;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.JNI.callPPP;
import static org.lwjgl.system.MemoryUtil.memAddressSafe;

public final class AngleRenderer {
    private static boolean lwjglConfigured;
    private static boolean rendererInitialized;

    private static final int[] DEPTH_PREFERENCES = {32, 24};
    private static final int EGL_OPENGL_ES3_BIT = 0x00000040;
    private static final int EGL_PLATFORM_ANGLE_ANGLE = 0x3202;
    private static final int EGL_PLATFORM_ANGLE_TYPE_ANGLE = 0x3203;
    private static final int EGL_PLATFORM_ANGLE_TYPE_METAL_ANGLE = 0x3489;
    private final long display;
    private final long context;
    private final long surface;

    public AngleRenderer(long layerPointer) {
        ensureLwjglAngleConfigured();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer displayAttrs = stack.pointers(EGL_PLATFORM_ANGLE_TYPE_ANGLE, EGL_PLATFORM_ANGLE_TYPE_METAL_ANGLE, EGL10.EGL_NONE);
            // eglGetPlatformDisplay(...) should accept a null native display here: ANGLE selects the
            // backend from the attribute list, and for EGL_PLATFORM_ANGLE_ANGLE a 0 native display is valid.
            // LWJGL's checked wrappers reject native_display == 0 here, so call the function pointer directly.
            // display = org.lwjgl.egl.EGL15.eglGetPlatformDisplay(EGL_PLATFORM_ANGLE_ANGLE, 0L, displayAttrs);
            display = callPPP(EGL_PLATFORM_ANGLE_ANGLE, 0L, memAddressSafe(displayAttrs), EGL.getCapabilities().eglGetPlatformDisplay);
            if (display == EGL10.EGL_NO_DISPLAY)
                throw eglError("eglGetPlatformDisplay");

            IntBuffer major = stack.mallocInt(1);
            IntBuffer minor = stack.mallocInt(1);
            if (!EGL10.eglInitialize(display, major, minor))
                throw eglError("eglInitialize");
            EGL.createDisplayCapabilities(display, major.get(0), minor.get(0));
            if (!EGL12.eglBindAPI(EGL12.EGL_OPENGL_ES_API))
                throw eglError("eglBindAPI");

            int samples = Math.max(0, JHVCanvas.GLSAMPLES);
            long config = chooseConfig(stack, samples);
            if (config == 0L)
                throw eglError("eglChooseConfig");
            logChosenConfig(stack, config);

            IntBuffer contextAttrs = stack.ints(EGL13.EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE);
            context = EGL10.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttrs);
            if (context == EGL10.EGL_NO_CONTEXT)
                throw eglError("eglCreateContext");

            surface = EGL10.eglCreateWindowSurface(display, config, layerPointer, stack.ints(EGL10.EGL_NONE));
            if (surface == EGL10.EGL_NO_SURFACE)
                throw eglError("eglCreateWindowSurface");
        }

        if (!EGL10.eglMakeCurrent(display, surface, surface, context))
            throw eglError("eglMakeCurrent");
        GLES.createCapabilities();
        GL.useGles(true);
        JHVCanvas.glVersion = GL.formatVersionString(GL.glGetString(GL.VERSION));
        JHVCanvas.maxTextureSize = GL.glGetInteger(GL.MAX_TEXTURE_SIZE);
        initSharedJhvRenderer();
        render(false);
    }

    public void render(boolean whiteBackground) {
        if (!EGL10.eglMakeCurrent(display, surface, surface, context))
            throw eglError("eglMakeCurrent");
        GLRenderer.display(whiteBackground);
        if (!EGL10.eglSwapBuffers(display, surface))
            throw eglError("eglSwapBuffers");
    }

    public void destroy() {
        disposeSharedJhvRenderer();
        EGL10.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        GLES.destroy();
        EGL10.eglDestroySurface(display, surface);
        EGL10.eglDestroyContext(display, context);
        EGL10.eglTerminate(display);
    }

    private static synchronized void initSharedJhvRenderer() {
        if (rendererInitialized)
            return;
        GLRenderer.init();
        rendererInitialized = true;
    }

    private static synchronized void disposeSharedJhvRenderer() {
        if (!rendererInitialized)
            return;
        GLRenderer.dispose();
        rendererInitialized = false;
    }

    private static synchronized void ensureLwjglAngleConfigured() {
        if (lwjglConfigured)
            return;
        AngleLibraries.configureLwjglProperty("org.lwjgl.egl.libname", "libEGL.dylib");
        AngleLibraries.configureLwjglProperty("org.lwjgl.opengles.libname", "libGLESv2.dylib");
        EGL.getCapabilities();
        lwjglConfigured = true;
    }

    private long chooseConfig(MemoryStack stack, int samples) {
        for (int depthBits : DEPTH_PREFERENCES) {
            if (samples > 0) {
                long config = chooseConfig(stack, depthBits, samples);
                if (config != 0L)
                    return config;
            }
        }
        for (int depthBits : DEPTH_PREFERENCES) {
            long config = chooseConfig(stack, depthBits, 0);
            if (config != 0L)
                return config;
        }
        return 0L;
    }

    private long chooseConfig(MemoryStack stack, int depthBits, int samples) {
        PointerBuffer configOut = stack.mallocPointer(1);
        IntBuffer numConfigs = stack.mallocInt(1);
        IntBuffer configAttrs = stack.mallocInt(samples > 0 ? 19 : 15);
        configAttrs.put(EGL10.EGL_SURFACE_TYPE).put(EGL10.EGL_WINDOW_BIT);
        configAttrs.put(EGL12.EGL_RENDERABLE_TYPE).put(EGL_OPENGL_ES3_BIT);
        configAttrs.put(EGL10.EGL_RED_SIZE).put(8);
        configAttrs.put(EGL10.EGL_GREEN_SIZE).put(8);
        configAttrs.put(EGL10.EGL_BLUE_SIZE).put(8);
        configAttrs.put(EGL10.EGL_ALPHA_SIZE).put(8);
        configAttrs.put(EGL10.EGL_DEPTH_SIZE).put(depthBits);
        if (samples > 0) {
            configAttrs.put(EGL10.EGL_SAMPLE_BUFFERS).put(1);
            configAttrs.put(EGL10.EGL_SAMPLES).put(samples);
        }
        configAttrs.put(EGL10.EGL_NONE);
        configAttrs.flip();

        if (!EGL10.eglChooseConfig(display, configAttrs, configOut, numConfigs) || numConfigs.get(0) <= 0)
            return 0L;
        return configOut.get(0);
    }

    private void logChosenConfig(MemoryStack stack, long config) {
        IntBuffer value = stack.mallocInt(1);
        int red = configAttrib(value, config, EGL10.EGL_RED_SIZE);
        int green = configAttrib(value, config, EGL10.EGL_GREEN_SIZE);
        int blue = configAttrib(value, config, EGL10.EGL_BLUE_SIZE);
        int alpha = configAttrib(value, config, EGL10.EGL_ALPHA_SIZE);
        int depth = configAttrib(value, config, EGL10.EGL_DEPTH_SIZE);
        int stencil = configAttrib(value, config, EGL10.EGL_STENCIL_SIZE);
        int sampleBuffers = configAttrib(value, config, EGL10.EGL_SAMPLE_BUFFERS);
        int samples = configAttrib(value, config, EGL10.EGL_SAMPLES);

        Log.info("ANGLE EGL config: rgba=" + red + "/" + green + "/" + blue + "/" + alpha
                + " depth=" + depth
                + " stencil=" + stencil
                + " sampleBuffers=" + sampleBuffers
                + " samples=" + samples);
    }

    private int configAttrib(IntBuffer value, long config, int attribute) {
        if (!EGL10.eglGetConfigAttrib(display, config, attribute, value))
            throw eglError("eglGetConfigAttrib");
        return value.get(0);
    }

    private static RuntimeException eglError(String step) {
        int code = EGL10.eglGetError();
        return new RuntimeException(step + " failed with EGL error 0x" + Integer.toHexString(code));
    }

}
