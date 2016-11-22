package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JComponent;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.logging.LogSettings;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.LoadStartup;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduMessageSystem;

// Main
public class JavaHelioViewer {

    public static void main(String[] args) {
        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLineProcessor.getUsageMessage());
            return;
        }
        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();

        // Save command line arguments
        CommandLineProcessor.setArguments(args);

        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());

        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Save current default locale to user.locale
        System.setProperty("user.locale", Locale.getDefault().toString());

        // Per default, the us locale should be used
        Locale.setDefault(Locale.US);

        // init log
        LogSettings logSettings = new LogSettings("/settings/log4j.initial.properties", JHVDirectory.SETTINGS.getPath() + "log4j.properties", JHVDirectory.LOGS.getPath());

        // Information log message
        StringBuilder argString = new StringBuilder();
        for (String arg : args) {
            argString.append(' ').append(arg);
        }
        Log.info("JHelioviewer started with command-line options:" + argString);

        // This attempts to create the necessary directories for the application
        JHVGlobals.createDirs();

        // Save the log settings. Must be done AFTER the directories are created
        logSettings.update();

        // Read the version and revision from the JAR metafile
        JHVGlobals.determineVersionAndRevision();

        Log.info("Initializing JHelioviewer");
        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been initialized
        Settings.getSingletonInstance().load(false);

        // Set the platform system properties
        SystemProperties.setPlatform();
        Log.info("OS: " + System.getProperty("jhv.os") + " - arch: " + System.getProperty("jhv.arch") + " - java arch: " + System.getProperty("jhv.java.arch"));

        System.setProperty("newt.window.icons", "null,null");

        Log.debug("Instantiate Kakadu engine");
        try {
            JHVLoader.copyKDULibs();
            KakaduMessageSystem.startKduMessageSystem();
        } catch (Exception e) {
            Message.err("Failed to setup Kakadu", e.getMessage(), true);
            return;
        }

        FileUtils.deleteDir(JHVDirectory.PLUGINSCACHE.getFile());
        JHVDirectory.PLUGINSCACHE.getFile().mkdirs();
        EventQueue.invokeLater(() -> {
            UIGlobals.setUIFont(UIGlobals.UIFont);

            Log.info("Start main window");
            ExitHooks.attach();
            ImageViewerGui.prepareGui();

            DataSources.loadSources();

            Log.info("Load plugin settings");
            PluginManager.getSingletonInstance().loadSettings(JHVDirectory.PLUGINS.getPath());

            try {
                if (args.length != 0 && args[0].equals("--exclude-plugins")) {
                    Log.info("Do not load plugins");
                } else {
                    Log.info("Load bundled plugins");
                    JHVLoader.loadBundledPlugin("EVEPlugin.jar");
                    JHVLoader.loadBundledPlugin("SWEKPlugin.jar");
                    JHVLoader.loadBundledPlugin("PfssPlugin.jar");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // after loading plugins fix the minimum width of left pane
            JComponent leftScrollPane = ImageViewerGui.getLeftScrollPane();
            leftScrollPane.setMinimumSize(new Dimension(leftScrollPane.getPreferredSize().width + ImageViewerGui.SIDE_PANEL_WIDTH_EXTRA, -1));
            ImageViewerGui.getMainFrame().pack();

            LoadStartup.loadCommandLine();

            try {
                JHVUpdate update = new JHVUpdate(false);
                update.check();
            } catch (Exception e) {
                // Should never happen
                Log.error("Error retrieving internal update URL", e);
            }
        });
    }

}
