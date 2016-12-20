package org.helioviewer.jhv.launcher;

import java.awt.EventQueue;

import org.helioviewer.jhv.JHelioviewer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;

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
        JHelioviewer.main(args2);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                PluginManager.getSingletonInstance().addPlugin(new EVEPlugin(), null);
                PluginManager.getSingletonInstance().addPlugin(new SWEKPlugin(), null);
                PluginManager.getSingletonInstance().addPlugin(new PfssPlugin(), null);
            }
        });
    }

}
