package org.helioviewer.jhv.opengl.angle;

import java.awt.Canvas;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import org.helioviewer.jhv.app.Log;

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
    private static final MethodHandle SET_FRAME = downcall("jhv_metal_host_set_frame",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
                    ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
    private static final MethodHandle SET_FRAME_SYNC = downcall("jhv_metal_host_set_frame_sync",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
                    ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
    private static final MethodHandle DESTROY = downcall("jhv_metal_host_destroy",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    private static final MethodHandle DEVICE_INFO = downcall("jhv_metal_device_info",
            FunctionDescriptor.of(ValueLayout.ADDRESS));

    public static void prewarm() {
        // Force class initialization and native symbol resolution before the first canvas attach.
        Log.info("Metal device: " + deviceInfo());
    }

    private static String deviceInfo() {
        try {
            MemorySegment info = (MemorySegment) DEVICE_INFO.invokeExact();
            if (info.address() == 0L)
                return "unavailable";
            return info.reinterpret(Long.MAX_VALUE).getString(0);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to query Metal device info", t);
        }
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
                if (layer == 0L) {
                    DESTROY.invokeExact(metalHost);
                    handle = 0L;
                    throw new IllegalStateException("Metal host did not expose a CAMetalLayer");
                }
                return new Host(handle, layer);
            } catch (Throwable t) {
                if (handle != 0L)
                    destroy(handle);
                throw new RuntimeException("Failed to create Metal host layer", t);
            }
        });
    }

    public static void setFrame(long handle, double x, double y, double width, double height) {
        try {
            MemorySegment metalHost = MemorySegment.ofAddress(handle);
            SET_FRAME.invokeExact(metalHost, x, y, width, height);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to resize Metal host layer", t);
        }
    }

    // Blocks until the layer frame + drawableSize are updated, so an immediate render is at-size.
    public static void setFrameSync(long handle, double x, double y, double width, double height) {
        try {
            MemorySegment metalHost = MemorySegment.ofAddress(handle);
            SET_FRAME_SYNC.invokeExact(metalHost, x, y, width, height);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to resize Metal host layer", t);
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
