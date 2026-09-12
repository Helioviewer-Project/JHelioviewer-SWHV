package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;

import org.json.JSONObject;
import org.lwjgl.opengles.GLES30;

public final class GLGrabRenderingTest {

    public static void main(String[] args) throws Exception {
        System.setProperty("user.timezone", TimeZone.getDefault().getID());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();

        AngleRenderer renderer = AngleRenderer.pbuffer(320, 180);
        Probe probe = new Probe();
        try {
            checkCapture();
            Layers.add(probe);
            GLRenderer.reshape(320, 180);
            for (MapMode mode : MapMode.values()) {
                Display.mode = mode;
                renderer.render(Sun.StartEarth);
                MapView screenView = GLRenderer.getMapView();
                for (int[] size : new int[][]{{256, 256}, {180, 320}, {320, 180}}) {
                    GLGrab grabber = new GLGrab(size[0], size[1]);
                    probe.captured = false;
                    probe.exporting = true;
                    try {
                        ByteBuffer pixels = ByteBuffer.allocate(3 * size[0] * size[1]);
                        grabber.renderFrame(pixels);
                        check(probe.captured, "Export did not render the probe layer");
                        check(pixels.remaining() == pixels.capacity(), "Incomplete pixel readback");
                        check(GLRenderer.getMapView() == screenView, "Export replaced the screen MapView");
                        check(Display.fullViewport.width == 320 && Display.fullViewport.height == 180, "Screen dimensions were not restored");
                        GLException.checkErrors("GLGrabRenderingTest " + mode);
                    } finally {
                        probe.exporting = false;
                        grabber.dispose();
                    }
                }
            }
        } finally {
            renderer.destroy();
        }
        System.out.println("GLGrabRenderingTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static void checkCapture() {
        int texture = GL.glGenTexture();
        GL.glActiveTexture(GL.TEXTURE0 + GLTexture.Unit.THREE.ordinal());
        GL.glBindTexture(GL.TEXTURE_2D, texture);
        GLFrameCapture capture = null;
        try {
            capture = new GLFrameCapture(7, 5);
            check(GLES30.glGetInteger(GLES30.GL_ACTIVE_TEXTURE) == GL.TEXTURE0 + GLTexture.Unit.THREE.ordinal(), "Capture changed the active texture unit");
            check(GLES30.glGetInteger(GLES30.GL_TEXTURE_BINDING_2D) == texture, "Capture replaced the texture binding");
            capture.bindForRender();
            GL.glClearColor(1, 0, 0, 1);
            GL.glClear(GL.COLOR_BUFFER_BIT);
            GLES30.glEnable(GLES30.GL_SCISSOR_TEST);
            GLES30.glScissor(0, 0, 7, 2);
            GL.glClearColor(0, 0, 1, 1);
            GL.glClear(GL.COLOR_BUFFER_BIT);
            GLES30.glDisable(GLES30.GL_SCISSOR_TEST);

            ByteBuffer pixels = ByteBuffer.allocate(7 * 5 * 3);
            capture.readPixels(pixels);
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 7; x++) {
                    check((pixels.get() & 255) == (y < 2 ? 0 : 255), "Incorrect red channel");
                    check(pixels.get() == 0, "Incorrect green channel");
                    check((pixels.get() & 255) == (y < 2 ? 255 : 0), "Incorrect blue channel or row order");
                }
            }
            GLException.checkErrors("Capture RGB readback");
        } finally {
            GLES30.glDisable(GLES30.GL_SCISSOR_TEST);
            GL.glBindFramebuffer(GL.FRAMEBUFFER, 0);
            if (capture != null)
                capture.dispose();
            GL.glDeleteTexture(texture);
        }
    }

    private static final class Probe extends AbstractLayer {
        boolean exporting;
        boolean captured;

        @Override
        public void render(MapView mv, Viewport vp) {
            if (!exporting)
                return;
            captured = true;
            check(mv.viewpoint() == GLRenderer.getDisplayedViewpoint(), "Export changed the displayed viewpoint");
            if (mv.mode() == MapMode.HPC) {
                MapScale scale = mv.scale(vp);
                double width = scale.toMapX(1) - scale.toMapX(0);
                double height = scale.toMapY(1) - scale.toMapY(0);
                check(Math.abs(width / height - vp.aspect) < 1e-12, "HPC export reused the screen aspect ratio");
            }
        }

        @Override public void renderScale(MapView mv, Viewport vp) { render(mv, vp); }
        @Override public void init() {}
        @Override public void dispose() {}
        @Override public void remove() { Layers.remove(this); }
        @Override public String getName() { return "Export scale probe"; }
        @Override public void serialize(JSONObject jo) {}
    }
}
