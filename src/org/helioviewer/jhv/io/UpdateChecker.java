package org.helioviewer.jhv.io;

import java.net.URI;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.threads.JHVThread;

import okio.BufferedSource;

// Verbose whether a dialog box should be popped up
// A message box is shown in case of an update error
public class UpdateChecker {

    public interface Handler {
        void updateAvailable(String message, boolean verbose);

        void upToDate(String message);

        void failed(String message, boolean verbose);
    }

    private static Handler handler = new LogHandler();

    public static void setHandler(Handler _handler) {
        handler = _handler;
    }

    public static void check(boolean verbose) {
        JHVThread.create(() -> {
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

                String runningVersion = JHVGlobals.version + '.' + JHVGlobals.revision;
                if (JHVGlobals.alphanumComparator.compare(version, runningVersion) > 0) {
                    Log.info("Found newer version " + version);
                    handler.updateAvailable(updateAvailableMessage(version, runningVersion), verbose);
                } else if (verbose) {
                    handler.upToDate(upToDateMessage(runningVersion));
                }
            } catch (Exception e) {
                Log.warn(e);
                handler.failed(failedMessage(e), verbose);
            }
        }, "JHV-CheckUpdate").start();
    }

    public static String updateAvailableMessage(String version, String runningVersion) {
        return "JHelioviewer " + version + " is now available (you have " + runningVersion + ").";
    }

    public static String upToDateMessage(String runningVersion) {
        return "You are running the latest JHelioviewer version (" + runningVersion + ')';
    }

    public static String failedMessage(Exception e) {
        return "While checking for a newer version got " + e.getMessage();
    }

    private static final class LogHandler implements Handler {
        @Override
        public void updateAvailable(String message, boolean verbose) {
            Log.info(message);
        }

        @Override
        public void upToDate(String message) {
            Log.info(message);
        }

        @Override
        public void failed(String message, boolean verbose) {}
    }

}
