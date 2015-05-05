package org.helioviewer.jhv.renderable;

import java.awt.Component;

import javax.swing.JPanel;

import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;

import com.jogamp.opengl.GL2;

public class RenderableDummy implements Renderable {

    private final RenderableType type;

    public RenderableDummy() {
        type = new RenderableType("Loading image layer");
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void render(GL2 gl) {
    }

    @Override
    public void remove(GL2 gl) {
    }

    @Override
    public RenderableType getType() {
        return null;
    }

    @Override
    public Component getOptionsPanel() {
        return new JPanel();
    }

    @Override
    public String getName() {
        return "Layer loading...";
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void setVisible(boolean b) {
    }

    @Override
    public String getTimeString() {
        return "N/A";
    }

}
