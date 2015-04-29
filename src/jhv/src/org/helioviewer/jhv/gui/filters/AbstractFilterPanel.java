package org.helioviewer.jhv.gui.filters;

import org.helioviewer.viewmodel.view.AbstractView;

public abstract class AbstractFilterPanel {

    protected AbstractView jp2view;

    public void setJP2View(AbstractView jp2view) {
        if (this.jp2view != jp2view) {
            this.jp2view = jp2view;
        }
    }

}
