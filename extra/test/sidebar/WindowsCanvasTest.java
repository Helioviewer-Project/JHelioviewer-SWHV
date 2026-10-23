package org.helioviewer.jhv.opengl;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.opengl.angle.WinAngleBridge;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.User32;

public final class WindowsCanvasTest {
    public static void main(String[] args) throws Exception {
        Platform.init();
        if (!Platform.isWindows())
            return;
        Directories.createPersistentDirs();
        SwingUtilities.invokeAndWait(WindowsCanvasTest::checkPosition);
        System.out.println("WindowsCanvasTest passed");
    }

    private static void checkPosition() {
        JFrame frame = new JFrame("Canvas positioning test");
        try {
            JPanel content = new JPanel(new BorderLayout());
            JPanel toolbar = new JPanel();
            toolbar.setPreferredSize(new Dimension(400, 47));
            content.add(toolbar, BorderLayout.NORTH);
            JPanel sidebar = new JPanel();
            sidebar.setPreferredSize(new Dimension(173, 300));
            content.add(sidebar, BorderLayout.WEST);
            Canvas canvas = new Canvas();
            content.add(canvas, BorderLayout.CENTER);
            frame.setContentPane(content);
            frame.setBounds(30, 40, 640, 420);
            frame.setVisible(true);
            long handle = WinAngleBridge.hwnd(canvas);
            if (handle == 0L)
                throw new AssertionError("Missing canvas HWND");
            Rectangle javaBounds = canvas.getBounds();
            Rectangle original = nativeBounds(handle);
            // Reproduce the DPI peer update that loses the lightweight parent offsets.
            if (!User32.SetWindowPos(null, handle, 0L, 0, 0, 0, 0,
                    User32.SWP_NOSIZE | User32.SWP_NOZORDER | User32.SWP_NOACTIVATE))
                throw new AssertionError("Cannot reposition canvas");
            if (original.equals(nativeBounds(handle)))
                throw new AssertionError("Canvas was not displaced");
            WinAngleBridge.syncPosition(canvas);
            if (!original.equals(nativeBounds(handle)))
                throw new AssertionError("Canvas position not restored: " + original + " versus " + nativeBounds(handle));
            if (!javaBounds.equals(canvas.getBounds()))
                throw new AssertionError("Canvas logical bounds changed");
        } finally {
            frame.dispose();
        }
    }

    private static Rectangle nativeBounds(long handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            RECT bounds = RECT.malloc(stack);
            if (!User32.GetWindowRect(null, handle, bounds))
                throw new AssertionError("Cannot read canvas bounds");
            return new Rectangle(bounds.left(), bounds.top(), bounds.right() - bounds.left(), bounds.bottom() - bounds.top());
        }
    }
}
