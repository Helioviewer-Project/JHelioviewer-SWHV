package org.helioviewer.jhv.opengl.angle;

import java.awt.Canvas;

import org.lwjgl.system.jawt.JAWTWin32DrawingSurfaceInfo;

public final class WinAngleBridge {
    private WinAngleBridge() {
    }

    public static long hwnd(Canvas canvas) {
        Long hwnd = AngleJAWT.withPlatformInfo(canvas, platformInfo -> {
            if (platformInfo == 0L)
                return 0L;
            return JAWTWin32DrawingSurfaceInfo.create(platformInfo).hwnd();
        });
        return hwnd == null ? 0L : hwnd;
    }
}
