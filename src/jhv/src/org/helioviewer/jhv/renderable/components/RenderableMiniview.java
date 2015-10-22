package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.jhv.base.math.GL3DMat4d;
import org.helioviewer.jhv.camera.GL3DObserverCamera;
import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.viewmodel.view.View;

import com.jogamp.opengl.GL2;

public class RenderableMiniview extends AbstractRenderable implements LayersListener {

    private final RenderableMiniviewOptionsPanel optionsPanel;

    private final GL3DViewport miniview = new GL3DViewport(0, 0, 0, 100, 100, new GL3DObserverCamera(), true);

    public RenderableMiniview() {
        Layers.addLayersListener(this);
        optionsPanel = new RenderableMiniviewOptionsPanel();
        setVisible(true);
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
    }

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
        GL3DMat4d cameraMatrix = vp.getCamera().getLocalRotation().toMatrix();
        gl.glDepthRange(0, 0);
        gl.glPushMatrix();
        {
            gl.glMultMatrixd(cameraMatrix.transpose().m, 0);

            gl.glColor4f(0, 1, 0, 0.2f);
            GLHelper.drawRectangle(gl, -30, -30, 60, 60);

            gl.glColor4f(1, 0, 0, 0.2f);
            GLHelper.drawCircle(gl, 0, 0, 1, 100);
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
        if (view != null)
            miniview.getCamera().zoomToFit();
    }

    public GL3DViewport getViewport() {
        int vpw = Displayer.getGLWidth();
        int offset = (int) (vpw * 0.01);
        int size = (int) (vpw * optionsPanel.scale * 0.01);
        miniview.setSize(offset, offset, size, size);

        return miniview;
    }

    @Override
    public boolean isVisible(int i) {
        return false;
    }

    @Override
    public boolean isVisible() {
        return miniview.isVisible();
    }

    @Override
    public void setVisible(boolean _isVisible) {
        miniview.setVisible(_isVisible);
    }

    @Override
    public void setVisible(int j) {
    }

}
