package org.helioviewer.jhv.gui.filters;

import javax.swing.JPanel;

import org.helioviewer.viewmodel.view.AbstractImageInfoView;

public abstract class AbstractFilterPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    protected AbstractImageInfoView jp2view;

    public void setJP2View(AbstractImageInfoView jp2view) {
        if (this.jp2view != jp2view) {
            setEnabled(jp2view == null);
            this.jp2view = jp2view;
        }
    }
}
