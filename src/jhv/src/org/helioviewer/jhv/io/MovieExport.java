package org.helioviewer.jhv.io;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.swing.UIManager;

import org.apache.log4j.Level;
import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.JHVUncaughtExceptionHandler;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.resourceloader.ResourceLoader;
import org.helioviewer.jhv.resourceloader.SystemProperties;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class MovieExport {
    int width = 640;
    int height = 640;
    int framerate = 20;
    String codec = "mpeg4";
    private OutputStream ffmpegStdin;
    private Process ffmpegProcess;
    private String filename = "";

    public MovieExport(int width, int height) {
        this.width = width;
        this.height = height;
        init();

        //createProcess();
        //finishProcess();
    }

    private void init() {

    }

    public void finishProcess() {
        try {
            ffmpegStdin.flush();
            ffmpegStdin.close();
        } catch (IOException e1) {
            Log.error(">> ExportMovieDialog > Error closing FFmpeg stdin.", e1);
        } finally {
            ffmpegStdin = null;
        }

        try {
            ffmpegProcess.waitFor();
        } catch (InterruptedException e1) {
            Log.error(">> ExportMovie > Interrupted while waiting for FFmpeg to finish.", e1);
            ffmpegProcess.destroy();
        }
        ffmpegProcess = null;
    }

    public void writeImage(BufferedImage bufferedImage) {
        //BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        /*
         * Graphics2D g2d = bufferedImage.createGraphics();
         *
         * g2d.drawString("Iets van text", 100, 100); g2d.dispose();
         */
        try {
            bufferedImage.flush();
            ImageIO.write(bufferedImage, "bmp", ffmpegStdin);
            ffmpegStdin.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void createProcess() {
        List<String> ffmpegoptions = new ArrayList<String>();
        ffmpegoptions.add("-qscale:v");
        ffmpegoptions.add("1");

        List<String> args = new ArrayList<String>();
        args.add("-f");
        args.add("image2pipe");
        args.add("-vcodec");
        args.add("bmp");
        args.add("-s");
        args.add("" + width + "x" + height);
        args.add("-r");

        AbstractView activeView = Displayer.getLayersModel().getActiveView();
        if (activeView instanceof JHVJPXView) {
            framerate = ((JHVJPXView) activeView).getDesiredRelativeSpeed();
        }

        if (framerate <= 0 || framerate > 60) {
            framerate = 20;
            Log.warn("Resetting framerate to reasonable value");
        }
        args.add(Integer.toString(framerate));
        args.add("-y");
        args.add("-i");
        args.add("-");
        args.add("-vcodec");
        args.add(codec);
        args.add("-qscale:v");
        args.add("1");
        args.add("-pix_fmt");
        args.add("yuv420p");
        args.add("-vf");
        args.add("scale=out_range=jpeg");

        args.add("-an");
        JHVDirectory exportdir = JHVDirectory.EXPORTS;
        String exportPath = exportdir.getPath();
        int i = 0;

        String path;
        if (activeView instanceof JHVJPXView) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss");
            Date d = new Date(System.currentTimeMillis());
            String dateString = format.format(d);
            path = "JHV_" + ((JHVJPXView) activeView).getName().replace(" ", "_") + "__" + dateString;
        } else {
            path = "JHV_movie_" + i;
        }

        filename = exportPath + path + ".mp4";
        File f = new File(filename);
        while (f.exists()) {
            i++;
            filename = exportPath + path + i + ".mp4";
            f = new File(exportPath + path + i + ".mp4");
        }

        args.add(f.getPath());
        try {
            ffmpegProcess = FileUtils.invokeExecutable("ffmpeg", args);
            ffmpegStdin = ffmpegProcess.getOutputStream();

            try {
                FileUtils.logProcessOutput(ffmpegProcess, "FFmpeg", Level.DEBUG, false);

            } catch (IOException e1) {
                Log.error(">> ExportMovieDialog > Error logging FFmpeg process.", e1);
                return;
            }
        } catch (IOException e) {
            Message.err("FFmpeg error", "Error starting ffmpeg. Cannot export movie", false);
            Log.error(">> ExportMovieDialog > Error starting ffmpeg. Cannot export movie", e);
            return;
        }
    }

    public void release() {
        if (ffmpegStdin != null) {
            try {
                ffmpegStdin.close();
            } catch (IOException e) {
                Log.error(">> ExportMovieDialog > Error closing FFmpeg stdin.", e);
            } finally {
                ffmpegStdin = null;
            }
        }

        if (ffmpegProcess != null) {
            ffmpegProcess.destroy();
            ffmpegProcess = null;
        }
    }

    public static void main(String[] args) {
        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();

        // Save command line arguments
        //CommandLineProcessor.setArguments(args);

        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());

        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        // Save current default locale to user.locale
        System.setProperty("user.locale", Locale.getDefault().toString());

        // Per default, the us locale should be used
        Locale.setDefault(Locale.US);

        // init log
        LogSettings.init("/settings/log4j.initial.properties", JHVDirectory.SETTINGS.getPath() + "log4j.properties", JHVDirectory.LOGS.getPath(), false);

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

        // Directories where to search for lib config files
        URI libs = JHVDirectory.LIBS.getFile().toURI();

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
        if (null == ResourceLoader.getSingletonInstance().loadResource("ffmpeg-2-1", libsRemote, libs, libs, libsBackup, System.getProperties())) {
            Log.error("Error installing FFmpeg");
            Message.err("Error installing FFmpeg", "Could not install FFmpeg tool. Movie export will not work.", false);
        } else {
            Log.info("Successfully installed FFmpeg tool");
        }
        new MovieExport(640, 640);
    }

    public String getFileName() {
        return filename;
    }

}
