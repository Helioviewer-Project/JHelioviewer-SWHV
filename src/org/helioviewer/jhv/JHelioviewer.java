package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.io.CommandLine;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.io.UpdateChecker;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;

import com.google.common.util.concurrent.FutureCallback;

public class JHelioviewer {

    public static void main(String[] args) throws Exception {
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        System.setProperty("apple.awt.application.name", "JHelioviewer");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());
        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Per default, the US locale should be used
        Locale.setDefault(Locale.US);

        if (isHeadless())
            throw new Exception("This application cannot run in a headless configuration.");

        // Set the platform
        Platform.init();
        // This attempts to create the necessary directories for the application
        JHVGlobals.createDirs();
        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();
        // Init log
        Log.init();
        // Information log message
        Log.info("JHelioviewer started with command-line options: " + String.join(" ", args));

        // Read the version and revision from the JAR metafile
        JHVGlobals.getVersion();

        DataSources.initSources(); // sources must be initialized before settings
        Settings.load();
        System.setProperty("sun.awt.noerasebackground", "true");
        System.setProperty("org.sqlite.tmpdir", JHVGlobals.libCacheDir);
        System.setProperty("org.lwjgl.system.SharedLibraryExtractPath", JHVGlobals.libCacheDir);
        // System.setProperty("flatlaf.nativeLibraryPath", JHVGlobals.libCacheDir);
        // System.setProperty("jsamp.nosystray", "true");

        ProxySettings.init();
        JHVInit.loadSpice();

        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLine.getUsageMessage());
            return;
        }
        // Save command line arguments
        CommandLine.setArguments(args);

        EventQueue.invokeLater(() -> {
            Log.info("Start main window");
            UIGlobals.setLaf();
            JFrame frame = JHVFrame.prepare();

            try {
                Log.info("Load enabled plugins");
                PluginManager.addPlugin(new EVEPlugin());
                PluginManager.addPlugin(new SWEKPlugin());
                PluginManager.addPlugin(new PfssPlugin());
            } catch (Exception e) {
                Log.warn("Plugin load error", e);
            }

            JComponent leftPane = JHVFrame.getLeftScrollPane();
            ImageLayer dummy = ImageLayer.create(null);
            leftPane.setMinimumSize(new Dimension(leftPane.getPreferredSize().width, -1));
            dummy.unload();

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            UITimer.start();

            EDTCallbackExecutor.pool.submit(new Init(), new Callback());
        });
    }

    private record Init() implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            JHVInit.init();
            return null;
        }
    }

    private static class Callback implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
            DataSources.loadSources();
            CommandLine.load();
            UpdateChecker.check(false);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred during initialization", t.getMessage());
        }

    }

    private static boolean isHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        return screens == null || screens.length == 0;
    }

}
