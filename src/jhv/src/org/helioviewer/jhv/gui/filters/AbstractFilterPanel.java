package org.helioviewer.jhv.gui.filters;

import javax.swing.JPanel;

import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

public abstract class AbstractFilterPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    protected JHVJP2View jp2view;

    public void setJP2View(JHVJP2View jp2view) {
        if (this.jp2view != jp2view) {
            setEnabled(jp2view == null);
            this.jp2view = jp2view;
        }
    }
}
