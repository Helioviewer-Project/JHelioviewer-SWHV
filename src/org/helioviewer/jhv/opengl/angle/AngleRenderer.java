package org.helioviewer.jhv.opengl.angle;

import java.nio.IntBuffer;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLRenderer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.egl.EGL;
import org.lwjgl.egl.EGL15;
import org.lwjgl.opengles.GLES;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class AngleRenderer {
    @SuppressWarnings("serial")
    public static final class ContextLostException extends RuntimeException {
        private ContextLostException(String message) {
            super(message);
        }
    }

    private enum Backend {
        D3D11(EGL_PLATFORM_ANGLE_TYPE_D3D11_ANGLE, "D3D11"),
        METAL(EGL_PLATFORM_ANGLE_TYPE_METAL_ANGLE, "Metal"),
        OPENGL(EGL_PLATFORM_ANGLE_TYPE_OPENGL_ANGLE, "OpenGL"),
        VULKAN(EGL_PLATFORM_ANGLE_TYPE_VULKAN_ANGLE, "Vulkan");

        private final int eglType;
        private final String label;

        Backend(int _eglType, String _label) {
            eglType = _eglType;
            label = _label;
        }

        private static Backend platform() {
            if (Platform.isMacOS())
                return METAL;
            if (Platform.isWindows())
                return D3D11;
            if (Platform.isLinux())
                return OPENGL;
            throw new IllegalStateException("Unsupported ANGLE platform");
        }
    }

    private enum SurfaceKind {
        WINDOW(EGL15.EGL_WINDOW_BIT, true),
        PBUFFER(EGL15.EGL_PBUFFER_BIT, false);

        private final int eglBit;
        private final boolean swapBuffers;

        SurfaceKind(int _eglBit, boolean _swapBuffers) {
            eglBit = _eglBit;
            swapBuffers = _swapBuffers;
        }
    }

    private static Backend selectBackend(SurfaceKind surfaceKind) {
        // Pbuffer Vulkan is currently used only for external SwiftShader; if an ICD is configured, assume it is SwiftShader.
        if (surfaceKind == SurfaceKind.PBUFFER && AngleLibraries.loadSwiftShader())
            return Backend.VULKAN;
        return Backend.platform();
    }

    private static boolean lwjglConfigured;
    private static AngleRenderer activeRenderer;

    private static final int[] DEPTH_PREFERENCES = {32, 24};
    private static final int EGL_OPENGL_ES3_BIT = 0x00000040;
    private static final int EGL_PLATFORM_ANGLE_ANGLE = 0x3202;
    private static final int EGL_PLATFORM_ANGLE_TYPE_ANGLE = 0x3203;
    private static final int EGL_PLATFORM_ANGLE_TYPE_D3D11_ANGLE = 0x3208;
    private static final int EGL_PLATFORM_ANGLE_TYPE_METAL_ANGLE = 0x3489;
    private static final int EGL_PLATFORM_ANGLE_TYPE_OPENGL_ANGLE = 0x320D;
    private static final int EGL_PLATFORM_ANGLE_TYPE_VULKAN_ANGLE = 0x3450;
    private final long display;
    private final long context;
    private final long surface;
    private final boolean swapBuffers;
    private final Backend backend;

    // Front-load LWJGL/ANGLE library setup and EGL capability discovery before the first real renderer is created.
    public static void prewarm() {
        ensureLwjglAngleConfigured();
    }

    public AngleRenderer(long nativeWindowHandle) {
        this(SurfaceKind.WINDOW, nativeWindowHandle, 0, 0);
    }

    public static AngleRenderer pbuffer(int width, int height) {
        return new AngleRenderer(SurfaceKind.PBUFFER, 0L, width, height);
    }

    private AngleRenderer(SurfaceKind surfaceKind, long nativeWindowHandle, int pbufferWidth, int pbufferHeight) {
        backend = selectBackend(surfaceKind);
        swapBuffers = surfaceKind.swapBuffers;
        ensureLwjglAngleConfigured();
        if (activeRenderer != null)
            throw new IllegalStateException("Only one AngleRenderer may be active");
        activeRenderer = this;

        long newDisplay = EGL15.EGL_NO_DISPLAY;
        long newContext = EGL15.EGL_NO_CONTEXT;
        long newSurface = EGL15.EGL_NO_SURFACE;
        boolean glesInitialized = false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer displayAttrs = stack.pointers(EGL_PLATFORM_ANGLE_TYPE_ANGLE, backend.eglType, EGL15.EGL_NONE);
            // LWJGL's checked wrappers reject native_display == 0 here, so call the function pointer directly.
            // display = org.lwjgl.egl.EGL15.eglGetPlatformDisplay(EGL_PLATFORM_ANGLE_ANGLE, 0L, displayAttrs);
            newDisplay = JNI.callPPP(EGL_PLATFORM_ANGLE_ANGLE, 0L, MemoryUtil.memAddressSafe(displayAttrs), EGL.getCapabilities().eglGetPlatformDisplay);
            if (newDisplay == EGL15.EGL_NO_DISPLAY)
                throw eglError("eglGetPlatformDisplay");
            display = newDisplay;

            IntBuffer major = stack.mallocInt(1);
            IntBuffer minor = stack.mallocInt(1);
            if (!EGL15.eglInitialize(newDisplay, major, minor))
                throw eglError("eglInitialize");
            if (!EGL15.eglBindAPI(EGL15.EGL_OPENGL_ES_API))
                throw eglError("eglBindAPI");

            int samples = GL.SAMPLES > 1 ? GL.SAMPLES : 0;
            long config = chooseConfig(stack, newDisplay, samples, surfaceKind.eglBit);
            if (config == 0L)
                throw eglError("eglChooseConfig");
            logChosenConfig(stack, config);

            IntBuffer contextAttrs = stack.ints(EGL15.EGL_CONTEXT_CLIENT_VERSION, 3, EGL15.EGL_NONE);
            newContext = EGL15.eglCreateContext(newDisplay, config, EGL15.EGL_NO_CONTEXT, contextAttrs);
            if (newContext == EGL15.EGL_NO_CONTEXT)
                throw eglError("eglCreateContext");

            if (surfaceKind == SurfaceKind.WINDOW) {
                newSurface = EGL15.eglCreateWindowSurface(newDisplay, config, nativeWindowHandle, stack.ints(EGL15.EGL_NONE));
                if (newSurface == EGL15.EGL_NO_SURFACE)
                    throw eglError("eglCreateWindowSurface");
            } else {
                newSurface = EGL15.eglCreatePbufferSurface(newDisplay, config, stack.ints(
                        EGL15.EGL_WIDTH, pbufferWidth,
                        EGL15.EGL_HEIGHT, pbufferHeight,
                        EGL15.EGL_NONE));
                if (newSurface == EGL15.EGL_NO_SURFACE)
                    throw eglError("eglCreatePbufferSurface");
            }

            if (!EGL15.eglMakeCurrent(newDisplay, newSurface, newSurface, newContext))
                throw eglError("eglMakeCurrent");
            GLES.createCapabilities();
            glesInitialized = true;
            GL.initInfo();
            Log.info("OpenGL context: " + GL.contextDescription());
            GLRenderer.init();
        } catch (RuntimeException | Error e) {
            try {
                if (glesInitialized)
                    GLES.setCapabilities(null);
                if (newDisplay != EGL15.EGL_NO_DISPLAY) {
                    EGL15.eglMakeCurrent(newDisplay, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_CONTEXT);
                    if (newSurface != EGL15.EGL_NO_SURFACE)
                        EGL15.eglDestroySurface(newDisplay, newSurface);
                    if (newContext != EGL15.EGL_NO_CONTEXT)
                        EGL15.eglDestroyContext(newDisplay, newContext);
                    EGL15.eglTerminate(newDisplay);
                }
            } finally {
                activeRenderer = null;
            }
            throw e;
        }

        context = newContext;
        surface = newSurface;
    }

    public void render(Position viewpoint, boolean surfaceResized) {
        if (!EGL15.eglMakeCurrent(display, surface, surface, context))
            throw eglError("eglMakeCurrent");
        // Synchronize native window dimensions before drawing; ANGLE otherwise resizes after swap.
        if (surfaceResized && (backend == Backend.D3D11 || backend == Backend.OPENGL)) {
            if (!EGL15.eglWaitNative(EGL15.EGL_CORE_NATIVE_ENGINE))
                throw eglError("eglWaitNative");
        }
        GLRenderer.display(viewpoint);
        if (swapBuffers && !EGL15.eglSwapBuffers(display, surface))
            throw eglError("eglSwapBuffers");
    }

    public void destroy() {
        if (activeRenderer != this)
            throw new IllegalStateException("AngleRenderer is not active");
        try {
            // A failed swap leaves this context current, including after device loss.
            // Do not try to bind it again: ANGLE rejects binding a lost context.
            if (EGL15.eglGetCurrentContext() != context && !EGL15.eglMakeCurrent(display, surface, surface, context))
                throw eglError("eglMakeCurrent");
            GLRenderer.dispose();
        } finally {
            try {
                GLES.setCapabilities(null);
                EGL15.eglMakeCurrent(display, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_CONTEXT);
                EGL15.eglDestroySurface(display, surface);
                EGL15.eglDestroyContext(display, context);
                EGL15.eglTerminate(display);
            } finally {
                activeRenderer = null;
            }
        }
    }

    private static void ensureLwjglAngleConfigured() {
        if (lwjglConfigured)
            return;
        AngleLibraries.configureLwjglAngleLibraries();
        EGL.getCapabilities();
        lwjglConfigured = true;
    }

    private static long chooseConfig(MemoryStack stack, long display, int samples, int surfaceType) {
        for (int depthBits : DEPTH_PREFERENCES) {
            if (samples > 0) {
                long config = chooseConfig(stack, display, depthBits, samples, surfaceType);
                if (config != 0L)
                    return config;
            }
        }
        for (int depthBits : DEPTH_PREFERENCES) {
            long config = chooseConfig(stack, display, depthBits, 0, surfaceType);
            if (config != 0L)
                return config;
        }
        return 0L;
    }

    private static long chooseConfig(MemoryStack stack, long display, int depthBits, int samples, int surfaceType) {
        PointerBuffer configOut = stack.mallocPointer(1);
        IntBuffer numConfigs = stack.mallocInt(1);
        int attributeCount = samples > 0 ? 19 : 15;
        IntBuffer configAttrs = stack.mallocInt(attributeCount);
        configAttrs.put(EGL15.EGL_SURFACE_TYPE).put(surfaceType);
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

    private void logChosenConfig(MemoryStack stack, long config) {
        IntBuffer attribValue = stack.mallocInt(1);
        int red = configAttrib(attribValue, config, EGL15.EGL_RED_SIZE);
        int green = configAttrib(attribValue, config, EGL15.EGL_GREEN_SIZE);
        int blue = configAttrib(attribValue, config, EGL15.EGL_BLUE_SIZE);
        int alpha = configAttrib(attribValue, config, EGL15.EGL_ALPHA_SIZE);
        int depth = configAttrib(attribValue, config, EGL15.EGL_DEPTH_SIZE);
        int stencil = configAttrib(attribValue, config, EGL15.EGL_STENCIL_SIZE);
        int sampleBuffers = configAttrib(attribValue, config, EGL15.EGL_SAMPLE_BUFFERS);
        int samples = configAttrib(attribValue, config, EGL15.EGL_SAMPLES);

        Log.info("ANGLE EGL config: backend=" + backend.label
                + " rgba=" + red + "/" + green + "/" + blue + "/" + alpha
                + " depth=" + depth
                + " stencil=" + stencil
                + " sampleBuffers=" + sampleBuffers
                + " samples=" + samples);
    }

    private int configAttrib(IntBuffer value, long config, int attribute) {
        if (!EGL15.eglGetConfigAttrib(display, config, attribute, value))
            throw eglError("eglGetConfigAttrib");
        return value.get(0);
    }

    private RuntimeException eglError(String step) {
        int code = EGL15.eglGetError();
        if (code == EGL15.EGL_SUCCESS)
            return new RuntimeException(step + " failed without EGL error; backend=" + backend.label);
        if (code == EGL15.EGL_CONTEXT_LOST)
            return new ContextLostException(step + " failed with EGL_CONTEXT_LOST; backend=" + backend.label);
        return new RuntimeException(step + " failed with EGL error 0x" + Integer.toHexString(code) + "; backend=" + backend.label);
    }

}
