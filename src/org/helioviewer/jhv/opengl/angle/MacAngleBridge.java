package org.helioviewer.jhv.opengl.angle;

import java.awt.Canvas;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

@SuppressWarnings("restricted")
public final class MacAngleBridge {
    private static final Method GET_COMPONENT_ACCESSOR;
    private static final Method GET_PEER;
    private static final Method GET_PLATFORM_WINDOW;
    private static final Method GET_CONTENT_VIEW;
    private static final Method GET_AWT_VIEW;

    private static final Arena ARENA = Arena.ofShared();
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = SymbolLookup.libraryLookup(
            AngleLibraries.libraryPath("libjhvmetalhost.dylib"), ARENA);

    private static final MethodHandle CREATE = downcall("jhv_metal_host_create",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
    private static final MethodHandle GET_LAYER = downcall("jhv_metal_host_get_layer",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    private static final MethodHandle SET_FRAME = downcall("jhv_metal_host_set_frame",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
                    ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
    private static final MethodHandle DESTROY = downcall("jhv_metal_host_destroy",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    static {
        try {
            GET_COMPONENT_ACCESSOR = Class.forName("sun.awt.AWTAccessor").getMethod("getComponentAccessor");
            GET_PEER = Class.forName("sun.awt.AWTAccessor$ComponentAccessor").getMethod("getPeer", java.awt.Component.class);
            GET_PLATFORM_WINDOW = Class.forName("sun.lwawt.LWComponentPeer").getMethod("getPlatformWindow");
            GET_CONTENT_VIEW = Class.forName("sun.lwawt.macosx.CPlatformWindow").getMethod("getContentView");
            GET_AWT_VIEW = Class.forName("sun.lwawt.macosx.CPlatformView").getMethod("getAWTView");
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private MacAngleBridge() {
    }

    public static long create(Canvas canvas, double x, double y, double width, double height) {
        long hostPointer = awtViewPointer(canvas);
        if (hostPointer == 0L)
            return 0L;

        try {
            return ((MemorySegment) CREATE.invokeExact(MemorySegment.ofAddress(hostPointer), x, y, width, height)).address();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create Metal host layer", t);
        }
    }

    public static void setFrame(long handle, double x, double y, double width, double height) {
        try {
            SET_FRAME.invokeExact(MemorySegment.ofAddress(handle), x, y, width, height);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to resize Metal host layer", t);
        }
    }

    public static long getLayer(long handle) {
        try {
            return ((MemorySegment) GET_LAYER.invokeExact(MemorySegment.ofAddress(handle))).address();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to resolve Metal layer", t);
        }
    }

    public static void destroy(long handle) {
        if (handle == 0L)
            return;

        try {
            DESTROY.invokeExact(MemorySegment.ofAddress(handle));
        } catch (Throwable t) {
            throw new RuntimeException("Failed to destroy Metal host layer", t);
        }
    }

    private static long awtViewPointer(Canvas canvas) {
        try {
            Object componentAccessor = GET_COMPONENT_ACCESSOR.invoke(null);
            Object peer = GET_PEER.invoke(componentAccessor, canvas);
            if (peer == null)
                return 0L;

            Object platformWindow = GET_PLATFORM_WINDOW.invoke(peer);
            if (platformWindow == null)
                return 0L;

            Object contentView = GET_CONTENT_VIEW.invoke(platformWindow);
            if (contentView == null)
                return 0L;

            return ((Number) GET_AWT_VIEW.invoke(contentView)).longValue();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to resolve macOS AWT host view", e);
        }
    }

    private static MethodHandle downcall(String symbol, FunctionDescriptor descriptor) {
        MemorySegment function = LOOKUP.find(symbol).orElseThrow(() -> new UnsatisfiedLinkError(symbol));
        return LINKER.downcallHandle(function, descriptor);
    }
}
