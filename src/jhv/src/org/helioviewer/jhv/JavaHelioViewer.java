package org.helioviewer.jhv;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.UIManager;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.layerTable.LayerTableOverlapWatcher;
import org.helioviewer.jhv.internal_plugins.InternalFilterPlugin;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.resourceloader.ResourceLoader;
import org.helioviewer.jhv.resourceloader.SystemProperties;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

import com.jogamp.common.jvm.JNILibLoaderBase;

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
    static class JoglLoaderDummy implements JNILibLoaderBase.LoaderAction {

        @Override
        public boolean loadLibrary(String arg0, boolean arg1, ClassLoader arg2) {
            return true;
        }

        @Override
        public void loadLibrary(String arg0, String[] arg1, boolean arg2, ClassLoader arg3) {
        }
    }

    public static void main(String[] args) {
        main(args, (Plugin[]) null);
    }

    public static void main(String[] args, Plugin builtinPlugin) {
        main(args, new Plugin[] { builtinPlugin });
    }

    public static void main(String[] args, Plugin[] builtinPlugins) {
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
        JHVGlobals.determineVersionAndRevision();

        Log.info("Initializing JHelioviewer");
        // display the splash screen
        Log.debug("Create splash screen");
        JHVSplashScreen splash = JHVSplashScreen.getSingletonInstance();

        int numProgressSteps = 10;
        Log.debug("Number of progress steps: " + numProgressSteps);
        splash.setProgressSteps(numProgressSteps);

        splash.setProgressText("Initializing JHelioviewer...");

        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been
        // initialized
        splash.setProgressText("Loading settings...");
        splash.nextStep();
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
        splash.nextStep();
        splash.setProgressText("Determining platform...");

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

        // Directories where to search for lib config files
        URI libs = JHVDirectory.LIBS.getFile().toURI();
        URI defaultPlugins = JHVDirectory.PLUGINS.getFile().toURI();
        URI defaultPluginsBackup = JHVDirectory.PLUGINS_LAST_CONFIG.getFile().toURI();
        URI libsBackup = JHVDirectory.LIBS_LAST_CONFIG.getFile().toURI();
        URI libsRemote = null;
        try {
            Log.warn(Settings.getSingletonInstance().getProperty("default.remote.lib.path"));
            libsRemote = new URI(Settings.getSingletonInstance().getProperty("default.remote.lib.path"));
        } catch (URISyntaxException e1) {
            Log.error("Invalid uri for remote library server");
        }

        // Determine glibc version
        if (System.getProperty("jhv.os").equals("linux")) {
            splash.setProgressText("Determining glibc version...");
            Log.info("Try to install glibc-version tool");
            if (null == ResourceLoader.getSingletonInstance().loadResource("glibc-version", libsRemote, libs, libs, libsBackup, System.getProperties())) {
                Log.error(">> JavaHelioViewer > Could not load glibc-version tool");
                Message.err("Error loading glibc-version tool", "Error! The glibc-version tool could not be loaded. This may slow down the loading process and increase the network load.", false);
            } else {
                Log.info("Successfully installed glibc version tool");
                try {
                    if (SystemProperties.setGLibcVersion() != null) {
                        Log.info("Successfully determined glibc version: " + System.getProperty("glibc.version"));
                    } else {
                        Log.error(">> JavaHelioViewer > Could not determine glibc version");
                        Message.err("Error detecting glibc version", "Error! The glibc version could not be detected. This may slow down the loading process and increase the network load.", false);
                    }
                } catch (Throwable t) {
                    Log.error(">> JavaHelioViewer > Could not determine glibc version", t);
                    Message.err("Error detecting glibc version", "Error! The glibc version could not be detected. This may slow down the loading process and increase the network load.", false);
                }
            }
        }

        /* ----------Setup kakadu ----------- */
        Log.debug("Instantiate Kakadu engine");
        KakaduEngine engine = new KakaduEngine();

        splash.nextStep();
        splash.setProgressText("Initializing Kakadu libraries...");
        Log.info("Try to load Kakadu libraries");
        if (null == ResourceLoader.getSingletonInstance().loadResource("kakadu", libsRemote, libs, libs, libsBackup, System.getProperties())) {
            Log.fatal("Could not load Kakadu libraries");
            Message.err("Error loading Kakadu libraries", "Fatal error! The kakadu libraries could not be loaded. The log output may contain additional information.", true);
            return;
        } else {
            Log.info("Successfully loaded Kakadu libraries");
        }

        // The following code-block attempts to start the native message
        // handling
        splash.nextStep();
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

        Log.info("Update settings");
        Settings.getSingletonInstance().update();

        /* ----------Setup OpenGL ----------- */

        splash.nextStep();
        splash.setProgressText("Load OpenGL libraries...");

        final URI finalLibs = libs;
        final URI finalLibsRemote = libsRemote;
        final URI finalLibsBackup = libsBackup;

        // Has to run in EventQueue due to bug in NVidia Driver 260.99
        try {
            EventQueue.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    Log.info("Try to load OpenGL libraries");

                    if (null == ResourceLoader.getSingletonInstance().loadResource("jogl2.2.0", finalLibsRemote, finalLibs, finalLibs, finalLibsBackup, System.getProperties())) {
                        Log.error("Could not load OpenGL libraries");
                        Message.err("Error loading OpenGL libraries", "The OpenGL libraries could not be loaded. JHelioviewer will run in software mode.", false);
                        GLInfo.glUnusable();
                    } else {
                        System.setProperty("jogamp.gluegen.UseTempJarCache", "false");
                        JNILibLoaderBase.setLoadingAction(new JoglLoaderDummy());
                        Log.info("Successfully loaded OpenGL libraries");
                    }
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        Log.info("Try to install CG Compiler");
        if (null == ResourceLoader.getSingletonInstance().loadResource("cgc", libsRemote, libs, libs, libsBackup, System.getProperties())) {
            Log.error("Could not install CG Compiler");
            Message.err("Error installing CG Compiler", "The CG Compiler could not be installed. JHelioviewer will run in software mode.", false);
            GLInfo.glUnusable();
        } else {
            Log.info("Successfully installed CG Compiler");
        }

        /* ----------Setup FFmpeg ----------- */

        splash.nextStep();
        splash.setProgressText("Initialize FFmpeg...");
        // Load/download ffmpeg
        Log.info("Install FFmpeg");
        if (null == ResourceLoader.getSingletonInstance().loadResource("ffmpeg-2-1", libsRemote, libs, libs, libsBackup, System.getProperties())) {
            Log.error("Error installing FFmpeg");
            Message.err("Error installing FFmpeg", "Could not install FFmpeg tool. Movie export will not work.", false);
        } else {
            Log.info("Successfully installed FFmpeg tool");
        }

        splash.nextStep();
        splash.setProgressText("Initialize MP4Box...");
        // Load/download mp4box
        Log.info("Install MP4Box");
        if (null == ResourceLoader.getSingletonInstance().loadResource("mp4box", libsRemote, libs, libs, libsBackup, System.getProperties())) {
            Log.error("Error installing MP4Box");
            Message.err("Error installing MP4Box", "Could not install MP4Box tool. Exported movies will not contain timestamps in subtitles.", false);
        } else {
            Log.info("Successfully installed MP4Box tool");
        }

        // This code updates the ImageViewer
        Log.info("Initialize GUI");
        ImageViewerGui.getSingletonInstance().updateComponents();

        // Check for updates in parallel, if newer version is available a small
        // message is displayed
        try {
            JHVUpdate update = new JHVUpdate();
            update.check();
        } catch (MalformedURLException e) {
            // Should never happen
            Log.error("Error retrieving internal update URL", e);
        }

        Log.debug("Installing Overlap Watcher");
        LayerTableOverlapWatcher overlapWatcher = new LayerTableOverlapWatcher();
        LayersModel.getSingletonInstance().addLayersListener(overlapWatcher);

        /* ----------Setup Plug-ins ----------- */

        splash.setProgressText("Loading Plugins...");
        splash.nextStep();

        // check if plug-ins have to be deleted
        final File tmpFile = new File(JHVDirectory.PLUGINS.getPath() + JHVGlobals.TEMP_FILENAME_DELETE_PLUGIN_FILES);

        if (tmpFile.exists()) {
            try {
                final BufferedReader in = new BufferedReader(new FileReader(tmpFile));

                String line = null;
                String content = "";

                while ((line = in.readLine()) != null) {
                    content += line;
                }

                in.close();

                final StringTokenizer st = new StringTokenizer(content, ";");

                while (st.hasMoreElements()) {
                    final File delFile = new File(st.nextToken());
                    delFile.delete();
                }

                tmpFile.delete();
            } catch (final Exception e) {
            }
        }

        // Load Plug ins at the very last point
        Log.info("Load plugin settings");
        PluginManager.getSingeltonInstance().loadSettings(JHVDirectory.PLUGINS.getPath());

        Log.info("Add internal plugin: " + "FilterPlugin");
        Plugin internalPlugin = new InternalFilterPlugin();
        PluginManager.getSingeltonInstance().addInternalPlugin(internalPlugin.getClass().getClassLoader(), internalPlugin);

        if (builtinPlugins != null) {
            for (int i = 0; i < builtinPlugins.length; ++i) {
                PluginManager.getSingeltonInstance().addPlugin(builtinPlugins[i].getClass().getClassLoader(), builtinPlugins[i], null);
            }
        }

        Set<String> deactivedPlugins = new HashSet<String>();

        for (int i = 0; i < args.length - 1; ++i) {
            if (args[i].equals("--deactivate-plugin")) {
                deactivedPlugins.add(args[i + 1]);
            }
        }

        Log.info("Download default plugins");
        if (null == ResourceLoader.getSingletonInstance().loadResource("sdo-cutout", libsRemote, defaultPlugins, defaultPlugins, defaultPluginsBackup, System.getProperties())) {
            Log.error("Error fetching SDO-Cutout plugin. Maybe JHelioviewer version not recent enough. Try updating JHV to the newest version.");
        } else {
            Log.info("Successfully downloaded SDO-Cutout plugin.");
        }

        if (null == ResourceLoader.getSingletonInstance().loadResource("default-plugins", libsRemote, defaultPlugins, defaultPlugins, defaultPluginsBackup, System.getProperties())) {
            Log.error("Error fetching default plugins");
            Message.err("Error fetching default plugins", "Could not download default plugins. You can try to download them from their respective website.", false);
        } else {
            Log.info("Successfully downloaded default plugins.");
        }

        try {
            Log.info("Search for plugins in " + JHVDirectory.PLUGINS.getPath());
            PluginManager.getSingeltonInstance().searchForPlugins(JHVDirectory.PLUGINS.getFile(), true, deactivedPlugins);
        } catch (IOException e) {
            String title = "An error occured while loading the plugin files. At least one plugin file is corrupt!";
            String message = "The following files are affected:\n" + e.getMessage();
            Log.error(title + " " + message, e);
            Message.warn(title, message);
        }

        splash.setProgressText("Start main window...");
        splash.nextStep();
        // Create main view chain and display main window
        Log.info("Start main window");
        splash.initializeViewchain();

    }
};
