/**
 * 
 */
package ch.fhnw.jhv.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ch.fhnw.jhv.gui.controller.cam.CameraContainer;
import ch.fhnw.jhv.gui.controller.cam.CameraContainer.CameraType;
import ch.fhnw.jhv.gui.viewport.ViewPort;
import ch.fhnw.jhv.plugins.PluginBundle;
import ch.fhnw.jhv.plugins.PluginBundle.PluginBundleType;
import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;

/**
 * PluginChooserControlPlugin is responsible for choosing the current active
 * plugin.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class PluginChooserControlPlugin extends AbstractPlugin implements ControlPlugin {

    /**
     * ComboBox which contains the different plugins
     */
    private JComboBox pluginsCombobox;

    /**
     * PluginManager instance
     */
    private PluginManager pluginManager = PluginManager.getInstance();

    /**
     * Contains all the listeners which are interested if the plugin selection
     * has changed.
     */
    private ArrayList<Listener> listeners = new ArrayList<Listener>();

    /**
     * Old PluginType
     */
    private PluginBundleType currPluginType;

    /**
     * Return the title
     * 
     * @return String Title of the ControlPlugin
     */
    public String getTitle() {
        return "Plugin Chooser";
    }

    /**
     * Listener Interface for notifying Listeners if current active plugin has
     * been changed.
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    public interface Listener {

        /**
         * The current active plugin has been changed. Notify all the listeners
         * and pass them the new PluginType.
         * 
         * @param type
         *            the plugin bundle type
         */
        void pluginChanged(PluginBundleType type);
    }

    /**
     * Constructor
     * 
     */
    public PluginChooserControlPlugin() {
        pluginsCombobox = new JComboBox();
        pluginsCombobox.setToolTipText("Select a plugin to load");

        ArrayList<PluginBundle> plugins = pluginManager.getPlugins();

        for (PluginBundle plugin : plugins) {
            pluginsCombobox.addItem(plugin.getType());
        }

        // NOTE:
        // Not working if there are more than one plugin active..
        // But this is currently not possible
        pluginsCombobox.setSelectedItem(PluginManager.getInstance().getActivePlugins().get(0).getType());
        currPluginType = (PluginBundleType) pluginsCombobox.getSelectedItem();
    }

    /**
     * Get the ControlPlugin Component
     * 
     * return JComponent ControlPlugin Component
     */
    public JComponent getComponent() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("Choose a plugin: "));

        pluginsCombobox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();

                PluginBundleType type = (PluginBundleType) cb.getSelectedItem();

                if (!type.equals(currPluginType)) {

                    // UNINSTALL all old plugins
                    @SuppressWarnings("unchecked")
                    ArrayList<PluginBundle> activePlugins = (ArrayList<PluginBundle>) pluginManager.getActivePlugins().clone();

                    for (PluginBundle plugin : activePlugins) {
                        pluginManager.uninstallPluginBundle(plugin.getType());
                    }

                    // Set cam to RotationCameraSun
                    ViewPort.getInstance().setActiveCamera(CameraContainer.getCamera(CameraType.ROTATION_SUN));

                    // INSTALL the new plugin
                    pluginManager.installPluginBundle(type);

                    for (Listener listener : listeners) {
                        listener.pluginChanged(type);
                    }

                    currPluginType = type;
                }
            }
        });

        panel.add(pluginsCombobox);

        return panel;
    }

    /**
     * Register a new listener for plugin changes
     * 
     * @param listener
     *            Listener
     */
    public void addListener(Listener listener) {
        if (!this.listeners.contains(listener))
            this.listeners.add(listener);
    }

    /**
     * Return the ControlPlugin type
     * 
     * @return ControlPluginType type
     */
    public ControlPluginType getType() {
        return ControlPluginType.PLUGIN_CHOOSER;
    }

    /**
     * Should the ControlPlugin start in expanded mode
     * 
     * @return boolean Should start in expanded mode
     */
    public boolean shouldStartExpanded() {
        return true;
    }
}
