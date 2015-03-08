package org.helioviewer.jhv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
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
     * File address to check for updates
     */
    private final URL updateURL;
    /**
     * Determines whether to show a message box if already the latest version is
     * running and if a message box is shown in case of an error.
     * 
     * Also it determines whether the properties update.check.* are used to
     * suspend the checks.
     */
    private boolean verbose;

    /**
     * Constructs a new update object which is not verbose
     * 
     * @throws MalformedURLException
     *             Error while parsing the internal update URL
     */
    public JHVUpdate() throws MalformedURLException {
        updateURL = new URL("http://www.jhelioviewer.org/updateJHV.txt");
        verbose = false;
    }

    /**
     * Checks for update in a new thread
     */
    public void check() {
        Thread t = new Thread(this, "JHV Update Checker");
        t.start();
    }

    /**
     * Checks for update in a new thread, when called from the menu
     */
    public void checkMenu() {
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
                    Settings.getSingletonInstance().save();
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
        Log.trace("Start cheching for updates");
        String runningVersion = JHVGlobals.getJhvVersion();
        if (runningVersion == null) {
            Log.error("Could not retrieve version information");
            if (verbose) {
                Message.warn("Unknown running version", "Version information is not included");
            }
            return;
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new DownloadStream(updateURL, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout()).getInput()));
            String version = in.readLine();
            if (version == null || version.equals("")) {
                throw new IOException("JHVUpdate: Empty version string");
            }
            if (version.compareTo(runningVersion) > 0) {
                String message = in.readLine();
                Log.info("Found newer version " + version);
                NewVersionDialog d = new NewVersionDialog(version, message, verbose);
                d.showDialog();
                if (!verbose) {
                    Settings.getSingletonInstance().setProperty("update.check.next", Integer.toString(d.getNextCheck()));
                    Settings.getSingletonInstance().save();
                }
            } else {
                Log.info("Running the newest version of JHelioviewer");
                if (verbose)
                    JOptionPane.showMessageDialog(null, "You are running the latest JHelioviewer version (" + runningVersion + ")");
            }
            in.close();
        } catch (IOException e) {
            Log.error("Error retrieving update server", e);
            if (verbose)
                Message.warn("Update check error", "While checking for a newer version got " + e.getLocalizedMessage());
        }
    }

    /**
     * Sets if there should pop up a output anyway. Otherwise only in case of an
     * update a message box is shown
     * 
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

}
