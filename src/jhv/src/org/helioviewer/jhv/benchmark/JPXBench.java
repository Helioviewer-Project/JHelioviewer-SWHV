package org.helioviewer.jhv.benchmark;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.UIManager;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.JHVUncaughtExceptionHandler;
import org.helioviewer.jhv.KakaduEngine;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.resourceloader.ResourceLoader;
import org.helioviewer.jhv.resourceloader.SystemProperties;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.region.RegionAdapter;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.ViewportAdapter;

public class JPXBench implements ViewListener {
    private final JHVJPXView jpxView;
    private double avg;
    private long previousTime;
    private int numberOfIterations = 0;

    public JPXBench() {
        URI file = (new File("/Users/freekv/JHelioviewer/Downloads/AIA171.jpx")).toURI();
        ImageInfoView view = null;
        try {
            view = APIRequestManager.newLoad(file, true, null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.jpxView = view.getAdapter(JHVJPXView.class);
        previousTime = System.currentTimeMillis();
        this.jpxView.addViewListener(this);
        this.jpxView.render();

    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        System.out.println("HERE");

        if (aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
            long time = System.currentTimeMillis();
            double delta = (-previousTime + time) / 1000.0;
            double fps = 1.0 / delta;
            numberOfIterations = numberOfIterations + 1;
            if (numberOfIterations == 1) {
                avg = fps;
            } else {
                avg = (avg * (numberOfIterations - 1) + fps) / numberOfIterations;
            }
            System.out.println("fps:" + fps);
            System.out.println("avg:" + avg);

            previousTime = time;

            if (this.jpxView.getCurrentFrameNumber() + 1 < this.jpxView.getMaximumFrameNumber()) {
                this.jpxView.setCurrentFrame(this.jpxView.getCurrentFrameNumber() + 1, new ChangeEvent(), true);
            } else {
                this.jpxView.setCurrentFrame(0, new ChangeEvent(), true);
            }
            System.out.println("" + jpxView.getImageData().getWidth() + " - " + jpxView.getImageData().getHeight());

            System.out.println(((HelioviewerMetaData) jpxView.getMetadata()).getSunPixelRadius());
            System.out.println(((HelioviewerMetaData) jpxView.getMetadata()).getDateTime().getFormattedDate());
            System.out.println(((HelioviewerMetaData) jpxView.getMetadata()).getDateTime().getMillis());
            JHVJP2View jp2View = jpxView.getAdapter(JHVJP2View.class);
            if (jp2View != null) {
                HelioviewerMetaData md = (HelioviewerMetaData) jp2View.getMetadata();
                Double mpp = md.getUnitsPerPixel();
                jp2View.setViewport(new ViewportAdapter(new StaticViewport(1024, 1024)), new ChangeEvent());
                jp2View.setRegion(new RegionAdapter(new StaticRegion(0 * mpp, 0 * mpp, new Vector2dDouble(4096.0 * mpp, 4096.0 * mpp))), new ChangeEvent());
            }
        }
    }

    public static void main(String[] args) {
        jhvMain(args);
        JPXBench jpxbench = new JPXBench();

    }

    private static void jhvMain(String[] args) {
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
        // JHVSplashScreen splash = JHVSplashScreen.getSingletonInstance();

        int numProgressSteps = 10;
        Log.debug("Number of progress steps: " + numProgressSteps);
        // splash.setProgressSteps(numProgressSteps);

        // splash.setProgressText("Initializing JHelioviewer...");

        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been
        // initialized
        // splash.setProgressText("Loading settings...");
        // splash.nextStep();
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
        // splash.nextStep();
        // splash.setProgressText("Determining platform...");

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
            // splash.setProgressText("Determining glibc version...");
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

        // splash.nextStep();
        // splash.setProgressText("Initializing Kakadu libraries...");
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
        // splash.nextStep();
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

        // splash.nextStep();
        // splash.setProgressText("Load OpenGL libraries...");

        final URI finalLibs = libs;
        final URI finalLibsRemote = libsRemote;
        final URI finalLibsBackup = libsBackup;

        // Has to run in EventQueue due to bug in NVidia Driver 260.99
        try {
            EventQueue.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    Log.info("Try to load OpenGL libraries");
                    if (!System.getProperty("jhv.os").equals("mac")) {
                        System.loadLibrary("jawt");
                    }
                    if (null == ResourceLoader.getSingletonInstance().loadResource("jogl", finalLibsRemote, finalLibs, finalLibs, finalLibsBackup, System.getProperties())) {
                        Log.error("Could not load OpenGL libraries");
                        Message.err("Error loading OpenGL libraries", "The OpenGL libraries could not be loaded. JHelioviewer will run in software mode.", false);
                        GLInfo.glUnusable();
                    } else {
                        // com.sun.opengl.impl.NativeLibLoader.disableLoading();
                        // com.sun.gluegen.runtime.NativeLibLoader.disableLoading();
                        Log.info("Successfully loaded OpenGL libraries");
                    }
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }

}
