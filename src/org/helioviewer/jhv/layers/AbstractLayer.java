package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;

import com.jogamp.opengl.GL2;

public abstract class AbstractLayer implements Layer {

    protected boolean enabled;
    protected final boolean[] isVisible = {false, false, false, false};

    @Override
    public void setEnabled(boolean _enabled) {
        enabled = _enabled;
        // ugly but keeps enabled in sync with options panel
        if (JHVFrame.getLayersPanel() != null)
            JHVFrame.getLayersPanel().setOptionsPanel(this);
        for (int i = 0; i < isVisible.length; i++)
            isVisible[i] = _enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setVisible(int j) {
        for (int i = 0; i < isVisible.length; i++)
            isVisible[i] = false;
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
