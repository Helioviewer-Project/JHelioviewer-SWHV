package org.helioviewer.jhv.renderable.gui;

import org.helioviewer.jhv.camera.GL3DViewport;

import com.jogamp.opengl.GL2;

public abstract class AbstractRenderable implements Renderable {

    protected final boolean[] isVisible = { false, false, false, false };

    @Override
    public boolean isVisible(int i) {
        return isVisible[i];
    }

    @Override
    public boolean isVisible() {
        for (int i = 0; i < this.isVisible.length; i++) {
            if (this.isVisible[i])
                return true;
        }
        return false;
    }

    @Override
    public void setVisible(boolean isVisible) {
        for (int i = 0; i < this.isVisible.length; i++) {
            this.isVisible[i] = isVisible;
        }
    }

    @Override
    public void setVisible(int j) {
        for (int i = 0; i < isVisible.length; i++) {
            this.isVisible[i] = false;
        }
        if (j >= 0 && j < this.isVisible.length)
            this.isVisible[j] = true;
    }

    @Override
    public void prerender(GL2 gl) {
    }

    @Override
    public void renderFloat(GL2 gl, GL3DViewport vp) {
    }

}
