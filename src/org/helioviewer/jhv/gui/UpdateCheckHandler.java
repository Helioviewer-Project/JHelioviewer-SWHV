package org.helioviewer.jhv.gui;

import java.awt.EventQueue;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.Message;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.dialogs.NewVersionDialog;
import org.helioviewer.jhv.io.UpdateChecker;

final class UpdateCheckHandler implements UpdateChecker.Handler {

    @Override
    public void updateAvailable(String message, boolean verbose) {
        EventQueue.invokeLater(() -> {
            NewVersionDialog dialog = new NewVersionDialog(message, verbose);
            dialog.showDialog();
            if (!verbose)
                Settings.setProperty("update.next", Integer.toString(dialog.getNextCheck()));
        });
    }

    @Override
    public void upToDate(String message) {
        EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(null, message));
    }

    @Override
    public void failed(String message, boolean verbose) {
        if (verbose)
            Message.warn("Update check error", message);
    }

}
