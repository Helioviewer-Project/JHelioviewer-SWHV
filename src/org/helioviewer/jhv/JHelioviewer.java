package org.helioviewer.jhv;

import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import javax.swing.JFrame;

import org.helioviewer.jhv.app.AppInfo;
import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.HeadlessEDT;
import org.helioviewer.jhv.app.JHVUncaughtExceptionHandler;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.io.CommandLine;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.opengl.AnglePbuffer;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.thread.Task;

public class JHelioviewer {

    static void main(String[] args) throws Exception {
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        System.setProperty("apple.awt.application.name", "JHelioviewer");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("sun.awt.noerasebackground", "true");
        //System.setProperty("org.lwjgl.util.NoChecks", "true");
        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());
        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Per default, the US locale should be used
        Locale.setDefault(Locale.US);

        boolean headless = isHeadless();
        // Uncaught runtime errors are reported to the GUI or stderr, depending on startup mode.
        JHVUncaughtExceptionHandler.setupHandlerForThread(headless);

        // Set the platform
        Platform.init();
        // Create persistent directories, including Logs.
        JHVDirectory.createPersistentDirs();
        // Init log
        Log.init();
        // Create transient cache directories after logging is available. On Windows this may need an ASCII-safe path.
        JHVDirectory.createCacheDirs();
        // Information log message
        Log.info("JHelioviewer started with command-line options: " + String.join(" ", args));

        // Read the version and revision from the JAR metafile
        AppInfo.loadVersion();

        DataSources.initSources(); // sources must be initialized before settings
        Settings.load();
        // System.setProperty("jsamp.nosystray", "true");

        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLine.getUsageMessage());
            return;
        }
        // Save command line arguments
        CommandLine.setArguments(args);

        ProxySettings.init();
        try {
            AppInit.loadSpice();
        } catch (Exception e) {
            Log.error("Failed to setup SPICE", e);
            Message.fatalErr("Failed to setup SPICE:\n" + e.getMessage());
            return;
        }

        PluginManager.setGUIEnabled(!headless);
        if (headless)
            startHeadless();
        else
            startGUI();
    }

    private static void startGUI() {
        EventQueue.invokeLater(() -> {
            Log.info("Start main window");
            UIGlobals.setLaf();
            JFrame frame = JHVFrame.prepare();

            loadPlugins(true);

            frame.pack();
            JHVFrame.stabilizeLeftPaneWidth();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            UITimer.start();

            Task.submit("init", new Init(true), JHelioviewer::onSuccessInit, JHelioviewer::onFailureInit);
        });
    }

    private static void startHeadless() throws InterruptedException {
        HeadlessEDT.invokeLater(() -> {
            Log.info("Start headless mode");
            AnglePbuffer renderer = new AnglePbuffer();
            DisplayController.setRenderRequestHandler(renderer::requestRender);

            loadPlugins(false);

            Task.submit("init", new Init(false), JHelioviewer::onSuccessInit, JHelioviewer::onFailureInit);
        });
    }

    private static void loadPlugins(boolean loadTimelines) {
        try {
            Log.info("Load enabled plugins");
            if (loadTimelines)
                PluginManager.addPlugin(new EVEPlugin());
            PluginManager.addPlugin(new SWEKPlugin());
            PluginManager.addPlugin(new PfssPlugin());
        } catch (Exception e) {
            Log.warn("Plugin load error", e);
        }
    }

    private record Init(boolean webProfilePopup) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            AppInit.init(webProfilePopup);
            return null;
        }
    }

    private static void onSuccessInit(Void ignoredResult) {
        DataSources.loadSources(true);
        CommandLine.load();
    }

    private static void onFailureInit(String ignoredLogContext, Throwable t) {
        Log.error(t);
        Message.err("An error occurred during initialization", t.getMessage());
    }

    private static boolean isHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        return screens == null || screens.length == 0;
    }

}
