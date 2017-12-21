package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.helioviewer.jhv.base.ProxySettings;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.plugin.PluginManager;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.io.CommandLine;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.log.LogSettings;
import org.helioviewer.jhv.metadata.AIAResponse;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.samp.SampPlugin;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.view.jp2view.kakadu.KakaduMessageSystem;

import nom.tam.fits.FitsFactory;

public class JHelioviewer {

    public static void main(String[] args) {
        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLine.getUsageMessage());
            return;
        }

        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();
        // Save command line arguments
        CommandLine.setArguments(args);
        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());
        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Save current default locale to user.locale
        System.setProperty("user.locale", Locale.getDefault().toString());
        // Per default, the us locale should be used
        Locale.setDefault(Locale.US);

        // Init log
        LogSettings.init("/settings/log4j.properties", JHVDirectory.LOGS.getPath());
        // Information log message
        StringBuilder argString = new StringBuilder();
        for (String arg : args) {
            argString.append(' ').append(arg);
        }
        Log.info("JHelioviewer started with command-line options:" + argString);

        // This attempts to create the necessary directories for the application
        JHVGlobals.createDirs();
        // Read the version and revision from the JAR metafile
        JHVGlobals.determineVersionAndRevision();
        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been initialized
        Settings.getSingletonInstance().load();
        // Set the platform system properties
        SystemProperties.setPlatform();
        System.setProperty("newt.window.icons", "null,null");

        FitsFactory.setUseHierarch(true);

        try {
            JHVLoader.loadKDULibs();
            KakaduMessageSystem.startKduMessageSystem();
        } catch (Exception e) {
            Message.err("Failed to setup Kakadu", e.getMessage(), true);
            return;
        }

        ProxySettings.init();
        try {
            AIAResponse.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        EventQueue.invokeLater(() -> {
            UIGlobals.setUIFont(UIGlobals.UIFont);

            Log.info("Start main window");
            ExitHooks.attach();
            JFrame frame = ImageViewerGui.prepareGui();

            PluginManager.getSingletonInstance().loadSettings(JHVDirectory.PLUGINS.getPath());
            try {
                if (args.length != 0 && args[0].equals("--exclude-plugins")) {
                    Log.info("Do not load plugins");
                } else {
                    Log.info("Load bundled plugins");
                      PluginManager.getSingletonInstance().addPlugin(new EVEPlugin(), "Eve");
                      PluginManager.getSingletonInstance().addPlugin(new SWEKPlugin(), "SWEK");
                      PluginManager.getSingletonInstance().addPlugin(new PfssPlugin(), "PFSS");
                      PluginManager.getSingletonInstance().addPlugin(new SampPlugin(), "SAMP");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // set left pane width to fit the viewpoint options
            ImageViewerGui.getLayersPanel().setOptionsPanel(Layers.getViewpointLayer()); // nasty
            frame.pack();
            JComponent leftPane = ImageViewerGui.getLeftScrollPane();
            leftPane.setMinimumSize(new Dimension(leftPane.getPreferredSize().width, -1));
            ImageViewerGui.getLayersPanel().setOptionsPanel(Layers.getGridLayer()); // nasty

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            DataSources.loadSources();
            CommandLine.load();

            try {
                JHVUpdate update = new JHVUpdate(false);
                update.check();
            } catch (Exception e) {
                // Should never happen
                Log.error("Error retrieving update URL", e);
            }
        });
    }

}
