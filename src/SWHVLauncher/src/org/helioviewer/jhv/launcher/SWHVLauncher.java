package org.helioviewer.jhv.launcher;

import java.awt.EventQueue;

import org.helioviewer.jhv.JavaHelioViewer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.plugins.swhvhekplugin.SWHVHEKPlugin;

/**
 * Class for launching jhv with several plugins added.
 *
 * @author Bram.Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWHVLauncher {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("JHelioviewer developer version with external plugins compiled-in.");
        System.out.println("=================================================================\n\n");

        String[] args2 = new String[] { "--exclude-plugins" };
        JavaHelioViewer.main(args2);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                PluginManager.getSingletonInstance().addPlugin(EVEPlugin.class.getClassLoader(), new EVEPlugin(), null);
                PluginManager.getSingletonInstance().addPlugin(SWEKPlugin.class.getClassLoader(), new SWEKPlugin(false), null);
                PluginManager.getSingletonInstance().addPlugin(SWHVHEKPlugin.class.getClassLoader(), new SWHVHEKPlugin(), null);
                PluginManager.getSingletonInstance().addPlugin(PfssPlugin.class.getClassLoader(), new PfssPlugin(), null);
            }
        });
    }

}
