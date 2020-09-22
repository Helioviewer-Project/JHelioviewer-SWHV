package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.io.CommandLine;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.SampHub;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.log.LogSettings;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;

public class JHelioviewer {

    public static void main(String[] args) throws Exception {
        // Save command line arguments
        CommandLine.setArguments(args);
        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLine.getUsageMessage());
            return;
        }

        if (isHeadless())
            throw new Exception("This application cannot run in a headless configuration.");

        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();
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
        Log.info("JHelioviewer started with command-line options: " + String.join(" ", args));

        // This attempts to create the necessary directories for the application
        JHVGlobals.createDirs();
        // Read the version and revision from the JAR metafile
        JHVGlobals.determineVersionAndRevision();
        Settings.load();
        // Set the platform system properties
        SystemProperties.setPlatform();
        System.setProperty("sun.awt.noerasebackground", "true");
        System.setProperty("org.sqlite.tmpdir", JHVGlobals.libCacheDir);
        System.setProperty("org.lwjgl.system.SharedLibraryExtractPath", JHVGlobals.libCacheDir);
        // System.setProperty("jsamp.nosystray", "true");
        // if (true) throw new RuntimeException("This is a Sentry test. Please ignore.");

        JHVInit.init();

        EventQueue.invokeLater(() -> {
            UIGlobals.setUIFont(UIGlobals.uiFont);

            Log.info("Start main window");
            ExitHooks.attach();
            JFrame frame = JHVFrame.prepare();

            try {
                if (args.length != 0 && args[0].equals("--exclude-plugins")) {
                    Log.info("Do not load plugins");
                } else {
                    Log.info("Load bundled plugins");
                    PluginManager.addPlugin(new EVEPlugin());
                    PluginManager.addPlugin(new SWEKPlugin());
                    PluginManager.addPlugin(new PfssPlugin());
                }
            } catch (Exception e) {
                Log.error("Plugin load error", e);
            }

            JComponent leftPane = JHVFrame.getLeftScrollPane();
            leftPane.setMinimumSize(new Dimension(leftPane.getPreferredSize().width + 10, -1));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            UITimer.start();

            DataSources.loadSources();
            CommandLine.load();
            SampHub.init();

            new JHVUpdate(false).check();
        });
    }

    private static boolean isHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }
        try {
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            return screens == null || screens.length == 0;
        } catch (HeadlessException e) {
            e.printStackTrace();
            return true;
        }
    }

}
