package org.helioviewer.jhv;

import javax.swing.JOptionPane;

public class Platform {

    private static void die(String msg) {
        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(msg);
        optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        optionPane.setOptions(new String[]{"Quit JHelioviewer"});
        optionPane.createDialog(null, "JHelioviewer").setVisible(true);
        System.exit(1);
    }

    // Reads the builtin Java properties to determine the platform and set simplified properties used by JHV
    static void init() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        if (os == null || arch == null) {
            die("Could not determine platform. OS: " + os + " - arch: " + arch);
            return; // avoid warnings from static analysis
        }

        os = os.toLowerCase();
        arch = arch.toLowerCase();

        if (arch.contains("x86_64") || arch.contains("amd64"))
            jhvArch = "amd64";
       else if (arch.contains("aarch64"))
            jhvArch = "aarch64";
        else
            die("Please install Java 64-bit to run JHelioviewer.");

        if (os.contains("windows"))
            isWindows = true;
        else if (os.contains("linux"))
            isLinux = true;
        else if (os.contains("mac os"))
            isMacOS = true;
        else
            die("Could not determine platform. OS: " + os + " - arch: " + arch);
    }

    private static boolean isLinux = false;
    private static boolean isMacOS = false;
    private static boolean isWindows = false;
    private static String jhvArch;

    public static boolean isLinux() {
        return isLinux;
    }

    public static boolean isMacOS() {
        return isMacOS;
    }

    public static boolean isWindows() {
        return isWindows;
    }

    static String getArch() {
        return jhvArch;
    }

}
