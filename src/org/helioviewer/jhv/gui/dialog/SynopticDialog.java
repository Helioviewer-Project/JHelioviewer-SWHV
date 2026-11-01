package org.helioviewer.jhv.gui.dialog;

import org.helioviewer.jhv.ui.SynopticScreen;

@SuppressWarnings("serial")
public final class SynopticDialog extends ScreenDialog {

    private static SynopticDialog instance;

    public static SynopticDialog getInstance() {
        return instance == null ? instance = new SynopticDialog() : instance;
    }

    private SynopticDialog() {
        super(new SynopticScreen(), true);
    }

}
