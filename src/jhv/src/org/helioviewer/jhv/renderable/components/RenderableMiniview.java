package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.Viewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.viewmodel.view.View;

import com.jogamp.opengl.GL2;

public class RenderableMiniview extends AbstractRenderable implements LayersListener {

    private final RenderableMiniviewOptionsPanel optionsPanel;

    private final Camera miniCamera = new Camera();
    private Viewport miniViewport = new Viewport(0, 0, 0, 100, 100, true);

    public RenderableMiniview() {
        Layers.addLayersListener(this);
        optionsPanel = new RenderableMiniviewOptionsPanel();
        setVisible(true);
    }

    public void createMiniViewport() {
        int vpw = Displayer.getGLWidth();
        int offset = (int) (vpw * 0.01);
        int size = (int) (vpw * 0.01 * optionsPanel.scale);

        miniViewport = new Viewport(0, offset, offset, size, size, true);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
    }

    public void renderBackground(Camera camera, Viewport vp, GL2 gl) {
        Mat4 cameraMatrix = camera.getOrientation().toMatrix();
        gl.glDepthRange(0, 0);
        gl.glPushMatrix();
        {
            gl.glMultMatrixd(cameraMatrix.transpose().m, 0);

            gl.glColor4f(0, 1, 0, 0.2f);
            GLHelper.drawRectangleFront(gl, -30, -30, 60, 60);

            gl.glColor4f(1, 0, 0, 0.2f);
            GLHelper.drawCircleFront(gl, 0, 0, 1, 100);
        }
        gl.glPopMatrix();
        gl.glDepthRange(0, 1);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Miniview";
    }

    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
    }

    @Override
    public void layerAdded(View view) {
        activeLayerChanged(view);
    }

    @Override
    public void activeLayerChanged(View view) {
        CameraHelper.zoomToFit(miniCamera);
    }

    public Viewport getViewport() {
        return miniViewport;
    }

    public Camera getCamera() {
        return miniCamera;
    }

}
