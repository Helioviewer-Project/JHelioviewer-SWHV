package org.helioviewer.jhv.launcher;

import java.awt.EventQueue;
import java.util.Arrays;

import org.helioviewer.jhv.JavaHelioViewer;
import org.helioviewer.jhv.JavaHelioViewerLauncher;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.plugins.swhvhekplugin.SWHVHEKPlugin;
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

        JavaHelioViewerLauncher.loadLibs();

        String[] args2 = Arrays.copyOf(args, args.length + 4);

        args2[args2.length - 2] = "--deactivate-plugin";
        args2[args2.length - 1] = "SWEKPlugin.jar";
        args2[args2.length - 4] = "--deactivate-plugin";
        args2[args2.length - 3] = "EVEPlugin.jar";

        JavaHelioViewer.main(args2, new SWEKPlugin(false));

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                PluginManager.getSingletonInstance().addPlugin(EVEPluginLauncher.class.getClassLoader(), new EVEPlugin(), null);
                PluginManager.getSingletonInstance().addPlugin(SWHVHEKPlugin.class.getClassLoader(), new SWHVHEKPlugin(), null);
            }
        });
    }

}
