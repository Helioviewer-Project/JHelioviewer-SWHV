package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;
import org.helioviewer.jhv.renderable.viewport.GL3DViewport;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.View;

import com.jogamp.opengl.GL2;

public class RenderableMiniview implements Renderable, LayersListener {

    private final RenderableType type;
    private boolean isVisible = true;
    RenderableMiniviewOptionsPanel optionsPanel;

    public RenderableMiniview(RenderableMiniviewType renderableMiniviewType) {
        type = renderableMiniviewType;
        Layers.addLayersListener(this);
        optionsPanel = new RenderableMiniviewOptionsPanel();
    }

    private void drawCircle(GL2 gl, double x, double y, double r, int segments) {
        gl.glDisable(GL2.GL_TEXTURE_2D);
        {
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex2d(x, y);
            for (int n = 0; n <= segments; ++n) {
                double t = 2 * Math.PI * n / segments;
                gl.glVertex2d(x + Math.sin(t) * r, y + Math.cos(t) * r);
            }
            gl.glEnd();
        }
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    private void drawRectangle(GL2 gl, double x0, double y0, double w, double h) {
        double x1 = x0 + w;
        double y1 = y0 + h;
        gl.glDisable(GL2.GL_TEXTURE_2D);
        {
            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex2d(x0, -y0);
            gl.glVertex2d(x0, -y1);
            gl.glVertex2d(x1, -y1);
            gl.glVertex2d(x1, -y0);
            gl.glEnd();
        }
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
    }

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
        GL3DMat4d cameraMatrix = vp.getCamera().getLocalRotation().toMatrix();
        gl.glDepthRange(0.f, 0.f);
        gl.glPushMatrix();
        {
            gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
            gl.glColor4d(0, 0, 0, 1);
            drawRectangle(gl, -30, -30, 60, 60);
            gl.glColor4d(1, 0, 0, 1);
            drawCircle(gl, 0, 0, 1, 100);
            gl.glColor4d(0, 1, 0, 0.2);
            View v = Layers.getActiveView();
            if (v != null) {
                MetaData m = v.getMetaData(new ImmutableDateTime(0));
                drawRectangle(gl, m.getPhysicalLowerLeft().x, m.getPhysicalLowerLeft().y, m.getPhysicalSize().x, m.getPhysicalSize().y);
            }
        }
        gl.glPopMatrix();
        gl.glDepthRange(0.f, 1.f);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public RenderableType getType() {
        return type;
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
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
        Displayer.getMiniview().setVisible(isVisible);
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
    public boolean isActiveImageLayer() {
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
            Displayer.getMiniview().getCamera().zoomToFit(view);
    }

}
