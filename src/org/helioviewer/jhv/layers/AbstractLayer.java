package org.helioviewer.jhv.layers;

import java.util.Arrays;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;

import com.jogamp.opengl.GL2;

public abstract class AbstractLayer implements Layer {

    protected boolean enabled;
    protected final boolean[] isVisible = {false, false, false, false, false, false}; // match max of Display.viewports.length

    @Override
    public void setEnabled(boolean _enabled) {
        enabled = _enabled;
        Arrays.fill(isVisible, enabled);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setVisible(int j) {
        Arrays.fill(isVisible, false);
        if (j >= 0 && j < isVisible.length)
            isVisible[j] = true;
    }

    @Override
    public boolean isVisible(int idx) {
        return isVisible[idx];
    }

    @Override
    public int isVisibleIdx() {
        for (int i = 0; i < isVisible.length; i++) {
            if (isVisible[i])
                return i;
        }
        return -1;
    }

    @Override
    public boolean isDownloading() {
        return false;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void prerender(GL2 gl) {
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void renderMiniview(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
    }

}
