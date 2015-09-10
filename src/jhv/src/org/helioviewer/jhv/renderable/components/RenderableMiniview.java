package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;
import org.helioviewer.jhv.renderable.helpers.RenderableHelper;
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
            RenderableHelper.drawRectangle(gl, -30, -30, 60, 60);
            gl.glColor4d(1, 0, 0, 1);
            RenderableHelper.drawCircle(gl, 0, 0, 1, 100);
            gl.glColor4d(0, 1, 0, 0.2);
            View v = Layers.getActiveView();
            if (v != null) {
                MetaData m = v.getMetaData(new ImmutableDateTime(0));
                RenderableHelper.drawRectangle(gl, m.getPhysicalLowerLeft().x, m.getPhysicalLowerLeft().y, m.getPhysicalSize().x, m.getPhysicalSize().y);
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
