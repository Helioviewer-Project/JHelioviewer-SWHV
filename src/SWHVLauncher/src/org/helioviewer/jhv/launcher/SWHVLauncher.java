package org.helioviewer.jhv.launcher;

import java.awt.EventQueue;

import org.helioviewer.jhv.JavaHelioViewer;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.plugins.swhvhekplugin.SWHVHEKPlugin;
import org.helioviewer.plugins.eveplugin.EVEPlugin;
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

        String[] args2 = new String[7];

        args2[0] = "--remote-plugins";
        args2[1] = "--deactivate-plugin";
        args2[2] = "SWEKPlugin.jar";
        args2[3] = "--deactivate-plugin";
        args2[4] = "EVEPlugin.jar";
        args2[5] = "--deactivate-plugin";
        args2[6] = "SWHVHEKPlugin.jar";
        args2[5] = "--deactivate-plugin";
        args2[6] = "PfssPlugin.jar";
        JavaHelioViewer.main(args);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                PluginManager.getSingletonInstance().addPlugin(EVEPlugin.class.getClassLoader(), new EVEPlugin(), null);
                PluginManager.getSingletonInstance().addPlugin(SWEKPlugin.class.getClassLoader(), new SWEKPlugin(false), null);
                PluginManager.getSingletonInstance().addPlugin(SWHVHEKPlugin.class.getClassLoader(), new SWHVHEKPlugin(), null);
                PluginManager.getSingletonInstance().addPlugin(SWHVHEKPlugin.class.getClassLoader(), new PfssPlugin(), null);
            }
        });
    }

}
