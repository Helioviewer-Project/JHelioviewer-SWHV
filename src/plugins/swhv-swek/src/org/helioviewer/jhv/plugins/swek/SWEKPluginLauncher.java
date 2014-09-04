/**
 *
 */
package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.base.JavaCompatibility;
import org.helioviewer.jhv.JavaHelioViewer;

/**
 * @author bramb
 *
 */
public class SWEKPluginLauncher {

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("JHelioviewer developer version with external plugin compiled-in.");
        System.out.println("================================================================\n\n");

        String[] args2 = JavaCompatibility.copyArrayString(args, args.length + 4);

        args2[args2.length - 2] = "--deactivate-plugin";
        args2[args2.length - 1] = "SWEKPlugin.jar";
        args2[args2.length - 4] = "--deactivate-plugin";
        args2[args2.length - 3] = "HEKPlugin.jar";
        JavaHelioViewer.main(args2, new SWEKPlugin());
    }

}
