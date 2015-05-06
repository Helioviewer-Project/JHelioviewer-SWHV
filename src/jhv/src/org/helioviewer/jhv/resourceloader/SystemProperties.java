package org.helioviewer.jhv.resourceloader;

import org.helioviewer.base.logging.Log;

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

}
