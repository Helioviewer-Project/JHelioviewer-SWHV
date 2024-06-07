package org.helioviewer.jhv.layers;

import java.util.Arrays;

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

}
