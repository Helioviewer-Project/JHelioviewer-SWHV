package org.helioviewer.jhv.opengl.angle;

import java.awt.Canvas;

import org.lwjgl.system.jawt.JAWTX11DrawingSurfaceInfo;

public final class X11AngleBridge {
    public static long drawable(Canvas canvas) {
        Long drawable = AngleJAWT.withPlatformInfo(canvas, platformInfo -> {
            if (platformInfo == 0L)
                return 0L;

            JAWTX11DrawingSurfaceInfo surfaceInfo = JAWTX11DrawingSurfaceInfo.create(platformInfo);
            long display = surfaceInfo.display();
            long x11Drawable = surfaceInfo.drawable();
            return display == 0L || x11Drawable == 0L ? 0L : x11Drawable;
        });
        return drawable == null ? 0L : drawable;
    }

    private X11AngleBridge() {}
}
