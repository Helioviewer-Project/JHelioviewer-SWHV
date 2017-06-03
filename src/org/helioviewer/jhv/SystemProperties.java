package org.helioviewer.jhv;

import org.helioviewer.jhv.log.Log;

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
        String javaArch = System.getProperty("sun.arch.data.model");
        System.setProperty("jhv.java.arch", javaArch);

        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        if (os != null && arch != null) {
            os = os.toLowerCase();
            arch = arch.toLowerCase();
            if (os.contains("windows")) {
                System.setProperty("jhv.os", "windows");
                if (arch.contains("64"))
                    System.setProperty("jhv.arch", "x86-64");
                else if (arch.contains("86"))
                    System.setProperty("jhv.arch", "x86-32");
                else {
                    Log.error("Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                }
            } else if (os.contains("linux")) {
                System.setProperty("jhv.os", "linux");
                if (arch.contains("64"))
                    System.setProperty("jhv.arch", "x86-64");
                else if (arch.contains("86"))
                    System.setProperty("jhv.arch", "x86-32");
                else {
                    Log.error("Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                }
            } else if (os.contains("mac os x")) {
                System.setProperty("jhv.os", "mac");
                if (arch.contains("ppc"))
                    System.setProperty("jhv.arch", "ppc");
                else if (arch.contains("64"))
                    System.setProperty("jhv.arch", "x86-64");
                else if (arch.contains("86"))
                    System.setProperty("jhv.arch", "x86-32");
                else {
                    Log.error("Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                }
            } else {
                Log.error("Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
            }
        } else {
            Log.error("Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
        }
    }

}
