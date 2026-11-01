package org.helioviewer.jhv.gui.dialog;

import org.helioviewer.jhv.ui.AspiicsScreen;

@SuppressWarnings("serial")
public final class AspiicsDialog extends ScreenDialog {

    private static AspiicsDialog instance;

    public static AspiicsDialog getInstance() {
        return instance == null ? instance = new AspiicsDialog() : instance;
    }

    private AspiicsDialog() {
        super(new AspiicsScreen(), true);
    }

}
