package org.helioviewer.jhv.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.JavaCompatibility;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JavaHelioViewer;
import org.helioviewer.jhv.JavaHelioViewerLauncher;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.resourceloader.SystemProperties;
import org.helioviewer.plugins.eveplugin.EVEPlugin;
import org.helioviewer.plugins.eveplugin.EVEPluginLauncher;
import org.helioviewer.viewmodelplugin.controller.PluginManager;

/**
 * Class for launching jhv with several plugins added.
 * 
 * @author Bram.Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWHVLauncher {
    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("JHelioviewer developer version with external plugin compiled-in.");
        System.out.println("================================================================\n\n");

        loadLibs();

        String[] args2 = JavaCompatibility.copyArrayString(args, args.length + 6);

        args2[args2.length - 2] = "--deactivate-plugin";
        args2[args2.length - 1] = "SWEKPlugin.jar";
        args2[args2.length - 4] = "--deactivate-plugin";
        args2[args2.length - 3] = "HEKPlugin.jar";
        args2[args2.length - 6] = "--deactivate-plugin";
        args2[args2.length - 5] = "EVEPlugin.jar";

        JavaHelioViewer.main(args2, new SWEKPlugin(false));

        PluginManager.getSingeltonInstance().addPlugin(EVEPluginLauncher.class.getClassLoader(), new EVEPlugin(), null);
    }

    private static void loadLibs() {
        SystemProperties.setPlatform();
        String libpath = JHVDirectory.LIBS.getPath().substring(0, JHVDirectory.LIBS.getPath().length() - 1) + "/";
        System.out.println(libpath);
        String libs[] = new String[4];// ["","",""];
        String pathlib = "";
        if (System.getProperty("jhv.os").equals("mac")) {
            libs[0] = "libgluegen-rt.jnilib";
            libs[1] = "libnativewindow_awt.jnilib";
            libs[2] = "libnativewindow_macosx.jnilib";
            libs[3] = "libjogl_desktop.jnilib";
            pathlib = "macosx-universal/";
        } else if (System.getProperty("jhv.os").equals("windows") && System.getProperty("jhv.arch").equals("x86-64")) {
            libs[0] = "libgluegen-rt.dll";
            libs[1] = "libnativewindow_awt.dll";
            libs[2] = "libnativewindow_win32.dll";
            libs[3] = "libjogl_desktop.dll";
            pathlib = "windows-amd64/";
        } else if (System.getProperty("jhv.os").equals("windows") && System.getProperty("jhv.arch").equals("x86-32")) {
            libs[0] = "libgluegen-rt.dll";
            libs[1] = "libnativewindow_awt.dll";
            libs[2] = "libnativewindow_win32.dll";
            libs[3] = "libjogl_desktop.dll";
            pathlib = "windows-i586/";
        } else if (System.getProperty("jhv.os").equals("linux") && System.getProperty("jhv.arch").equals("x86-64")) {
            libs[0] = "libgluegen-rt.so";
            libs[1] = "libnativewindow_awt.so";
            libs[2] = "libnativewindow_x11.so";
            libs[3] = "libjogl_desktop.so";
            pathlib = "linux-amd64/";
        }
        // if (System.getProperty("jhv.os").equals("linux") &&
        // System.getProperty("jhv.arch").equals("x86-32")) {
        else {
            libs[0] = "libgluegen-rt.so";
            libs[1] = "libnativewindow_awt.so";
            libs[2] = "libnativewindow_x11.so";
            libs[3] = "libjogl_desktop.so";
            pathlib = "linux-i586/";
        }
        for (int i = 0; i < libs.length; i++) {
            try {
                InputStream in = JavaHelioViewerLauncher.class.getResourceAsStream("/resources/jogl/lib/" + pathlib + libs[i]);
                File fileOut = new File(libpath + libs[i]);
                OutputStream out = new FileOutputStream(fileOut);
                FileUtils.copy(in, out);
                in.close();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
