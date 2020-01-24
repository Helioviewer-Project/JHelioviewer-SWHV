package org.helioviewer.jhv;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.dialogs.TextDialog;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.log.Log;

import com.jidesoft.comparator.AlphanumComparator;

public class JHVGlobals {

    public static final String programName = "ESA JHelioviewer";
    public static final String downloadURL = "http://swhv.oma.be/download/";
    public static final String documentationURL = "http://swhv.oma.be/user_manual/";
    public static final String emailAddress = "swhv@oma.be";
    public static String version = "2.-1.-1";
    public static String revision = "-1";
    public static String userAgent = "JHV/SWHV-";
    public static String versionDetail = "";

    public static final AlphanumComparator alphanumComparator = new AlphanumComparator(true);

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
        try (InputStream is = JHVGlobals.class.getResourceAsStream("/version.properties")) {
            Properties p = new Properties();
            p.load(is);
            p.stringPropertyNames().forEach(key -> System.setProperty(key, p.getProperty(key)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        String v = System.getProperty("jhv.version");
        String r = System.getProperty("jhv.revision");
        version = v == null ? version : v;
        revision = r == null ? revision : r;

        userAgent += version + '.' + revision + " (" +
                System.getProperty("os.arch") + ' ' + System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ") " +
                System.getProperty("java.vendor") + " JRE " + System.getProperty("java.version");
        versionDetail = String.format("%s %.1fGB %dCPU", userAgent, Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024.), Runtime.getRuntime().availableProcessors());
        Log.info(versionDetail);
    }

    // Attempts to create the necessary directories if they do not exist
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
            libCacheDir = FileUtils.tempDir(cacheDir, "lib").getAbsolutePath();
            dataCacheDir = FileUtils.tempDir(cacheDir, "data").getAbsolutePath();
            fileCacheDir = FileUtils.tempDir(cacheDir, "file");
            clientCacheDir = FileUtils.tempDir(cacheDir, "client");
            exportCacheDir = FileUtils.tempDir(cacheDir, "export");
        } catch (Exception e) {
            String cacheDir = System.getProperty("java.io.tmpdir");
            libCacheDir = cacheDir;
            dataCacheDir = cacheDir;
            fileCacheDir = new File(cacheDir);
            clientCacheDir = new File(cacheDir);
            exportCacheDir = new File(cacheDir);
        }
    }

    public static String libCacheDir;
    public static String dataCacheDir;
    public static File fileCacheDir;
    public static File clientCacheDir;
    public static File exportCacheDir;

    public static final HyperOpenURL hyperOpenURL = new HyperOpenURL();

    private static class HyperOpenURL implements HyperlinkListener {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
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
