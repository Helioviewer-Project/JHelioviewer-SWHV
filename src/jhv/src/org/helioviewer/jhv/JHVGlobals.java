package org.helioviewer.jhv;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.helioviewer.base.logging.Log;

/**
 * Intended to be a class for static functions and fields relevant to the
 * application as a whole.
 *
 * @author caplins
 */
public class JHVGlobals {

    private static final String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "seamonkey", "galeon", "kazehakase", "mozilla", "netscape" };

    public static final String TEMP_FILENAME_DELETE_PLUGIN_FILES = "delete-plugins.tmp";

    private static String agent = "JHV/SWHV-";
    private static String name = "ESA JHelioviewer SWHV";
    private static String version = "";
    private static String revision = "";

    private JHVGlobals() {}

    public static final boolean GoForTheBroke = true;

    /**
     * @return standard read timeout
     */
    public static int getStdReadTimeout() {
        return Integer.parseInt(Settings.getSingletonInstance().getProperty("connection.read.timeout"));
    }

    /**
     * @return standard connect timeout
     */
    public static int getStdConnectTimeout() {
        return Integer.parseInt(Settings.getSingletonInstance().getProperty("connection.connect.timeout"));
    }

    /**
     * This function must be called prior to the first call to getJhvVersion and
     * getJhvRevision
     */
    public static void determineVersionAndRevision() {
        File jarPath;
        try {
            jarPath = new File(JHVGlobals.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Log.info(">> JHVGlobals.determineVersionAndRevision() > Look for jar file: " + jarPath.getAbsolutePath());
        } catch (URISyntaxException e1) {
            Log.error(">> JHVGlobals.determineVersionAndRevision() > Could not open code source location: " + JHVGlobals.class.getProtectionDomain().getCodeSource().getLocation().toString());
            Log.warn(">> JHVGlobals.determineVersionAndRevision() > Set version and revision to null.");
            return;
        }
        JarFile jarFile = null;
        if (jarPath.isFile()) {
            try {
                jarFile = new JarFile(jarPath);
                Manifest manifest = jarFile.getManifest();
                Attributes mainAttributes = manifest.getMainAttributes();

                version = mainAttributes.getValue("version");
                revision = mainAttributes.getValue("revision");
                agent += version + "." + revision;

                System.setProperty("jhv.version", version);
                System.setProperty("jhv.revision", revision);
            } catch (IOException e) {
                Log.error(">> JHVGlobals.determineVersionAndRevision() > Error while reading version and revision from manifest in jar file: " + jarFile.getName(), e);
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                        Log.error(">> JHVGlobals.determineVersionAndRevision() > Error while closing stream to jar file: " + jarFile.getName(), e);
                    }
                }
            }
        } else {
            Log.warn(">> JHVGlobals.determineVersionAndRevision() > Classes are not within a jar file. Set version and revision to null.");
        }
    }

    /**
     * Returns the version of JHelioviewer as found in the manifest file of the
     * jar archive
     *
     * @return the version or empty string if the classes are not within a jar archive
     *         or the manifest does not contain the version
     */
    public static String getJhvVersion() {
        return version;
    }

    /**
     * Returns the revision of JHelioviewer as found in the manifest file of the
     * jar archive
     *
     * @return the revision or empty string if the classes are not within a jar archive
     *         or the manifest does not contain the revision
     */
    public static String getJhvRevision() {
        return revision;
    }

    public static String getUserAgent() {
        return agent;
    }

    public static String getProgramName() {
        return name;
    }

    /**
     * Attempts to create the necessary directories if they do not exist. It
     * gets its list of directories to create from the JHVDirectory class.
     *
     * @throws SecurityException
     */
    public static void createDirs() throws SecurityException {
        JHVDirectory[] dirs = JHVDirectory.values();
        for (JHVDirectory dir : dirs) {
            File f = dir.getFile();
            if (!f.exists()) {
                f.mkdirs();
            }
        }
    }

    /**
     * Opens the specified web page in the default web browser
     *
     * @param url
     *            A web address (URL) of a web page (e.g
     *            "http://www.jhelioviewer.org/")
     */
    public static void openURL(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
