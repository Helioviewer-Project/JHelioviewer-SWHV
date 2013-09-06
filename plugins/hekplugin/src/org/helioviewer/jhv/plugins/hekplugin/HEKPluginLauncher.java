package org.helioviewer.jhv.plugins.hekplugin;

import org.helioviewer.base.JavaCompatibility;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.JavaHelioViewer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.ViewchainFactory;
import org.helioviewer.viewmodelplugin.controller.PluginManager;

/**
 * Class for testing the external plugin
 * 
 * @author Andre Dau
 * 
 */
public class HEKPluginLauncher {

    /**
     * Used for testing the plugin
     * 
     * @see org.helioviewer.plugins.hekplugin.hekplugin#main(String[])
     * @param args
     */
    public static void main(String[] args) {

        System.out.println("================================================================");
        System.out.println("JHelioviewer developer version with external plugin compiled-in.");
        System.out.println("================================================================\n\n");

        String[] args2 = JavaCompatibility.copyArrayString(args, args.length + 2);

        args2[args2.length - 2] = "--deactivate-plugin";
        args2[args2.length - 1] = "HEKPlugin.jar";
        JavaHelioViewer.main(args2, new HEKPlugin(true));

    }
}
