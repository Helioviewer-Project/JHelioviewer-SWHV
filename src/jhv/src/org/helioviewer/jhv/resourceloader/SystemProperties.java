package org.helioviewer.jhv.resourceloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.JHVGlobals;

/**
 * Helper class to set platform dependent system properties for example in order
 * to determine compatible resource configurations in resource definitions
 * files.
 * 
 * @author Andre Dau
 * 
 */
public class SystemProperties {

    /**
     * Reads the builtin Java properties to determine the platform and set
     * simplified properties used by JHelioviewer.
     */

    public static void setPlatform() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        String javaArch = System.getProperty("sun.arch.data.model");

        System.setProperty("jhv.java.arch", javaArch);

        if (os != null && arch != null) {
            os = os.toLowerCase();
            arch = arch.toLowerCase();
            if (os.indexOf("windows") != -1) {
                System.setProperty("jhv.os", "windows");
                if (arch.indexOf("64") != -1)
                    System.setProperty("jhv.arch", "x86-64");
                else if (arch.indexOf("86") != -1)
                    System.setProperty("jhv.arch", "x86-32");
                else {
                    Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                }
            } else if (os.indexOf("linux") != -1) {
                System.setProperty("jhv.os", "linux");
                if (arch.indexOf("64") != -1)
                    System.setProperty("jhv.arch", "x86-64");
                else if (arch.indexOf("86") != -1)
                    System.setProperty("jhv.arch", "x86-32");
                else {
                    Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                }
            } else if (os.indexOf("mac os x") != -1) {
                System.setProperty("jhv.os", "mac");
                if (arch.indexOf("ppc") != -1)
                    System.setProperty("jhv.arch", "ppc");
                else if (arch.indexOf("64") != -1)
                    System.setProperty("jhv.arch", "x86-64");
                else if (arch.indexOf("86") != -1)
                    System.setProperty("jhv.arch", "x86-32");
                else {
                    Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                }
            } else {
                Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
            }
        } else {
            Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
        }
    }

    /**
     * Determine the glibc version if possible and store the version in the
     * system properties.
     * 
     * @return The glibc version or null if it could not be determined
     */
    public static String setGLibcVersion() {
        BufferedReader in = null;
        InputStream err = null;
        try {
            if (System.getProperty("jhv.os").equals("linux")) {
                Process p = FileUtils.invokeExecutable(JHVGlobals.GLibVersionTool, null);
                in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                err = p.getErrorStream();
                String version = in.readLine();
                System.setProperty("glibc.version", version);
                in.close();
                err.close();
                return version;
            }
        } catch (IOException e) {
            Log.error(">> SystemProperties.setGLibcVersion() > Could not determine glibc version", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.error(">> SystemProperties.setGLibcVersion() > Could not close stdout of glibc-version tool", e);
                }
            }
            if (err != null) {
                try {
                    err.close();
                } catch (IOException e) {
                    Log.error(">> SystemProperties.setGLibcVersion() > Could not close stderr of glibc-version tool", e);
                }
            }
        }
        return null;
    }
}
