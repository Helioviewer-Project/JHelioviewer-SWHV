package org.helioviewer.jhv;

import java.awt.EventQueue;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.plaf.FontUIResource;
import javax.swing.UIManager;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.resourceloader.SystemProperties;
import org.helioviewer.viewmodel.view.jp2view.J2KRenderGlobalOptions;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;

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
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

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
        //JHVGlobals.determineVersionAndRevision();

        Log.info("Initializing JHelioviewer");

        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been
        // initialized
        Log.info("Load settings");
        Settings.getSingletonInstance().load();

        // If the user has not specified any desired look and feel yet, the
        // system default theme will be used
        if (Settings.getSingletonInstance().getProperty("display.laf") == null || Settings.getSingletonInstance().getProperty("display.laf").length() <= 0) {
            Log.info("Use default look and feel");
            Settings.getSingletonInstance().setProperty("display.laf", UIManager.getSystemLookAndFeelClassName());
        }
        Settings.getSingletonInstance().save();

        // Set the platform system properties
        SystemProperties.setPlatform();
        Log.info("OS: " + System.getProperty("jhv.os") + " - arch: " + System.getProperty("jhv.arch") + " - java arch: " + System.getProperty("jhv.java.arch"));

        // Remove about menu on mac
        if (System.getProperty("jhv.os").equals("mac")) {
            try {
                Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
                Method getSingletonApplication = applicationClass.getMethod("getApplication", (Class<?>[]) null);
                Object application = getSingletonApplication.invoke(applicationClass.newInstance());
                Method removeAboutMenuItem = applicationClass.getMethod("removeAboutMenuItem", (Class<?>[]) null);
                removeAboutMenuItem.invoke(application);
            } catch (Exception e) {
                Log.warn(">> JavaHelioViewer.main(String[]) > Failed to disable native Mac OS about menu. Probably not running on Mac OS", e);
            }
        }

        /* ----------Setup kakadu ----------- */
        Log.debug("Instantiate Kakadu engine");
        KakaduEngine engine = new KakaduEngine();

        Log.info("Try to load Kakadu libraries");
        try {
            JHVLoader.copyKDULibs();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // The following code-block attempts to start the native message
        // handling
        try {
            Log.debug("Setup Kakadu message handlers.");
            engine.startKduMessageSystem();
        } catch (JHV_KduException e) {
            Log.fatal("Failed to setup Kakadu message handlers.", e);
            Message.err("Error starting Kakadu message handler", e.getMessage(), true);
            return;
        }

        // Apply settings after kakadu engine has been initialized
        Log.info("Use cache directory: " + JHVDirectory.CACHE.getPath());
        JP2Image.setCachePath(JHVDirectory.CACHE.getFile());

        J2KRenderGlobalOptions.setDoubleBufferingOption(true);

        Log.info("Start main window");
        try {
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    UIGlobals.getSingletonInstance(); // initialize
                    setUIFont(new FontUIResource(UIGlobals.UIFont));
                    ImageViewerGui.getSingletonInstance(); // build UI
                    ImageViewerGui.getSingletonInstance().loadAtStart();
                    Settings.getSingletonInstance().update();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        /* ----------Setup Plug-ins ----------- */
        JHVLoader.loadRemotePlugins(args);

        /*
        try {
            JHVLoader.loadJarPlugin("EVEPlugin.jar");
            JHVLoader.loadJarPlugin("PfssPlugin.jar");
            JHVLoader.loadJarPlugin("SWEKPlugin.jar");
            JHVLoader.loadJarPlugin("SWHVHEKPlugin.jar");
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }

    private static void setUIFont(FontUIResource f) {
        Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value != null && value instanceof FontUIResource)
                UIManager.put (key, f);
        }
    }

}
