package org.helioviewer.gl3d.plugin.pfss;

import org.helioviewer.base.JavaCompatibility;
import org.helioviewer.jhv.JavaHelioViewer;
import org.helioviewer.jhv.JavaHelioViewerLauncher;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;

/**
 * Class for testing the external plugin
 *
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 */
public class PfssPluginLauncher {

    /**
     * Used for testing the plugin
     *
     * @param args
     */
    public static void main(String[] args) {

        System.out.println("================================================================");
        System.out.println("JHelioviewer developer version with external plugin compiled-in.");
        System.out.println("================================================================\n\n");

        String[] args2 = JavaCompatibility.copyArrayString(args, args.length + 8);

        args2[args2.length - 2] = "--deactivate-plugin";
        args2[args2.length - 1] = "SWEKPlugin.jar";
        args2[args2.length - 4] = "--deactivate-plugin";
        args2[args2.length - 3] = "EVEPlugin.jar";
        args2[args2.length - 5] = "--deactivate-plugin";
        args2[args2.length - 6] = "PFSSPlugin.jar";
        args2[args2.length - 7] = "--deactivate-plugin";
        args2[args2.length - 8] = "PfssPlugin.jar";
        JavaHelioViewerLauncher.loadLibs();
        JavaHelioViewer.main(args2, new PfssPlugin(true));
    }
}
