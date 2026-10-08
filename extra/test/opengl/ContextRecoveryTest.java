package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;

import org.json.JSONObject;
import org.lwjgl.egl.EGL15;
import org.lwjgl.opengles.GLES;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class ContextRecoveryTest {
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        DisplayController.setRenderRequestHandler(_ -> {});

        Probe probe = new Probe();
        Layers.add(probe);
        AngleRenderer renderer = AngleRenderer.pbuffer(32, 32);
        try {
            for (int cycle = 0; cycle < 3; cycle++) {
                checkFrame(renderer);
                GLText.renderer();
                loseContext();
                renderer.destroy();
                renderer = null;
                check(EGL15.eglGetCurrentContext() == EGL15.EGL_NO_CONTEXT, "Lost context remains current");
                renderer = AngleRenderer.pbuffer(32, 32);
                checkFrame(renderer);
                GLText.renderer();
                check(probe.disposals == cycle + 1, "Retained layer was not disposed");
                check(probe.initializations == cycle + 2, "Retained layer was not reinitialized");
            }
            // Also exercise destruction when the healthy context must be rebound.
            long display = EGL15.eglGetCurrentDisplay();
            check(EGL15.eglMakeCurrent(display, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_SURFACE, EGL15.EGL_NO_CONTEXT), "Cannot unbind context");
        } finally {
            if (renderer != null)
                renderer.destroy();
        }
        System.out.println("ContextRecoveryTest passed");
    }

    private static void loseContext() {
        long request = GLES.getFunctionProvider().getFunctionAddress("glRequestExtensionANGLE");
        long lose = GLES.getFunctionProvider().getFunctionAddress("glLoseContextCHROMIUM");
        check(request != 0L && lose != 0L, "ANGLE context-loss injection is unavailable");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            JNI.callPV(MemoryUtil.memAddress(stack.ASCII("GL_CHROMIUM_lose_context")), request);
        }
        check(!GLException.checkErrors("Enable context-loss injection"), "Cannot enable context-loss injection");
        JNI.callV(0x8253, 0x8254, lose); // GL_GUILTY_CONTEXT_RESET, GL_INNOCENT_CONTEXT_RESET
        GL.glClear(GL.COLOR_BUFFER_BIT);
        check(GL.glGetError() == 0x0507, "Context-loss injection failed");
    }

    private static void checkFrame(AngleRenderer renderer) {
        GLRenderer.reshape(32, 32);
        renderer.render(Sun.StartEarth, false);
        ByteBuffer pixel = BufferUtils.newByteBuffer(4);
        GL.glReadPixels(16, 16, 1, 1, GL.RGBA, GL.UNSIGNED_BYTE, pixel);
        check((pixel.get(0) & 255) > 200 && pixel.get(1) == 0 && pixel.get(2) == 0, "Retained geometry or shader did not recover");
        check(!GLException.checkErrors("Recovered frame"), "Recovered frame has GL errors");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static final class Probe extends AbstractLayer {
        private final GLSLShape shape = new GLSLShape(false);
        private final BufVertex vertices = new BufVertex(3);
        private int initializations;
        private int disposals;

        Probe() {
            vertices.putVertex(-1, -1, 0, 1, Colors.Red.bytes());
            vertices.putVertex(1, -1, 0, 1, Colors.Red.bytes());
            vertices.putVertex(0, 1, 0, 1, Colors.Red.bytes());
        }

        @Override
        public void init() {
            shape.init();
            shape.upload(vertices);
            initializations++;
        }

        @Override
        public void dispose() {
            shape.dispose();
            disposals++;
        }

        @Override
        public void render(MapView mv, Viewport vp) {
            Transform.ortho2D(1, 2, 0, 0);
            GL.glDisable(GL.DEPTH_TEST);
            shape.renderShape(GL.TRIANGLES);
            GL.glEnable(GL.DEPTH_TEST);
        }

        @Override
        public void remove() {
            dispose();
        }

        @Override
        public String getName() {
            return "Recovery probe";
        }

        @Override
        public void serialize(JSONObject jo) {}
    }
}
