package org.helioviewer.jhv.gui.dialog;

import org.helioviewer.jhv.ui.SettingsScreen;

@SuppressWarnings("serial")
public final class SettingsDialog extends ScreenDialog {

    private static SettingsDialog instance;

    public static SettingsDialog getInstance() {
        return instance == null ? instance = new SettingsDialog() : instance;
    }

    private SettingsDialog() {
        super(new SettingsScreen(() -> {}, () -> {}), true);
    }

    // The title-bar X saves nothing; the fields keep the drafts for the next opening.
    @Override
    protected void closeWindow() {
        dispose();
    }

}
