package org.helioviewer.jhv;

import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.gui.dialogs.NewVersionDialog;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;

import okio.BufferedSource;

// Verbose whether a dialog box should be popped up.
// Otherwise a message box is shown in case of an update error.
public record JHVUpdate(boolean verbose) implements Runnable {

    public void check() {
        new Thread(this, "JHV Update Checker").start();
    }

    @Override
    public void run() {
        if (!verbose) {
            try {
                int n = Integer.parseInt(Settings.getProperty("update.next"));
                if (n > 0) {
                    n -= 1;
                    Settings.setProperty("update.next", Integer.toString(n));
                }
                if (n != 0) {
                    Log.info("Update check suspended for this startup");
                    return;
                }
            } catch (NumberFormatException e) {
                Log.error("Invalid update setting", e);
                Settings.setProperty("update.next", Integer.toString(0));
            }
        }

        try (NetClient nc = NetClient.of(JHVGlobals.downloadURL + "VERSION"); BufferedSource source = nc.getSource()) {
            String version = source.readUtf8Line();
            if (version == null || version.isEmpty()) {
                throw new IOException("JHVUpdate: Empty version string");
            }

            EventQueue.invokeLater(() -> {
                String runningVersion = JHVGlobals.version + '.' + JHVGlobals.revision;
                if (JHVGlobals.alphanumComparator.compare(version, runningVersion) > 0) {
                    Log.info("Found newer version " + version);

                    NewVersionDialog dialog = new NewVersionDialog("JHelioviewer " + version + " is now available (you have " + runningVersion + ").", verbose);
                    dialog.showDialog();
                    if (!verbose) {
                        Settings.setProperty("update.next", Integer.toString(dialog.getNextCheck()));
                    }
                } else {
                    if (verbose)
                        JOptionPane.showMessageDialog(null, "You are running the latest JHelioviewer version (" + runningVersion + ')');
                }
            });
        } catch (IOException e) {
            Log.error("Error retrieving update server", e);
            if (verbose)
                Message.warn("Update check error", "While checking for a newer version got " + e.getLocalizedMessage());
        }
    }

}
