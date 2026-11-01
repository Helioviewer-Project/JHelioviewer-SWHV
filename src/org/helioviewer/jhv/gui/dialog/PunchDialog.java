package org.helioviewer.jhv.gui.dialog;

import org.helioviewer.jhv.ui.PunchScreen;

@SuppressWarnings("serial")
public final class PunchDialog extends ScreenDialog {

    private static PunchDialog instance;

    public static PunchDialog getInstance() {
        return instance == null ? instance = new PunchDialog() : instance;
    }

    private PunchDialog() {
        super(new PunchScreen(), true);
    }

}
