package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.net.URI;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.gui.dialogs.NewVersionDialog;

import okio.BufferedSource;

// Verbose whether a dialog box should be popped up
// A message box is shown in case of an update error
public class UpdateChecker {

    public static void check(boolean verbose) {
        new Thread(() -> {
            if (!verbose) {
                try {
                    int n = Integer.parseInt(Settings.getProperty("update.next"));
                    if (n > 0) {
                        n--;
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

            try (NetClient nc = NetClient.of(new URI(JHVGlobals.downloadURL + "VERSION")); BufferedSource source = nc.getSource()) {
                String version = source.readUtf8Line();
                if (version == null || version.isEmpty()) {
                    throw new Exception("Update Checker: Empty version string");
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
            } catch (Exception e) {
                Log.warn(e);
                if (verbose)
                    Message.warn("Update check error", "While checking for a newer version got " + e.getMessage());
            }
        }, "JHV-CheckUpdate").start();
    }

}
