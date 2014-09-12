package org.helioviewer.jhv.launcher;

import org.helioviewer.base.JavaCompatibility;
import org.helioviewer.jhv.JavaHelioViewer;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
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
}
