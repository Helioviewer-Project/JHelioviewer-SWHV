package org.helioviewer.jhv.timelines.view.linedataselector;

import org.helioviewer.jhv.timelines.draw.DrawController;

public abstract class AbstractLineDataSelectorElement implements LineDataSelectorElement {

    protected boolean isVisible = true;

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisibility(boolean visible) {
        isVisible = visible;
        DrawController.fireRedrawRequest();
    }

}
