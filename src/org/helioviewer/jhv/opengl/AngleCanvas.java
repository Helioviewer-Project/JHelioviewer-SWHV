package org.helioviewer.jhv.opengl;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.Platform;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.opengl.angle.MacAngleBridge;
import org.helioviewer.jhv.opengl.angle.WinAngleBridge;
import org.helioviewer.jhv.opengl.angle.X11AngleBridge;

@SuppressWarnings("serial")
public final class AngleCanvas extends Canvas {
    private long macHostHandle;
    private long nativeDisplayHandle;
    private long nativeWindowHandle;
    private boolean whiteBackground;
    private AngleRenderer angleRenderer;
    private boolean displayPending;
    private boolean hostUpdatePending;
    private boolean hostRenderPending;
    private int fps;
    private int fpsCount;
    private long fpsTime = System.currentTimeMillis();
    private int lastGlWidth = -1;
    private int lastGlHeight = -1;
    private Rectangle lastHostBounds;

    public AngleCanvas() {
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setBackground(Color.BLACK);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                scheduleHostUpdate(true);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                // Force a redraw after AWT resize so the GL pixel size is recomputed
                // immediately and the aspect ratio does not lag behind the canvas size.
                lastGlWidth = lastGlHeight = -1;
                updatePixelScale();
                scheduleHostUpdate(true);
            }
        });
        addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            @Override
            public void ancestorMoved(HierarchyEvent e) {
                scheduleHostUpdate(false);
            }

            @Override
            public void ancestorResized(HierarchyEvent e) {
                scheduleHostUpdate(true);
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        updatePixelScale();
        scheduleHostUpdate(true);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        int oldX = getX();
        int oldY = getY();
        int oldWidth = getWidth();
        int oldHeight = getHeight();
        boolean changed = x != oldX || y != oldY || width != oldWidth || height != oldHeight;
        boolean sizeChanged = width != oldWidth || height != oldHeight;
        super.setBounds(x, y, width, height);
        if (!changed)
            return;

        if (sizeChanged) {
            lastGlWidth = lastGlHeight = -1;
            updatePixelScale();
        }
        scheduleHostUpdate(sizeChanged);
    }

    @Override
    public void removeNotify() {
        try {
            detach();
        } finally {
            super.removeNotify();
        }
    }

    @Override
    public void paint(Graphics g) {
        // Native ANGLE host owns presentation.
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public void requestRender() {
        if (displayPending)
            return;

        if (angleRenderer == null) {
            scheduleHostUpdate(true);
            return;
        }

        displayPending = true;
        EventQueue.invokeLater(() -> {
            displayPending = false;
            renderNow();
        });
    }

    public int getFramerate() {
        long now = System.currentTimeMillis();
        long delta = now - fpsTime;

        if (delta > 1000) {
            fps = (int) ((1000L * fpsCount + delta / 2) / delta);
            fpsCount = 0;
            fpsTime = now;
        }
        return fps;
    }

    // Render one frame and keep the shared viewport state in sync with the canvas size.
    private void renderNow() {
        attachIfNeeded();
        if (angleRenderer == null)
            return;

        updatePixelScale();
        int glWidth = (int) (getWidth() * Display.pixelScale[0] + .5);
        int glHeight = (int) (getHeight() * Display.pixelScale[1] + .5);
        if (glWidth != lastGlWidth || glHeight != lastGlHeight) {
            GLRenderer.reshape(glWidth, glHeight);
            lastGlWidth = glWidth;
            lastGlHeight = glHeight;
        }
        angleRenderer.render(whiteBackground);

        Camera camera = Display.getCamera();
        if (Movie.isRecording())
            ExportMovie.handleMovieExport(camera);
        Layers.getViewpointLayer().updateTime(camera.getViewpoint().time);
        JHVFrame.getZoomStatusPanel().update(camera.getCameraWidth(), camera.getViewpoint().distance, Display.mode);

        fpsCount++;
    }

    public void setWhiteBackground(boolean white) {
        whiteBackground = white;
    }

    // Create the platform-native host/window handle and ANGLE renderer on first use.
    private void attachIfNeeded() {
        if (nativeWindowHandle != 0L || !isDisplayable() || getWidth() <= 0 || getHeight() <= 0)
            return;

        Rectangle bounds = hostBounds();
        long newHostHandle = 0L;
        long newNativeDisplayHandle = 0L;
        long newNativeWindowHandle;
        try {
            if (Platform.isMacOS()) {
                MacAngleBridge.Host host = MacAngleBridge.create(this, bounds.x, bounds.y, bounds.width, bounds.height);
                if (host == null)
                    return;
                newHostHandle = host.handle();
                newNativeWindowHandle = host.layer();
            } else if (Platform.isWindows()) {
                newNativeWindowHandle = WinAngleBridge.hwnd(this);
                if (newNativeWindowHandle == 0L)
                    return;
            } else if (Platform.isLinux()) {
                X11AngleBridge.Surface surface = X11AngleBridge.surface(this);
                if (surface == null)
                    return;
                newNativeDisplayHandle = surface.display();
                newNativeWindowHandle = surface.drawable();
            } else {
                return;
            }

            AngleRenderer renderer = new AngleRenderer(newNativeDisplayHandle, newNativeWindowHandle);
            macHostHandle = newHostHandle;
            nativeDisplayHandle = newNativeDisplayHandle;
            nativeWindowHandle = newNativeWindowHandle;
            angleRenderer = renderer;
            lastHostBounds = bounds;
            lastGlWidth = lastGlHeight = -1;
        } catch (RuntimeException | Error e) {
            if (newHostHandle != 0L)
                MacAngleBridge.destroy(newHostHandle);
            throw e;
        }
    }

    // Push the current AWT bounds to the native host, then trigger a redraw if needed.
    private void updateHostFrame(boolean renderNeeded) {
        if (getWidth() <= 0 || getHeight() <= 0)
            return;

        if (nativeWindowHandle == 0L) {
            attachIfNeeded();
            if (nativeWindowHandle == 0L)
                return;
        }
        if (angleRenderer == null)
            return;

        Rectangle bounds = hostBounds();
        if (Platform.isMacOS() && !bounds.equals(lastHostBounds))
            MacAngleBridge.setFrame(macHostHandle, bounds.x, bounds.y, bounds.width, bounds.height);
        lastHostBounds = bounds;
        if (renderNeeded || lastGlWidth < 0 || lastGlHeight < 0)
            requestRender();
    }

    // Coalesce host updates onto the EDT so move/resize bursts become one native update.
    private void scheduleHostUpdate(boolean renderNeeded) {
        hostRenderPending |= renderNeeded;
        if (hostUpdatePending || !isDisplayable())
            return;

        hostUpdatePending = true;
        EventQueue.invokeLater(() -> {
            hostUpdatePending = false;
            boolean render = hostRenderPending;
            hostRenderPending = false;
            updateHostFrame(render);
        });
    }

    // Tear down renderer and native host state, even if part of the shutdown path fails.
    private void detach() {
        try {
            if (angleRenderer != null)
                angleRenderer.destroy();
        } finally {
            angleRenderer = null;
            try {
                if (Platform.isMacOS())
                    MacAngleBridge.destroy(macHostHandle);
            } finally {
                macHostHandle = 0L;
                nativeDisplayHandle = 0L;
                nativeWindowHandle = 0L;
                displayPending = hostUpdatePending = hostRenderPending = false;
                lastHostBounds = null;
                lastGlWidth = lastGlHeight = -1;
            }
        }
    }

    // Track the current HiDPI scale so GL sizes and UI coordinate conversion stay aligned.
    private void updatePixelScale() {
        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        double scaleX = 1;
        double scaleY = 1;
        if (graphicsConfiguration != null) {
            AffineTransform transform = graphicsConfiguration.getDefaultTransform();
            scaleX = transform.getScaleX();
            scaleY = transform.getScaleY();
        }
        Display.pixelScale[0] = scaleX;
        Display.pixelScale[1] = scaleY;
    }

    // Express the canvas bounds relative to the Swing content pane for native host placement.
    private Rectangle hostBounds() {
        int width = getWidth();
        int height = getHeight();
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane == null)
            return new Rectangle(0, 0, width, height);

        Container contentPane = rootPane.getContentPane();
        try {
            Point canvasOnScreen = getLocationOnScreen();
            Point contentOnScreen = contentPane.getLocationOnScreen();
            return new Rectangle(canvasOnScreen.x - contentOnScreen.x, canvasOnScreen.y - contentOnScreen.y, width, height);
        } catch (IllegalComponentStateException ignore) {
        }

        int x = 0;
        int y = 0;
        for (Component current = this; current != null && current != contentPane; current = current.getParent()) {
            x += current.getX();
            y += current.getY();
        }
        return new Rectangle(x, y, width, height);
    }
}
