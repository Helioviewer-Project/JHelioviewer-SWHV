package ch.fhnw.jhv.gui;

import ch.fhnw.jhv.plugins.PluginBundle.PluginBundleType;
import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.pfss.PfssPlugin;
import ch.fhnw.jhv.plugins.vectors.VectorPlugin;

/**
 * Settings class contains different settings of the plugins
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class Settings {

    /**
     * Load default settings
     */
    public void loadDefaultSettings() {
        PfssPlugin pfssPlugin = new PfssPlugin();
        VectorPlugin vectorPlugin = new VectorPlugin();

        PluginManager pluginManager = PluginManager.getInstance();

        pluginManager.addPlugin(pfssPlugin);
        pluginManager.addPlugin(vectorPlugin);

        // Per default install VectorPlugin
        pluginManager.installPluginBundle(PluginBundleType.VECTOR);
    }
}
