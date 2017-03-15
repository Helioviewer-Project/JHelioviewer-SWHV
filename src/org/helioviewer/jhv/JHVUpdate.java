package org.helioviewer.jhv;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.gui.dialogs.NewVersionDialog;

/**
 * Class to test in a new thread if there is a newer version of JHelioviewer
 * released and shows a message.
 *
 * After construction the code is available in run(), ie as a Runnable object.
 * To start in parallel use check().
 *
 * If verbose is false, ie. when called during startup, the property
 * update.check.next is used to suspend the checks: - If it is negative, the
 * update check is suspended forever - If it is 0, the update check is done - If
 * it is positive, it is decremented and then checked if 0
 *
 * For further version this gives much room for improvement: - automated
 * download - ... ?
 *
 * @author Helge Dietert
 */
public class JHVUpdate implements Runnable {
    /**
     * Determines whether to show a message box if already the latest version is
     * running and if a message box is shown in case of an error.
     *
     * Also it determines whether the properties update.check.* are used to
     * suspend the checks.
     */
    private final boolean verbose;

    /**
     * Verbose whether a dialog box should be popped up.
     * Otherwise a message box is shown in case of an update error.
     */
    public JHVUpdate(boolean _verbose) {
        verbose = _verbose;
    }

    /**
     * Checks for update in a new thread
     */
    public void check() {
        Thread t = new Thread(this, "JHV Update Checker");
        t.start();
    }

    /**
     * Checks for update and show a dialog box
     */
    public void run() {
        if (!verbose) {
            try {
                int n = Integer.parseInt(Settings.getSingletonInstance().getProperty("update.check.next"));
                if (n > 0) {
                    n -= 1;
                    Settings.getSingletonInstance().setProperty("update.check.next", Integer.toString(n));
                    Settings.getSingletonInstance().save("update.check.next");
                }
                if (n != 0) {
                    Log.info("Update check suspended for this startup");
                    return;
                }
            } catch (NumberFormatException e) {
                Log.error("Invalid update setting", e);
                Settings.getSingletonInstance().setProperty("update.check.next", Integer.toString(0));
            }
        }

        Log.trace("Start checking for updates");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new DownloadStream(JHVGlobals.downloadURL + "VERSION").getInput(), StandardCharsets.UTF_8))) {
            String version = in.readLine();
            if (version == null || version.isEmpty()) {
                throw new IOException("JHVUpdate: Empty version string");
            }
           // String message = in.readLine(); - extra

            EventQueue.invokeLater(() -> {
                String runningVersion = JHVGlobals.version + '.' + JHVGlobals.revision;
                if (JHVGlobals.alphanumComparator.compare(version, runningVersion) > 0) {
                    Log.info("Found newer version " + version);

                    NewVersionDialog dialog = new NewVersionDialog("JHelioviewer " + version + " is now available (you have " + runningVersion + ").", verbose);
                    dialog.showDialog();
                    if (!verbose) {
                        Settings.getSingletonInstance().setProperty("update.check.next", Integer.toString(dialog.getNextCheck()));
                        Settings.getSingletonInstance().save("update.check.next");
                    }
                } else {
                    Log.info("Running the newest version of JHelioviewer");
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
