package org.helioviewer.jhv.opengl.angle;

import java.awt.Canvas;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.jawt.JAWT;
import org.lwjgl.system.jawt.JAWTDrawingSurface;
import org.lwjgl.system.jawt.JAWTDrawingSurfaceInfo;
import org.lwjgl.system.jawt.JAWTFunctions;

final class AngleJAWT {
    interface PlatformInfoAccess<T> {
        T apply(long platformInfo);
    }

    private AngleJAWT() {}

    static <T> T withPlatformInfo(Canvas canvas, PlatformInfoAccess<T> access) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            JAWT awt = JAWT.calloc(stack);
            awt.version(JAWTFunctions.JAWT_VERSION_9);
            if (!JAWTFunctions.JAWT_GetAWT(awt))
                throw new IllegalStateException("JAWT_GetAWT failed");

            long freeDrawingSurface = awt.FreeDrawingSurface();
            JAWTDrawingSurface drawingSurface = JAWTFunctions.JAWT_GetDrawingSurface(canvas, awt.GetDrawingSurface());
            if (drawingSurface == null)
                return null;

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
                        return access.apply(drawingSurfaceInfo.platformInfo());
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
}
