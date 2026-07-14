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
import java.awt.geom.AffineTransform;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.opengl.angle.MacAngleBridge;
import org.helioviewer.jhv.opengl.angle.WinAngleBridge;
import org.helioviewer.jhv.opengl.angle.X11AngleBridge;

@SuppressWarnings("serial")
public final class AngleCanvas extends Canvas {
    private long macHostHandle;
    private long nativeWindowHandle;
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
    private Rectangle lastHostBounds;
    private boolean hostResyncPending; // suppress renders that would draw at a size the drawable lacks

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
                refreshPixelScale();
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
        refreshPixelScale();
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
            invalidateGlSize();
            refreshPixelScale();
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

    // Synchronously resize the native host frame to the current canvas bounds and re-render, in the
    // same event. Needed after a programmatic layout change (e.g. collapsing the sidebar): the
    // ordinary resize path is deferred, so AWT paints the old framebuffer stretched to the new width
    // before the correct frame lands. Doing it inline here removes both the stretch and the flash.
    public void refreshHost() {
        if (getWidth() <= 0 || getHeight() <= 0)
            return;
        lastHostBounds = null; // force MacAngleBridge.setFrame even if bounds look unchanged
        invalidateGlSize();    // force GLRenderer.reshape in renderNow
        refreshPixelScale();
        if (nativeWindowHandle == 0L) {
            attachIfNeeded();
            if (nativeWindowHandle == 0L)
                return;
        }
        if (angleRenderer == null)
            return;

        Rectangle bounds = hostBounds();
        if (Platform.isMacOS())
            // Synchronous so the native drawable is resized before we render — otherwise the frame
            // renders at the old resolution (oblate) until a later native cycle (a click) fixes it.
            MacAngleBridge.setFrameSync(macHostHandle, bounds.x, bounds.y, bounds.width, bounds.height);
        lastHostBounds = bounds;
        renderNow(GLRenderer.getDisplayedViewpoint());
    }

    // Called as soon as a programmatic layout change starts, before Swing repaints: from here until
    // resyncHostDeferred runs, we refuse to draw at a size the drawable has not reached.
    public void beginHostResync() {
        hostResyncPending = true;
    }

    public void resyncHostDeferred() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            hostResyncPending = false;
            return;
        }
        attachIfNeeded();
        if (nativeWindowHandle == 0L || angleRenderer == null) {
            hostResyncPending = false;
            return;
        }

        refreshPixelScale();
        Rectangle bounds = hostBounds();
        // Asynchronous, always. A synchronous dispatch to the main thread from here deadlocks against
        // AppKit -- it does it when collapsing, and it does it when expanding.
        if (Platform.isMacOS())
            MacAngleBridge.setFrame(macHostHandle, bounds.x, bounds.y, bounds.width, bounds.height);
        lastHostBounds = bounds;
        invalidateGlSize();

        // Give the queued native resize a moment to land, then draw. Until it does, renders stay
        // suppressed, so no frame is drawn at a size the drawable has not reached.
        javax.swing.Timer timer = new javax.swing.Timer(16, e -> { // one frame: long enough for the queued native resize, short enough not to be seen
            hostResyncPending = false;
            renderNow(GLRenderer.getDisplayedViewpoint());
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void requestRender() {
        requestRender(GLRenderer.getDisplayedViewpoint());
    }

    public void requestRender(Position viewpoint) {
        pendingViewpoint = viewpoint;
        if (displayPending)
            return;

        if (angleRenderer == null) {
            scheduleHostUpdate(true, viewpoint);
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

    // Render one frame and keep the shared viewport state in sync with the canvas size.
    private void renderNow(Position viewpoint) {
        attachIfNeeded();
        if (angleRenderer == null)
            return;

        refreshPixelScale();
        int glWidth = (int) (getWidth() * Display.pixelScale[0] + .5);
        int glHeight = (int) (getHeight() * Display.pixelScale[1] + .5);
        if (glWidth != lastGlWidth || glHeight != lastGlHeight) {
            // A layout change is in flight and the native drawable has not been brought to the new
            // size yet. Drawing now would reshape GL to the new size against the old drawable, and
            // that one frame is exactly the stretch the user sees. Skip it; resyncHostDeferred will
            // resize the drawable and render immediately afterwards.
            if (hostResyncPending)
                return;
            GLRenderer.reshape(glWidth, glHeight);
            lastGlWidth = glWidth;
            lastGlHeight = glHeight;
        }
        angleRenderer.render(viewpoint);
        fpsCount++;
    }

    // Create the platform-native host/window handle and ANGLE renderer on first use.
    private void attachIfNeeded() {
        if (nativeWindowHandle != 0L || !isDisplayable() || getWidth() <= 0 || getHeight() <= 0)
            return;

        Rectangle bounds = hostBounds();
        long newHostHandle = 0L;
        long newNativeWindowHandle = 0L;
        try {
            if (Platform.isMacOS()) {
                MacAngleBridge.Host host = MacAngleBridge.create(this, bounds.x, bounds.y, bounds.width, bounds.height);
                if (host == null)
                    return;
                newHostHandle = host.handle();
                newNativeWindowHandle = host.layer();
            } else if (Platform.isWindows()) {
                newNativeWindowHandle = WinAngleBridge.hwnd(this);
            } else if (Platform.isLinux()) {
                newNativeWindowHandle = X11AngleBridge.drawable(this);
            }
            if (newNativeWindowHandle == 0L)
                return;

            AngleRenderer renderer = new AngleRenderer(newNativeWindowHandle);
            macHostHandle = newHostHandle;
            nativeWindowHandle = newNativeWindowHandle;
            angleRenderer = renderer;
            lastHostBounds = bounds;
            invalidateGlSize();
        } catch (RuntimeException | Error e) {
            if (newHostHandle != 0L)
                MacAngleBridge.destroy(newHostHandle);
            throw e;
        }
    }

    // Push the current AWT bounds to the native host, then trigger a redraw if needed.
    private void updateHostFrame(boolean renderNeeded, Position viewpoint) {
        if (getWidth() <= 0 || getHeight() <= 0)
            return;

        boolean pixelScaleChanged = refreshPixelScale();

        if (nativeWindowHandle == 0L) {
            attachIfNeeded();
            if (nativeWindowHandle == 0L)
                return;
        }
        if (angleRenderer == null)
            return;

        Rectangle bounds = hostBounds();
        if (Platform.isMacOS() && (!bounds.equals(lastHostBounds) || pixelScaleChanged))
            MacAngleBridge.setFrame(macHostHandle, bounds.x, bounds.y, bounds.width, bounds.height);
        lastHostBounds = bounds;
        if (renderNeeded || pixelScaleChanged || lastGlWidth < 0 || lastGlHeight < 0)
            requestRender(viewpoint);
    }

    // Coalesce host updates onto the EDT so move/resize bursts become one native update.
    private void scheduleHostUpdate(boolean renderNeeded, Position viewpoint) {
        hostRenderPending |= renderNeeded;
        if (hostUpdatePending || !isDisplayable())
            return;

        hostUpdatePending = true;
        EventQueue.invokeLater(() -> {
            hostUpdatePending = false;
            boolean render = hostRenderPending;
            hostRenderPending = false;
            updateHostFrame(render, viewpoint);
        });
    }

    private void scheduleHostUpdate(boolean renderNeeded) {
        scheduleHostUpdate(renderNeeded, GLRenderer.getDisplayedViewpoint());
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
                nativeWindowHandle = 0L;
                displayPending = hostUpdatePending = hostRenderPending = false;
                lastHostBounds = null;
                invalidateGlSize();
            }
        }
    }

    private void invalidateGlSize() {
        lastGlWidth = -1;
        lastGlHeight = -1;
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
        } catch (IllegalComponentStateException ignore) {}

        int x = 0;
        int y = 0;
        for (Component current = this; current != null && current != contentPane; current = current.getParent()) {
            x += current.getX();
            y += current.getY();
        }
        return new Rectangle(x, y, width, height);
    }
}
