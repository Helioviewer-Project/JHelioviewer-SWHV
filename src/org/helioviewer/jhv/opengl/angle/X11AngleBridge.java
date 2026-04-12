package org.helioviewer.jhv.opengl.angle;

import java.awt.Canvas;

import org.lwjgl.system.jawt.JAWTX11DrawingSurfaceInfo;

public final class X11AngleBridge {
    public record Surface(long display, long drawable) {
    }

    private X11AngleBridge() {
    }

    public static Surface surface(Canvas canvas) {
        return AngleJAWT.withPlatformInfo(canvas, platformInfo -> {
            if (platformInfo == 0L)
                return null;

            JAWTX11DrawingSurfaceInfo surfaceInfo = JAWTX11DrawingSurfaceInfo.create(platformInfo);
            long display = surfaceInfo.display();
            long drawable = surfaceInfo.drawable();
            if (display == 0L || drawable == 0L)
                return null;
            return new Surface(display, drawable);
        });
    }
}
