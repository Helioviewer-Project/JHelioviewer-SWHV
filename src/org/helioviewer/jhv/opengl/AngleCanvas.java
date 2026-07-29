package org.helioviewer.jhv.opengl;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.opengl.angle.MacAngleBridge;
import org.helioviewer.jhv.opengl.angle.WinAngleBridge;
import org.helioviewer.jhv.opengl.angle.X11AngleBridge;

@SuppressWarnings("serial")
public final class AngleCanvas extends Canvas {
    private long macHostHandle;
    private AngleRenderer angleRenderer;
    private boolean displayPending;
    private Position pendingViewpoint;
    private boolean hostUpdatePending;
    private boolean hostRenderPending;
    private int fps;
    private int fpsCount;
    private long fpsTime = System.currentTimeMillis();
    private int lastGlWidth = -1;
    private int lastGlHeight = -1;
    private boolean hostVisible = true;
    private boolean nativeHostVisible = true;
    private double nativeHostScale = Double.NaN;

    public AngleCanvas() {
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setBackground(Color.BLACK);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                scheduleHostUpdate(true);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                // Force a redraw after AWT resize so the GL pixel size is recomputed
                // immediately and the aspect ratio does not lag behind the canvas size.
                invalidateGlSize();
                scheduleHostUpdate(true);
            }
        });
        addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            @Override
            public void ancestorMoved(HierarchyEvent e) {
                scheduleHostUpdate(false);
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        scheduleHostUpdate(true);
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
        requestRender(GLRenderer.getDisplayedViewpoint());
    }

    public void requestRender(Position viewpoint) {
        pendingViewpoint = viewpoint;
        queueRender();
    }

    private void queueRender() {
        if (displayPending)
            return;

        if (angleRenderer == null) {
            scheduleHostUpdate(true);
            return;
        }

        displayPending = true;
        EventQueue.invokeLater(() -> {
            displayPending = false;
            Position renderViewpoint = pendingViewpoint;
            pendingViewpoint = null;
            renderNow(renderViewpoint);
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

    public void setHostVisible(boolean visible) {
        if (!Platform.isMacOS() || hostVisible == visible)
            return;

        hostVisible = visible;
        if (!visible && macHostHandle != 0L && nativeHostVisible) {
            MacAngleBridge.setVisible(macHostHandle, false);
            nativeHostVisible = false;
        } else if (visible) {
            scheduleHostUpdate(true);
        }
    }

    // Render one frame and keep the shared viewport state in sync with the canvas size.
    private void renderNow(Position viewpoint) {
        if (!hostVisible)
            return;

        refreshPixelScale();
        attachIfNeeded();
        if (angleRenderer == null)
            return;

        syncHostScale();
        int glWidth = (int) (getWidth() * Display.pixelScale[0] + .5);
        int glHeight = (int) (getHeight() * Display.pixelScale[1] + .5);
        if (glWidth != lastGlWidth || glHeight != lastGlHeight) {
            GLRenderer.reshape(glWidth, glHeight);
            lastGlWidth = glWidth;
            lastGlHeight = glHeight;
        }
        angleRenderer.render(viewpoint);
        fpsCount++;
    }

    // Create the platform-native host/window handle and ANGLE renderer on first use.
    private void attachIfNeeded() {
        if (angleRenderer != null || !isDisplayable() || getWidth() <= 0 || getHeight() <= 0)
            return;

        long newHostHandle = 0L;
        long newNativeWindowHandle = 0L;
        try {
            if (Platform.isMacOS()) {
                JRootPane rootPane = SwingUtilities.getRootPane(this);
                Point location = rootPane == null ? getLocation() :
                        SwingUtilities.convertPoint(this, 0, 0, rootPane.getContentPane());
                MacAngleBridge.Host host = MacAngleBridge.create(
                        this, location.x, location.y, getWidth(), getHeight());
                if (host == null)
                    return;
                newHostHandle = host.handle();
                newNativeWindowHandle = host.layer();
                if (!hostVisible)
                    MacAngleBridge.setVisible(newHostHandle, false);
            } else if (Platform.isWindows()) {
                newNativeWindowHandle = WinAngleBridge.hwnd(this);
            } else if (Platform.isLinux()) {
                newNativeWindowHandle = X11AngleBridge.drawable(this);
            }
            if (newNativeWindowHandle == 0L)
                return;

            AngleRenderer renderer = new AngleRenderer(newNativeWindowHandle);
            macHostHandle = newHostHandle;
            angleRenderer = renderer;
            nativeHostVisible = hostVisible;
            if (Platform.isMacOS())
                nativeHostScale = Display.pixelScale[0];
            invalidateGlSize();
        } catch (RuntimeException | Error e) {
            if (newHostHandle != 0L)
                MacAngleBridge.destroy(newHostHandle);
            throw e;
        }
    }

    // Keep native scale and visibility synchronized, then trigger a redraw if needed.
    private void updateHost(boolean renderNeeded) {
        if (getWidth() <= 0 || getHeight() <= 0)
            return;

        boolean pixelScaleChanged = refreshPixelScale();

        if (angleRenderer == null) {
            attachIfNeeded();
            if (angleRenderer == null)
                return;
        }

        if (Platform.isMacOS()) {
            syncHostScale();
            if (hostVisible != nativeHostVisible) {
                MacAngleBridge.setVisible(macHostHandle, hostVisible);
                nativeHostVisible = hostVisible;
            }
        }
        if (hostVisible && (renderNeeded || pixelScaleChanged || lastGlWidth < 0 || lastGlHeight < 0)) {
            if (pendingViewpoint == null)
                pendingViewpoint = GLRenderer.getDisplayedViewpoint();
            queueRender();
        }
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
            updateHost(render);
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
                if (Platform.isMacOS() && macHostHandle != 0L)
                    MacAngleBridge.destroy(macHostHandle);
            } finally {
                macHostHandle = 0L;
                nativeHostVisible = true;
                nativeHostScale = Double.NaN;
                displayPending = hostUpdatePending = hostRenderPending = false;
                invalidateGlSize();
            }
        }
    }

    private void invalidateGlSize() {
        lastGlWidth = -1;
        lastGlHeight = -1;
    }

    private void syncHostScale() {
        if (!Platform.isMacOS() || nativeHostScale == Display.pixelScale[0])
            return;

        MacAngleBridge.setScale(macHostHandle, Display.pixelScale[0]);
        nativeHostScale = Display.pixelScale[0];
    }

    // Keep the shared pixel scale in sync and invalidate the GL size if a monitor switch
    // changed the backing pixel ratio.
    private boolean refreshPixelScale() {
        boolean changed = updatePixelScale();
        if (changed)
            invalidateGlSize();
        return changed;
    }

    // Track the current HiDPI scale so GL sizes and UI coordinate conversion stay aligned.
    private boolean updatePixelScale() {
        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        double scaleX = 1;
        double scaleY = 1;
        if (graphicsConfiguration != null) {
            AffineTransform transform = graphicsConfiguration.getDefaultTransform();
            scaleX = transform.getScaleX();
            scaleY = transform.getScaleY();
        }
        boolean changed = Display.pixelScale[0] != scaleX || Display.pixelScale[1] != scaleY;
        if (!changed)
            return false;

        Display.pixelScale[0] = scaleX;
        Display.pixelScale[1] = scaleY;
        return true;
    }

}
