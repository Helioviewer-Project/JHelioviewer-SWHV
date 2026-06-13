package org.helioviewer.jhv.app;

public class Platform {

    // Reads the builtin Java properties to determine the platform and set simplified properties used by JHV
    public static void init() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        if (os == null || arch == null)
            throw new IllegalStateException("Could not determine platform. OS: " + os + " - arch: " + arch);

        os = os.toLowerCase();
        arch = arch.toLowerCase();

        if (arch.contains("x86_64") || arch.contains("amd64"))
            jhvArch = "amd64";
        else if (arch.contains("aarch64"))
            jhvArch = "aarch64";
        else
            throw new IllegalStateException("Please install Java 64-bit to run JHelioviewer.");

        if (os.contains("windows"))
            isWindows = true;
        else if (os.contains("mac os"))
            isMacOS = true;
        else if (os.contains("linux"))
            isLinux = true;
        else
            throw new IllegalStateException("Could not determine platform. OS: " + os + " - arch: " + arch);

        resourceDir = buildResourceDir();
    }

    private static boolean isLinux = false;
    private static boolean isMacOS = false;
    private static boolean isWindows = false;
    private static String jhvArch;
    private static String resourceDir;

    public static boolean isLinux() {
        return isLinux;
    }

    public static boolean isMacOS() {
        return isMacOS;
    }

    public static boolean isWindows() {
        return isWindows;
    }

    public static String getResourceDir() {
        return resourceDir;
    }

    private static String buildResourceDir() {
        String prefix = "/jhv/";
        if (isMacOS) {
            if ("amd64".equals(jhvArch))
                return prefix + "macos-amd64/";
            else if ("aarch64".equals(jhvArch))
                return prefix + "macos-arm64/";
        } else if (isWindows && "amd64".equals(jhvArch)) {
            return prefix + "windows-amd64/";
        } else if (isLinux && "amd64".equals(jhvArch)) {
            return prefix + "linux-amd64/";
        }
        return prefix;
    }

}
