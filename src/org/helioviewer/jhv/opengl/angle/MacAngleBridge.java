package org.helioviewer.jhv.opengl.angle;

import java.awt.Canvas;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

@SuppressWarnings("restricted")
public final class MacAngleBridge {
    public record Host(long handle, long layer) {}

    private static final Arena ARENA = Arena.ofShared();
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = SymbolLookup.libraryLookup(
            AngleLibraries.libraryPath("libjhvmetalhost.dylib"), ARENA);

    private static final MethodHandle CREATE = downcall("jhv_metal_host_create",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
    private static final MethodHandle GET_LAYER = downcall("jhv_metal_host_get_layer",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    private static final MethodHandle SET_SCALE = downcall("jhv_metal_host_set_scale",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE));
    private static final MethodHandle SET_VISIBLE = downcall("jhv_metal_host_set_visible",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
    private static final MethodHandle DESTROY = downcall("jhv_metal_host_destroy",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    public static void prewarm() {
        // Force class initialization and native symbol resolution before the first canvas attach.
    }

    public static Host create(Canvas canvas, double x, double y, double width, double height) {
        return AngleJAWT.withPlatformInfo(canvas, platformInfo -> {
            if (platformInfo == 0L)
                return null;

            long handle = 0L;
            try {
                MemorySegment surfaceLayers = MemorySegment.ofAddress(platformInfo);
                handle = ((MemorySegment) CREATE.invokeExact(surfaceLayers, x, y, width, height)).address();
                if (handle == 0L)
                    return null;

                MemorySegment metalHost = MemorySegment.ofAddress(handle);
                long layer = ((MemorySegment) GET_LAYER.invokeExact(metalHost)).address();
                if (layer == 0L)
                    throw new IllegalStateException("Metal host did not expose a CAMetalLayer");
                return new Host(handle, layer);
            } catch (Throwable t) {
                if (handle != 0L)
                    destroy(handle);
                throw new RuntimeException("Failed to create Metal host layer", t);
            }
        });
    }

    public static void setScale(long handle, double scale) {
        try {
            MemorySegment metalHost = MemorySegment.ofAddress(handle);
            SET_SCALE.invokeExact(metalHost, scale);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to scale Metal host layer", t);
        }
    }

    public static void setVisible(long handle, boolean visible) {
        try {
            MemorySegment metalHost = MemorySegment.ofAddress(handle);
            SET_VISIBLE.invokeExact(metalHost, visible ? 1 : 0);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to change Metal host visibility", t);
        }
    }

    public static void destroy(long handle) {
        if (handle == 0L)
            return;

        try {
            MemorySegment metalHost = MemorySegment.ofAddress(handle);
            DESTROY.invokeExact(metalHost);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to destroy Metal host layer", t);
        }
    }

    private static MethodHandle downcall(String symbol, FunctionDescriptor descriptor) {
        MemorySegment function = LOOKUP.find(symbol).orElseThrow(() -> new UnsatisfiedLinkError(symbol));
        return LINKER.downcallHandle(function, descriptor);
    }

    private MacAngleBridge() {}
}
