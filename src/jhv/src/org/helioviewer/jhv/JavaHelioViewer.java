package org.helioviewer.jhv;

import java.awt.EventQueue;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JComponent;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.message.Message;
import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.plugin.controller.PluginManager;
import org.helioviewer.jhv.resourceloader.SystemProperties;
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduEngine;

/**
 * This class starts the applications.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Markus Langenberg
 * @author Stephan Pagel
 * @author Andre Dau
 * @author Helge Dietert
 *
 */
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
        LogSettings.init("/settings/log4j.initial.properties", JHVDirectory.SETTINGS.getPath() + "log4j.properties", JHVDirectory.LOGS.getPath(), CommandLineProcessor.isOptionSet("--use-existing-log-time-stamp"));

        // Information log message
        String argString = "";
        for (int i = 0; i < args.length; ++i) {
            argString += " " + args[i];
        }
        Log.info("JHelioviewer started with command-line options:" + argString);

        // This attempts to create the necessary directories for the application
        Log.info("Create directories...");
        JHVGlobals.createDirs();

        // Save the log settings. Must be done AFTER the directories are created
        LogSettings.getSingletonInstance().update();

        // Read the version and revision from the JAR metafile
        JHVGlobals.determineVersionAndRevision();

        Log.info("Initializing JHelioviewer");
        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been
        // initialized
        Log.info("Load settings");
        Settings.getSingletonInstance().load();

        // Set the platform system properties
        SystemProperties.setPlatform();
        Log.info("OS: " + System.getProperty("jhv.os") + " - arch: " + System.getProperty("jhv.arch") + " - java arch: " + System.getProperty("jhv.java.arch"));

        Log.debug("Instantiate Kakadu engine");
        try {
            JHVLoader.copyKDULibs();
            KakaduEngine engine = new KakaduEngine();
            engine.startKduMessageSystem();
        } catch (Exception e) {
            Message.err("Failed to setup Kakadu", e.getMessage(), true);
            return;
        }

        FileUtils.deleteDir(JHVDirectory.PLUGINSCACHE.getFile());
        JHVDirectory.PLUGINSCACHE.getFile().mkdirs();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeUtils.getSingletonInstance(); // instantiate class
                UIGlobals.getSingletonInstance().setUIFont(UIGlobals.UIFont);
                Settings.getSingletonInstance().setLookAndFeelEverywhere(null, null); // for Windows and testing

                Log.info("Start main window");
                ExitHooks.attach();
                ImageViewerGui.prepareGui();
                ImageViewerGui.loadImagesAtStartup();

                Log.info("Load plugin settings");
                PluginManager.getSingletonInstance().loadSettings(JHVDirectory.PLUGINS.getPath());

                try {
                    if (theArgs.length != 0 && theArgs[0].equals("--exclude-plugins")) {
                        Log.info("Do not load plugins");
                    } else if (theArgs.length != 0 && theArgs[0].equals("--remote-plugins")) {
                        Log.info("Load remote plugins -- not recommended");
                        JHVLoader.loadRemotePlugins(theArgs);
                    } else {
                        Log.info("Load bundled plugins");
                        JHVLoader.loadBundledPlugin("EVEPlugin.jar");
                        JHVLoader.loadBundledPlugin("SWEKPlugin.jar");
                        JHVLoader.loadBundledPlugin("PfssPlugin.jar");
                        JHVLoader.loadBundledPlugin("SWHVHEKPlugin.jar");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // after loading plugins fix the minimum width of left pane
                JComponent leftPane = ImageViewerGui.getLeftScrollPane();
                leftPane.setMinimumSize(leftPane.getPreferredSize());
            }

            private String[] theArgs;

            public Runnable init(String[] _args) {
                theArgs = _args;
                return this;
            }

        }.init(args));
    }

}
