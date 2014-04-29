package org.helioviewer.gl3d.plugin.pfss;

import org.helioviewer.base.JavaCompatibility;
import org.helioviewer.jhv.JavaHelioViewer;
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

        String[] args2 = JavaCompatibility.copyArrayString(args, args.length + 2);
        args2[args2.length - 2] = "--deactivate-plugin";
        args2[args2.length - 1] = "PfssPlugin.jar";
        JavaHelioViewer.main(args2, new PfssPlugin(true));
    }
}
