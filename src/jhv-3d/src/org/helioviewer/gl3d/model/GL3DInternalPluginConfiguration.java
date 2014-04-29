package org.helioviewer.gl3d.model;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.plugin.GL3DModelPlugin;
import org.helioviewer.gl3d.plugin.GL3DPluginConfiguration;
//import org.helioviewer.gl3d.plugin.hekplugin.HEKPlugin;
//import org.helioviewer.gl3d.plugin.pfss.PfssPlugin;
//import org.helioviewer.gl3d.plugin.vectors.VectorsPlugin;

/**
 * A static plugin configuration that includes the three internal plugins - PFSS
 * - Vectors - HEK 3d-Plugin
 * 
 * @author Julian Fisch (julian.fisch@fhnw.ch)
 * 
 */
public class GL3DInternalPluginConfiguration implements GL3DPluginConfiguration {

    private static class PluginEntry {
        public String path;
        public String clazz;

        public PluginEntry(String path, String clazz) {
            this.path = path;
            this.clazz = clazz;
        }
    }

    @Override
    public List<GL3DModelPlugin> findPlugins() {
        List<GL3DModelPlugin> plugins = new ArrayList<GL3DModelPlugin>();

        /*
         * ArrayList<PluginEntry> entries = new ArrayList<PluginEntry>();
         * entries.add(new
         * PluginEntry("C:\\Projects\\JHelioViewer\\hek3d_plugin.jar",
         * "org.helioviewer.gl3d.plugin.hekplugin.HEKPlugin")); entries.add(new
         * PluginEntry("C:\\Projects\\JHelioViewer\\pfss_plugin.jar",
         * "org.helioviewer.gl3d.plugin.pfss.PfssPlugin"));
         * 
         * for(PluginEntry entry : entries) { GL3DModelPlugin p =
         * loadPlugin(entry.path, entry.clazz); if(p != null && p instanceof
         * GL3DModelPlugin) plugins.add((GL3DModelPlugin) p); }
         */

        return plugins;
    }

    private GL3DModelPlugin loadPlugin(String path, String clazz) {
        File f = new File(path);

        // Load plugins with a URLClassLoader
        try {
            URL[] urls = new URL[1];
            urls[0] = f.toURL();

            URLClassLoader classLoader = new URLClassLoader(urls);
            return (GL3DModelPlugin) classLoader.loadClass(clazz).newInstance();
        } catch (Exception e) {
            Log.error(">> Couldn't load plugin from: " + path + ": " + e.getMessage());
        }

        return null;
    }

    // @Override
    public List<GL3DModelPlugin> findPlugins2() {
        File pfss = new File("C:\\Users\\jf\\Desktop\\PfssPlugin.jar");
        // File vectors = new File("C:\\Users\\jf\\Desktop\\VectorsPlugin.jar");
        // File hek = new File("C:\\Users\\jf\\Desktop\\HEKPlugin.jar");
        // File hek = new
        // File("C:\\Users\\jf\\Documents\\FHNW\\projects\\hekplugin-3d\\trunk\\bin\\");
        File hek = new File("C:\\Projects\\JHelioViewer\\hekplugin-3d\\hek3d.jar");

        if (hek.exists())
            System.out.println("EXISTS ========================================================================================");
        else
            System.out.println("NOT EXISTS ========================================================================================");

        /*
         * try { //urls[0] = pfss.toURL(); //urls[1] = vectors.toURL();
         * //urls[2] = hek.toURL(); urls[0] = hek.toURL(); } catch
         * (MalformedURLException e) { e.printStackTrace(); }
         */

        List<GL3DModelPlugin> plugins = new ArrayList<GL3DModelPlugin>();

        // Load plugins with a URLClassLoader
        try {
            URL[] urls = new URL[1];

            urls[0] = hek.toURL();

            URLClassLoader classLoader = new URLClassLoader(urls);

            // /// classLoader.loadClass(name)

            Object pfssPlugin = null;
            // Object vectorsPlugin = null;
            Object hekPlugin = null;

            pfssPlugin = classLoader.loadClass("org.helioviewer.gl3d.plugin.pfss.PfssPlugin").newInstance();
            // vectorsPlugin =
            // classLoader.loadClass("org.helioviewer.gl3d.plugin.vectors.VectorsPlugin").newInstance();
            hekPlugin = classLoader.loadClass("org.helioviewer.gl3d.plugin.hekplugin.HEKPlugin").newInstance();

            plugins.add((GL3DModelPlugin) pfssPlugin);
            // plugins.add((GL3DModelPlugin) vectorsPlugin);
            plugins.add((GL3DModelPlugin) hekPlugin);
            // plugins.add(new VectorsPlugin());
            // plugins.add(new HEKPlugin());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return plugins;
    }

}
