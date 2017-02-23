package org.helioviewer.jhv.renderable.gui;

import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.jetbrains.annotations.NotNull;

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
    public int isVisibleIdx() {
        for (int i = 0; i < this.isVisible.length; i++) {
            if (this.isVisible[i])
                return i;
        }
        return -1;
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
    public boolean isDownloading() {
        return false;
    }

    @Override
    public void renderScale(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl, @NotNull GLSLSolarShader shader, @NotNull GridScale scale) {
    }

    @Override
    public void prerender(@NotNull GL2 gl) {
    }

    @Override
    public void renderFloat(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl) {
    }

    @Override
    public void renderMiniview(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl) {
    }

    @Override
    public void renderFullFloat(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl) {
    }

}
