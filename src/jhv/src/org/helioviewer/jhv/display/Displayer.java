package org.helioviewer.jhv.display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

public class Displayer implements JHVEventHighlightListener {

    public static enum DisplayMode {
        ORTHO, POLAR, LATITUDINAL, LOGPOLAR;

        public String getLabel() {
            String label;
            switch (this) {
            case ORTHO:
                label = "Orthographic";
                break;
            case POLAR:
                label = "Polar";
                break;
            case LATITUDINAL:
                label = "Latitudinal";
                break;
            case LOGPOLAR:
                label = "LogPolar";
                break;
            default:
                label = "";
            }
            return label;
        }

        public GLSLSolarShader getSolarShader() {
            switch (this) {
            case ORTHO:
                return GLSLSolarShader.ortho;
            case POLAR:
                return GLSLSolarShader.polar;
            case LATITUDINAL:
                return GLSLSolarShader.lati;
            case LOGPOLAR:
                return GLSLSolarShader.logpolar;
            default:
                return null;
            }
        }

        public void setGridScale() {
            switch (this) {
            case ORTHO:
                GridScale.current = GridScale.ortho;
                break;
            case POLAR:
                GridScale.current = GridScale.polar;
                break;
            case LATITUDINAL:
                GridScale.current = GridScale.latitudinal;
                break;
            case LOGPOLAR:
                GridScale.current = GridScale.logpolar;
                break;
            default:
                GridScale.current = null;
            }
        }

    }

    public static DisplayMode mode = DisplayMode.ORTHO;
    public static boolean multiview = false;

    public static void setMode(DisplayMode newMode) {
        mode = newMode;

        CameraHelper.zoomToFit(ImageViewerGui.getRenderableMiniview().getCamera());
        //ImageViewerGui.getRenderableMiniview().getCamera().reset();

        Displayer.getCamera().reset();
    }

    static int glWidth = 1;
    static int glHeight = 1;

    public static void setGLSize(int x, int y, int w, int h) {
        glWidth = w;
        glHeight = h;
        fullViewport = new Viewport(-1, x, y, w, h);
    }

    private static Camera camera = new Camera();

    public static Camera getCamera() {
        return camera;
    }

    private static Viewport[] viewports = { new Viewport(0, 0, 0, 100, 100), null, null, null };
    private static int activeViewport = 0;

    public static Viewport fullViewport = new Viewport(-1, 0, 0, 100, 100);

    public static void setActiveViewport(int x, int y) {
        if (multiview) {
            for (int i = 0; i < viewports.length; ++i) {
                Viewport vp = viewports[i];
                if (vp != null && vp.contains(x, y)) {
                    activeViewport = i;
                    break;
                }
            }
        }
    }

    public static Viewport getActiveViewport() {
        return viewports[activeViewport];
    }

    public static Viewport[] getViewports() {
        return viewports;
    }

    private static int countActiveLayers() {
        int ct = 0;
        if (multiview) {
            for (int i = 0; i < viewports.length; ++i) {
                if (ImageViewerGui.getRenderableContainer().getViewportRenderableImageLayer(i) != null)
                    ct++;
            }
        }
        return ct;
    }

    public static void reshapeAll() {
        activeViewport = 0;

        int ct = countActiveLayers();
        switch (ct) {
        case 2:
            reshape2();
            break;
        case 3:
            reshape3();
            break;
        case 4:
            reshape4();
            break;
        default:
            reshape();
            break;
        }
    }

    private static void reshape() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(0, 0, 0, w, h);
        viewports[1] = null;
        viewports[2] = null;
        viewports[3] = null;
    }

    private static void reshape2() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(0, 0, 0, w / 2, h);
        viewports[1] = new Viewport(1, w / 2, 0, w / 2, h);
        viewports[2] = null;
        viewports[3] = null;
    }

    private static void reshape3() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(0, 0, 0, w / 2, h / 2);
        viewports[1] = new Viewport(1, w / 2, 0, w / 2, h / 2);
        viewports[2] = new Viewport(2, 0, h / 2, w, h / 2);
        viewports[3] = null;
    }

    private static void reshape4() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(0, 0, 0, w / 2, h / 2);
        viewports[1] = new Viewport(1, w / 2, 0, w / 2, h / 2);
        viewports[2] = new Viewport(2, 0, h / 2, w / 2, h / 2);
        viewports[3] = new Viewport(3, w / 2, h / 2, w / 2, h / 2);
    }

    private static double renderFactor = -1;
    private static boolean toDisplay = false;

    private static final Timer displayTimer = new Timer(1000 / 60, new DisplayTimerListener());

    private Displayer() {
        displayTimer.setCoalesce(true);
        displayTimer.start();
    }

    public static void render(double f) {
        if (Layers.getActiveView() == null)
            toDisplay = true;
        else
            renderFactor = f;
    }

    public static void display() {
        toDisplay = true;
    }

    private static class DisplayTimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (toDisplay == true) {
                ImageViewerGui.getMainComponent().repaint();
                toDisplay = false;
            }
            if (renderFactor != -1) {
                ImageViewerGui.getRenderableContainer().setRender(camera, renderFactor);
                renderFactor = -1;
            }
        }
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        display();
    }

    private static final Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

}
