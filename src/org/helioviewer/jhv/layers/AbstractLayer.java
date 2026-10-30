package org.helioviewer.jhv.layers;

public abstract class AbstractLayer implements Layer {

    protected boolean enabled;

    @Override
    public void setEnabled(boolean _enabled) {
        enabled = _enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
