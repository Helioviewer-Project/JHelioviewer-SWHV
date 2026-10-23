package org.helioviewer.jhv.opengl.angle;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.Window;
import java.awt.geom.AffineTransform;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.jawt.JAWTWin32DrawingSurfaceInfo;
import org.lwjgl.system.windows.POINT;
import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.User32;

public final class WinAngleBridge {
    public static long hwnd(Canvas canvas) {
        Long hwnd = AngleJAWT.withPlatformInfo(canvas, platformInfo -> {
            if (platformInfo == 0L)
                return 0L;
            return JAWTWin32DrawingSurfaceInfo.create(platformInfo).hwnd();
        });
        return hwnd == null ? 0L : hwnd;
    }

    // AWT's Windows DPI update omits lightweight ancestors when repositioning the peer.
    public static void syncPosition(Canvas canvas) {
        int x = canvas.getX();
        int y = canvas.getY();
        Component parent = canvas.getParent();
        while (parent != null && parent.isLightweight()) {
            x += parent.getX();
            y += parent.getY();
            parent = parent.getParent();
        }
        AffineTransform transform = canvas.getGraphicsConfiguration().getDefaultTransform();
        // Match AWT's native ClipRound, which rounds exact halves down.
        x = (int) Math.ceil(x * transform.getScaleX() - .5);
        y = (int) Math.ceil(y * transform.getScaleY() - .5);
        long handle = hwnd(canvas);
        if (handle == 0L)
            return;
        if (parent instanceof Window) {
            // Java's logical insets lose precision at fractional display scales.
            try (MemoryStack stack = MemoryStack.stackPush()) {
                long window = User32.GetWindowLongPtr(handle, User32.GWL_HWNDPARENT);
                RECT bounds = RECT.malloc(stack);
                POINT client = POINT.calloc(stack);
                if (!User32.GetWindowRect(null, window, bounds) || !User32.ClientToScreen(window, client))
                    throw new IllegalStateException("Cannot read Windows canvas parent bounds");
                x -= client.x() - bounds.left();
                y -= client.y() - bounds.top();
            }
        }
        if (!User32.SetWindowPos(null, handle, 0L, x, y, 0, 0,
                User32.SWP_NOSIZE | User32.SWP_NOZORDER | User32.SWP_NOACTIVATE))
            throw new IllegalStateException("Cannot synchronize Windows canvas position");
    }

    private WinAngleBridge() {}
}
