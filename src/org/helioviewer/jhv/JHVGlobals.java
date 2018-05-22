package org.helioviewer.jhv;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.dialogs.TextDialog;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVExecutor;

import com.jidesoft.comparator.AlphanumComparator;

public class JHVGlobals {

    public static final String programName = "ESA JHelioviewer";
    public static final String downloadURL = "http://swhv.oma.be/download/";
    public static final String documentationURL = "http://swhv.oma.be/user_manual/";
    public static final String emailAddress = "swhv@oma.be";
    public static String version = "2.-1.-1";
    public static String revision = "-1";
    public static String userAgent = "JHV/SWHV-";

    public static final AlphanumComparator alphanumComparator = new AlphanumComparator(true);

    public static final int hiDpiCutoff = 1024;

    private static final ExecutorService executorService = JHVExecutor.createJHVWorkersExecutorService("MAIN", 10);
    private static final ScheduledExecutorService reaperService = JHVExecutor.createReaperService();

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static ScheduledExecutorService getReaperService() {
        return reaperService;
    }

    private static int readTimeout = -1;
    private static int connectTimeout = -1;

    public static int getReadTimeout() {
        if (readTimeout == -1)
            readTimeout = Integer.parseInt(Settings.getProperty("timeout.read"));
        return readTimeout;
    }

    public static int getConnectTimeout() {
        if (connectTimeout == -1)
            connectTimeout = Integer.parseInt(Settings.getProperty("timeout.connect"));
        return connectTimeout;
    }

    public static void determineVersionAndRevision() {
        File jarPath;
        try {
            jarPath = new File(JHVGlobals.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e1) {
            Log.error("JHVGlobals.determineVersionAndRevision > Could not open code source location: " + JHVGlobals.class.getProtectionDomain().getCodeSource().getLocation());
            Log.warn("JHVGlobals.determineVersionAndRevision > Set version and revision to null.");
            return;
        }

        if (jarPath.isFile()) {
            try (JarFile jarFile = new JarFile(jarPath)) {
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    Log.warn("JHVGlobals.determineVersionAndRevision > Manifest not found in jar file: " + jarPath + ". Set version and revision to null.");
                    return;
                }

                Attributes mainAttributes = manifest.getMainAttributes();
                version = mainAttributes.getValue("version");
                revision = mainAttributes.getValue("revision");
                userAgent += version + '.' + revision + " (" +
                         System.getProperty("os.arch") + ' ' + System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ") " +
                         System.getProperty("java.vendor") + " JRE " + System.getProperty("java.version");

                System.setProperty("jhv.version", version);
                System.setProperty("jhv.revision", revision);
                Log.info("Running " + userAgent);
            } catch (IOException e) {
                Log.error("JHVGlobals.determineVersionAndRevision > Error while reading version and revision from manifest in jar file: " + jarPath, e);
            }
        } else {
            Log.warn("JHVGlobals.determineVersionAndRevision > Classes are not within a jar file. Set version and revision to null.");
        }
    }

    /**
     * Attempts to create the necessary directories if they do not exist. It
     * gets its list of directories to create from the JHVDirectory class.
     *
     * @throws SecurityException
     */
    public static void createDirs() {
        JHVDirectory[] dirs = JHVDirectory.values();
        for (JHVDirectory dir : dirs) {
            File f = dir.getFile();
            if (!f.exists()) {
                f.mkdirs();
            }
        }

        try {
            File cacheDir = JHVDirectory.CACHE.getFile();
            libCacheDir = FileUtils.tempDir(cacheDir, "lib");
            fileCacheDir = FileUtils.tempDir(cacheDir, "file");
            clientCacheDir = FileUtils.tempDir(cacheDir, "client");
            exportCacheDir = FileUtils.tempDir(cacheDir, "export");
        } catch (Exception e) {
            String cacheDir = System.getProperty("java.io.tmpdir");
            libCacheDir = new File(cacheDir);
            fileCacheDir = new File(cacheDir);
            clientCacheDir = new File(cacheDir);
            exportCacheDir = new File(cacheDir);
        }
    }

    public static File libCacheDir;
    public static File fileCacheDir;
    public static File clientCacheDir;
    public static File exportCacheDir;
    public static final File jpipStreamCacheDir = new File(JHVDirectory.CACHE.getFile(), "JPIPStream");
    public static final File jpipLevelCacheDir = new File(JHVDirectory.CACHE.getFile(), "JPIPLevel");

    public static final HyperOpenURL hyperOpenURL = new HyperOpenURL();

    private static class HyperOpenURL implements HyperlinkListener  {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                openURL(e.getURL().toString());
            }
        }

    }

    public static void openURL(String url) {
        try {
            if (UIGlobals.canBrowse && url != null)
                Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void displayNotification(String moviePath) {
        String openURL = new File(moviePath).toURI().toString();

        if (System.getProperty("jhv.os").equals("mac")) {
            try {
                String msg = "File " + moviePath + " is ready.";
                File jarParent = new File(JHVGlobals.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalFile().getParentFile();
                if (jarParent != null) {
                    String[] cmd = {
                        jarParent.getParent() + "/Helpers/terminal-notifier.app/Contents/MacOS/terminal-notifier",
                        "-message", '"' + msg + '"',
                        "-execute", "open " + '"' + openURL + '"',
                        "-title", "JHelioviewer"
                    };
                    Log.info("JHVGlobals.displayNotification " + Arrays.toString(cmd));
                    Runtime.getRuntime().exec(cmd);
                    return;
                }
            } catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                Log.error("JHVGlobals.displayNotification " + errors);
            }
        }
        // otherwise
        new TextDialog("Ready", "File <a href=\"" + openURL + "\">" + moviePath + "</a> is ready.", false).showDialog();
    }

}
