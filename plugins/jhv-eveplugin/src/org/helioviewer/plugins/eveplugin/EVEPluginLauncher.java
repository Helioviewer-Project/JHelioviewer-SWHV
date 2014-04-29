package org.helioviewer.plugins.eveplugin;

import org.helioviewer.base.JavaCompatibility;
import org.helioviewer.jhv.JavaHelioViewer;
import org.helioviewer.viewmodelplugin.controller.PluginManager;

/**
 * Class for testing the external plugin
 * 
 * @author Andre Dau
 * 
 */
public class EVEPluginLauncher {

    /**
     * Used for testing the plugin
     * 
     * @see org.helioviewer.plugins.eveplugin.EVEPlugin#main(String[])
     * @param args
     */
    public static void main(String[] args) {
        String args2[] = JavaCompatibility.copyArray(args, args.length + 2);

        args2[args2.length - 2] = "--deactivate-plugin";
        args2[args2.length - 1] = "EVEPlugin.jar";

        JavaHelioViewer.main(args2);
        PluginManager.getSingeltonInstance().addPlugin(EVEPluginLauncher.class.getClassLoader(), new EVEPlugin(), null);
    }
}
