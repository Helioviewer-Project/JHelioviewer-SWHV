package org.helioviewer.jhv.opengl.angle;

import java.nio.IntBuffer;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Platform;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLRenderer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.egl.EGL;
import org.lwjgl.egl.EGL15;
import org.lwjgl.opengles.GLES;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class AngleRenderer {
    private record PlatformConfig(int backendType, String eglLibrary, String openGlesLibrary) {}

    private static boolean lwjglConfigured;
    private static boolean rendererInitialized;

    private static final int[] DEPTH_PREFERENCES = {32, 24};
    private static final int EGL_OPENGL_ES3_BIT = 0x00000040;
    private static final int EGL_PLATFORM_ANGLE_ANGLE = 0x3202;
    private static final int EGL_PLATFORM_ANGLE_TYPE_ANGLE = 0x3203;
    private static final int EGL_PLATFORM_ANGLE_TYPE_D3D11_ANGLE = 0x3208;
    private static final int EGL_PLATFORM_ANGLE_TYPE_METAL_ANGLE = 0x3489;
    private static final int EGL_PLATFORM_ANGLE_TYPE_OPENGL_ANGLE = 0x320D;
    private final long display;
    private final long context;
    private final long surface;

    // Front-load LWJGL/ANGLE library setup and EGL capability discovery before the first real renderer is created.
    public static void prewarm() {
        ensureLwjglAngleConfigured(platformConfig());
    }

    public AngleRenderer(long nativeWindowHandle) {
        PlatformConfig platform = platformConfig();
        ensureLwjglAngleConfigured(platform);

        long newDisplay = EGL15.EGL_NO_DISPLAY;
        long newContext = EGL15.EGL_NO_CONTEXT;
        long newSurface = EGL15.EGL_NO_SURFACE;
        boolean glesInitialized = false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer displayAttrs = displayAttrs(stack, platform);
            // LWJGL's checked wrappers reject native_display == 0 here, so call the function pointer directly.
            // display = org.lwjgl.egl.EGL15.eglGetPlatformDisplay(EGL_PLATFORM_ANGLE_ANGLE, 0L, displayAttrs);
            newDisplay = JNI.callPPP(EGL_PLATFORM_ANGLE_ANGLE, 0L, MemoryUtil.memAddressSafe(displayAttrs), EGL.getCapabilities().eglGetPlatformDisplay);
            if (newDisplay == EGL15.EGL_NO_DISPLAY)
                throw eglError("eglGetPlatformDisplay");

            IntBuffer major = stack.mallocInt(1);
            IntBuffer minor = stack.mallocInt(1);
            if (!EGL15.eglInitialize(newDisplay, major, minor))
                throw eglError("eglInitialize");
            EGL.createDisplayCapabilities(newDisplay, major.get(0), minor.get(0));
            if (!EGL15.eglBindAPI(EGL15.EGL_OPENGL_ES_API))
                throw eglError("eglBindAPI");

            int samples = Math.max(0, GL.SAMPLES);
            long config = chooseConfig(stack, newDisplay, samples);
            if (config == 0L)
                throw eglError("eglChooseConfig");
            logChosenConfig(stack, newDisplay, config);

            IntBuffer contextAttrs = stack.ints(EGL15.EGL_CONTEXT_CLIENT_VERSION, 3, EGL15.EGL_NONE);
            newContext = EGL15.eglCreateContext(newDisplay, config, EGL15.EGL_NO_CONTEXT, contextAttrs);
            if (newContext == EGL15.EGL_NO_CONTEXT)
                throw eglError("eglCreateContext");

            newSurface = EGL15.eglCreateWindowSurface(newDisplay, config, nativeWindowHandle, stack.ints(EGL15.EGL_NONE));
            if (newSurface == EGL15.EGL_NO_SURFACE)
                throw eglError("eglCreateWindowSurface");

            if (!EGL15.eglMakeCurrent(newDisplay, newSurface, newSurface, newContext))
                throw eglError("eglMakeCurrent");
            GLES.createCapabilities();
            glesInitialized = true;
            GL.initInfo();
            initSharedJhvRenderer();
        } catch (RuntimeException | Error e) {
            if (glesInitialized)
                GLES.destroy();
            if (newDisplay != EGL15.EGL_NO_DISPLAY) {
                EGL15.eglMakeCurrent(newDisplay, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_CONTEXT);
                if (newSurface != EGL15.EGL_NO_SURFACE)
                    EGL15.eglDestroySurface(newDisplay, newSurface);
                if (newContext != EGL15.EGL_NO_CONTEXT)
                    EGL15.eglDestroyContext(newDisplay, newContext);
                EGL15.eglTerminate(newDisplay);
            }
            throw e;
        }

        display = newDisplay;
        context = newContext;
        surface = newSurface;
        try {
            render(false);
        } catch (RuntimeException | Error e) {
            destroy();
            throw e;
        }
    }

    public void render(boolean whiteBackground) {
        if (!EGL15.eglMakeCurrent(display, surface, surface, context))
            throw eglError("eglMakeCurrent");
        GLRenderer.display(whiteBackground);
        if (!EGL15.eglSwapBuffers(display, surface))
            throw eglError("eglSwapBuffers");
    }

    public void destroy() {
        boolean current = EGL15.eglMakeCurrent(display, surface, surface, context);
        try {
            if (current)
                disposeSharedJhvRenderer();
            else
                rendererInitialized = false;
        } finally {
            EGL15.eglMakeCurrent(display, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_CONTEXT);
            if (current)
                GLES.destroy();
            EGL15.eglDestroySurface(display, surface);
            EGL15.eglDestroyContext(display, context);
            EGL15.eglTerminate(display);
        }
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

    private static synchronized void ensureLwjglAngleConfigured(PlatformConfig platform) {
        if (lwjglConfigured)
            return;
        Configuration.EGL_LIBRARY_NAME.set(AngleLibraries.libraryPath(platform.eglLibrary()).toString());
        Configuration.OPENGLES_LIBRARY_NAME.set(AngleLibraries.libraryPath(platform.openGlesLibrary()).toString());
        EGL.getCapabilities();
        lwjglConfigured = true;
    }

    private static PointerBuffer displayAttrs(MemoryStack stack, PlatformConfig platform) {
        return stack.pointers(EGL_PLATFORM_ANGLE_TYPE_ANGLE, platform.backendType(), EGL15.EGL_NONE);
    }

    private long chooseConfig(MemoryStack stack, long display, int samples) {
        for (int depthBits : DEPTH_PREFERENCES) {
            if (samples > 0) {
                long config = chooseConfig(stack, display, depthBits, samples);
                if (config != 0L)
                    return config;
            }
        }
        for (int depthBits : DEPTH_PREFERENCES) {
            long config = chooseConfig(stack, display, depthBits, 0);
            if (config != 0L)
                return config;
        }
        return 0L;
    }

    private long chooseConfig(MemoryStack stack, long display, int depthBits, int samples) {
        PointerBuffer configOut = stack.mallocPointer(1);
        IntBuffer numConfigs = stack.mallocInt(1);
        int attributeCount = samples > 0 ? 19 : 15;
        IntBuffer configAttrs = stack.mallocInt(attributeCount);
        configAttrs.put(EGL15.EGL_SURFACE_TYPE).put(EGL15.EGL_WINDOW_BIT);
        configAttrs.put(EGL15.EGL_RENDERABLE_TYPE).put(EGL_OPENGL_ES3_BIT);
        configAttrs.put(EGL15.EGL_RED_SIZE).put(8);
        configAttrs.put(EGL15.EGL_GREEN_SIZE).put(8);
        configAttrs.put(EGL15.EGL_BLUE_SIZE).put(8);
        configAttrs.put(EGL15.EGL_ALPHA_SIZE).put(8);
        configAttrs.put(EGL15.EGL_DEPTH_SIZE).put(depthBits);
        if (samples > 0) {
            configAttrs.put(EGL15.EGL_SAMPLE_BUFFERS).put(1);
            configAttrs.put(EGL15.EGL_SAMPLES).put(samples);
        }
        configAttrs.put(EGL15.EGL_NONE);
        configAttrs.flip();

        if (!EGL15.eglChooseConfig(display, configAttrs, configOut, numConfigs) || numConfigs.get(0) <= 0)
            return 0L;
        return configOut.get(0);
    }

    private void logChosenConfig(MemoryStack stack, long display, long config) {
        IntBuffer attribValue = stack.mallocInt(1);
        int red = configAttrib(attribValue, display, config, EGL15.EGL_RED_SIZE);
        int green = configAttrib(attribValue, display, config, EGL15.EGL_GREEN_SIZE);
        int blue = configAttrib(attribValue, display, config, EGL15.EGL_BLUE_SIZE);
        int alpha = configAttrib(attribValue, display, config, EGL15.EGL_ALPHA_SIZE);
        int depth = configAttrib(attribValue, display, config, EGL15.EGL_DEPTH_SIZE);
        int stencil = configAttrib(attribValue, display, config, EGL15.EGL_STENCIL_SIZE);
        int sampleBuffers = configAttrib(attribValue, display, config, EGL15.EGL_SAMPLE_BUFFERS);
        int samples = configAttrib(attribValue, display, config, EGL15.EGL_SAMPLES);

        Log.info("ANGLE EGL config: rgba=" + red + "/" + green + "/" + blue + "/" + alpha
                + " depth=" + depth
                + " stencil=" + stencil
                + " sampleBuffers=" + sampleBuffers
                + " samples=" + samples);
    }

    private int configAttrib(IntBuffer value, long display, long config, int attribute) {
        if (!EGL15.eglGetConfigAttrib(display, config, attribute, value))
            throw eglError("eglGetConfigAttrib");
        return value.get(0);
    }

    private static RuntimeException eglError(String step) {
        int code = EGL15.eglGetError();
        return new RuntimeException(step + " failed with EGL error 0x" + Integer.toHexString(code));
    }

    private static PlatformConfig platformConfig() {
        if (Platform.isMacOS())
            return new PlatformConfig(EGL_PLATFORM_ANGLE_TYPE_METAL_ANGLE, "libEGL.dylib", "libGLESv2.dylib");
        if (Platform.isWindows())
            return new PlatformConfig(EGL_PLATFORM_ANGLE_TYPE_D3D11_ANGLE, "libEGL.dll", "libGLESv2.dll");
        if (Platform.isLinux())
            return new PlatformConfig(EGL_PLATFORM_ANGLE_TYPE_OPENGL_ANGLE, "libEGL.so", "libGLESv2.so");
        throw new IllegalStateException("Unsupported ANGLE platform");
    }

}
