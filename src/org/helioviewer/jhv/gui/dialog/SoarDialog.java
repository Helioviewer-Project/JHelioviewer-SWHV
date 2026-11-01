package org.helioviewer.jhv.gui.dialog;

import org.helioviewer.jhv.ui.SoarScreen;

@SuppressWarnings("serial")
public final class SoarDialog extends ScreenDialog {

    private static SoarDialog instance;

    public static SoarDialog getInstance() {
        return instance == null ? instance = new SoarDialog() : instance;
    }

    private SoarDialog() {
        super(new SoarScreen(), true);
    }

}
