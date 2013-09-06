package org.helioviewer.jhv;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.JOptionPane;
import org.apache.log4j.Level;
import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;

/**
 * Intended to be a class for static functions and fields relevant to the
 * application as a whole.
 * 
 * @author caplins
 */
public class JHVGlobals {

    /** The maximum amount of memory the JVM will use for the heap. */
    public static final long MAX_JVM_HEAP_SIZE = Runtime.getRuntime().maxMemory();

    /** The the maximum amount of memory the BufferManager object will use. */
    public static final long MAX_BUFFER_MANAGER_SIZE = (MAX_JVM_HEAP_SIZE * 8) / 10;

    public static final String GLibVersionTool = "glibc-version";
    public static final String ffmpeg = "ffmpeg";
    public static final String cgc = "cgc";

    private static final String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "seamonkey", "galeon", "kazehakase", "mozilla", "netscape" };

    public static final String TEMP_FILENAME_DELETE_PLUGIN_FILES = "delete-plugins.tmp";

    private static String version = null;

    private static String revision = null;

    /** Constructor is private to prevent instantiation. */
    private JHVGlobals() {

    }

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
     * @return the version or null if the classes are not within a jar archive
     *         or the manifest does not contain the version
     */
    public static String getJhvVersion() {
        return version;
    }

    /**
     * Returns the revision of JHelioviewer as found in the manifest file of the
     * jar archive
     * 
     * @return the revision or null if the classes are not within a jar archive
     *         or the manifest does not contain the revision
     */
    public static String getJhvRevision() {
        return revision;
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
        Log.info("Opening URL " + url);
        String functionCall = "openURL(" + url + ")";
        String functionCallEntry = ">> " + functionCall;
        Log.trace(">> " + functionCall);

        try { // attempt to use Desktop library from JDK 1.6+ (even if on 1.5)
            Log.debug(functionCallEntry + " > Try to use java.awt.Desktop class from JDK 1.6+");
            Class<?> d = Class.forName("java.awt.Desktop");
            d.getDeclaredMethod("browse", new Class[] { java.net.URI.class }).invoke(d.getDeclaredMethod("getDesktop").invoke(null), new Object[] { java.net.URI.create(url) });
        } catch (Exception ignore) { // library not available or failed
            Log.debug(functionCallEntry + " > Loading class java.awt.Desktop failed. Trying other methods to open URL.");
            String osName = System.getProperty("os.name");
            Log.trace(functionCallEntry + " > OS: " + osName);
            try {
                if (osName.startsWith("Mac OS")) {
                    Log.debug(functionCallEntry + " > Open URL assuming MacOS");
                    Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                    Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
                    openURL.invoke(null, new Object[] { url });

                } else if (osName.startsWith("Windows")) {
                    Log.debug(functionCallEntry + " > Open URL assuming Windows");
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else { // assume Unix or Linux
                    Log.debug(functionCallEntry + " > Open URL assuming Unix");
                    boolean found = false;
                    for (String browser : browsers) {
                        if (!found) {
                            Process p = Runtime.getRuntime().exec(new String[] { "which", browser });
                            FileUtils.logProcessOutput(p, "which", Level.DEBUG, true);
                            found = p.waitFor() == 0;
                            if (found) {
                                p = Runtime.getRuntime().exec(new String[] { browser, url });
                                FileUtils.logProcessOutput(p, browser, Level.DEBUG, false);
                            }
                        }
                    }
                    if (!found) {
                        throw new Exception(Arrays.toString(browsers));
                    }
                }
            } catch (Exception e) {
                Log.error("Error attempting to launch web browser", e);
                JOptionPane.showMessageDialog(null, "Error attempting to launch web browser\n" + e.toString());
            }
        }
        Log.trace("<< " + functionCall);
    }

}
