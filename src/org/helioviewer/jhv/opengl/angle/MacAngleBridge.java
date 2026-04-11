package org.helioviewer.jhv.opengl.angle;

import java.awt.Canvas;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.jawt.JAWT;
import org.lwjgl.system.jawt.JAWTFunctions;
import org.lwjgl.system.jawt.JAWTDrawingSurface;
import org.lwjgl.system.jawt.JAWTDrawingSurfaceInfo;

@SuppressWarnings("restricted")
public final class MacAngleBridge {
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

    private MacAngleBridge() {
    }

    public static long create(Canvas canvas, double x, double y, double width, double height) {
        long surfaceLayersPointer = surfaceLayersPointer(canvas);
        if (surfaceLayersPointer == 0L)
            return 0L;

        try {
            MemorySegment surfaceLayers = MemorySegment.ofAddress(surfaceLayersPointer);
            return ((MemorySegment) CREATE.invokeExact(surfaceLayers, x, y, width, height)).address();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create Metal host layer", t);
        }
    }

    public static void setFrame(long handle, double x, double y, double width, double height) {
        try {
            MemorySegment metalHost = MemorySegment.ofAddress(handle);
            SET_FRAME.invokeExact(metalHost, x, y, width, height);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to resize Metal host layer", t);
        }
    }

    public static long getLayer(long handle) {
        try {
            MemorySegment metalHost = MemorySegment.ofAddress(handle);
            return ((MemorySegment) GET_LAYER.invokeExact(metalHost)).address();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to resolve Metal layer", t);
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

    private static long surfaceLayersPointer(Canvas canvas) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            JAWT awt = JAWT.calloc(stack);
            awt.version(JAWTFunctions.JAWT_VERSION_9);
            if (!JAWTFunctions.JAWT_GetAWT(awt))
                throw new IllegalStateException("JAWT_GetAWT failed");

            long freeDrawingSurface = awt.FreeDrawingSurface();
            JAWTDrawingSurface drawingSurface = JAWTFunctions.JAWT_GetDrawingSurface(canvas, awt.GetDrawingSurface());
            if (drawingSurface == null)
                return 0L;

            try {
                long lockDrawingSurface = drawingSurface.Lock();
                long unlockDrawingSurface = drawingSurface.Unlock();
                long getSurfaceInfo = drawingSurface.GetDrawingSurfaceInfo();
                long freeSurfaceInfo = drawingSurface.FreeDrawingSurfaceInfo();
                int lock = JAWTFunctions.JAWT_DrawingSurface_Lock(drawingSurface, lockDrawingSurface);
                if ((lock & JAWTFunctions.JAWT_LOCK_ERROR) != 0)
                    throw new IllegalStateException("JAWT_DrawingSurface_Lock failed");

                try {
                    JAWTDrawingSurfaceInfo drawingSurfaceInfo = JAWTFunctions.JAWT_DrawingSurface_GetDrawingSurfaceInfo(drawingSurface, getSurfaceInfo);
                    if (drawingSurfaceInfo == null)
                        throw new IllegalStateException("JAWT_DrawingSurface_GetDrawingSurfaceInfo failed");

                    try {
                        return drawingSurfaceInfo.platformInfo();
                    } finally {
                        JAWTFunctions.JAWT_DrawingSurface_FreeDrawingSurfaceInfo(drawingSurfaceInfo, freeSurfaceInfo);
                    }
                } finally {
                    JAWTFunctions.JAWT_DrawingSurface_Unlock(drawingSurface, unlockDrawingSurface);
                }
            } finally {
                JAWTFunctions.JAWT_FreeDrawingSurface(drawingSurface, freeDrawingSurface);
            }
        }
    }

    private static MethodHandle downcall(String symbol, FunctionDescriptor descriptor) {
        MemorySegment function = LOOKUP.find(symbol).orElseThrow(() -> new UnsatisfiedLinkError(symbol));
        return LINKER.downcallHandle(function, descriptor);
    }
}
