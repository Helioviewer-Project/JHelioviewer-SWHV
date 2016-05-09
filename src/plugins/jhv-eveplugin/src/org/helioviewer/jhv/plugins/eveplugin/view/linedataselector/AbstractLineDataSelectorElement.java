package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;

public abstract class AbstractLineDataSelectorElement implements LineDataSelectorElement {
    private boolean highlighted = false;
    protected boolean isVisible = true;

    @Override
    public void setHighlighted(boolean _highlighted) {
        highlighted = _highlighted;
    }

    @Override
    public boolean isHighlighted() {
        return highlighted;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisibility(boolean visible) {
        isVisible = visible;
        EVEPlugin.dc.fireRedrawRequest();
    }
}
