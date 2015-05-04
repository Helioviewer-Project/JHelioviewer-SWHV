package org.helioviewer.jhv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.resourceloader.ResourceLoader;
import org.helioviewer.viewmodelplugin.controller.PluginManager;

public class JHVLoader {

    public static void loadJarPlugin(String name) throws IOException, InterruptedException, InvocationTargetException {
        InputStream is = JavaHelioViewer.class.getResourceAsStream("/plugins/" + name);
        String path = JHVDirectory.PLUGINS.getPath() + name;
        File f = new File(path);

        FileUtils.save(is, f);
        PluginManager.getSingletonInstance().loadPlugin(f.toURI());
    }

    public static void copyKDULibs() throws IOException {
        String pathlib = "";
        ArrayList<String> kduLibs = new ArrayList<String>();

        if (System.getProperty("jhv.os").equals("mac") && System.getProperty("jhv.arch").equals("x86-64")) {
            kduLibs.add("libkdu_jni-mac-x86-64.jnilib");
            pathlib = "macosx-universal/";
        } else if (System.getProperty("jhv.os").equals("mac") && System.getProperty("jhv.arch").equals("x86-32")) {
            kduLibs.add("libkdu_jni-mac-x86-32.jnilib");
            pathlib = "macosx-universal/";
        } else if (System.getProperty("jhv.os").equals("windows") && System.getProperty("jhv.arch").equals("x86-64")) {
            kduLibs.add("msvcr100.dll");
            kduLibs.add("kdu_v63R.dll");
            kduLibs.add("kdu_a63R.dll");
            kduLibs.add("kdu_jni.dll");
            pathlib = "windows-amd64/";
        } else if (System.getProperty("jhv.os").equals("windows") && System.getProperty("jhv.arch").equals("x86-32")) {
            kduLibs.add("msvcr100.dll");
            kduLibs.add("kdu_v63R.dll");
            kduLibs.add("kdu_a63R.dll");
            kduLibs.add("kdu_jni.dll");
            pathlib = "windows-i586/";
        } else if (System.getProperty("jhv.os").equals("linux") && System.getProperty("jhv.arch").equals("x86-64")) {
            kduLibs.add("libkdu_jni-linux-x86-64-glibc-2-7.so");
            pathlib = "linux-amd64/";
        } else if (System.getProperty("jhv.os").equals("linux") && System.getProperty("jhv.arch").equals("x86-32")) {
            kduLibs.add("libkdu_jni-linux-x86-32-glibc-2-7.so");
            pathlib = "linux-i586/";
        }

        for (String kduLib : kduLibs) {
            InputStream is = JavaHelioViewer.class.getResourceAsStream("/natives/" + pathlib + kduLib);
            String path = JHVDirectory.LIBS.getPath() + kduLib;
            File f = new File(path);

            FileUtils.save(is, f);
            System.load(f.getAbsolutePath());
        }
    }

    public static void loadRemotePlugins(String[] args) {
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
        PluginManager.getSingletonInstance().loadSettings(JHVDirectory.PLUGINS.getPath());

        Set<String> deactivedPlugins = new HashSet<String>();

        for (int i = 0; i < args.length - 1; ++i) {
            if (args[i].equals("--deactivate-plugin")) {
                deactivedPlugins.add(args[i + 1]);
            }
        }

        Log.info("Download default plugins");
        if (null == ResourceLoader.getSingletonInstance().loadResource("default-plugins", libsRemote, defaultPlugins, defaultPlugins, defaultPluginsBackup, System.getProperties())) {
            Log.error("Error fetching default plugins");
            Message.err("Error fetching default plugins", "Could not download default plugins. You can try to download them from their respective website.", false);
        } else {
            Log.info("Successfully downloaded default plugins.");
        }

        try {
            Log.info("Search for plugins in " + JHVDirectory.PLUGINS.getPath());
            PluginManager.getSingletonInstance().searchForPlugins(JHVDirectory.PLUGINS.getFile(), true, deactivedPlugins);
        } catch (IOException e) {
            String title = "An error occured while loading the plugin files. At least one plugin file is corrupt!";
            String message = "The following files are affected:\n" + e.getMessage();
            Log.error(title + " " + message, e);
            Message.warn(title, message);
        }
    }

}
