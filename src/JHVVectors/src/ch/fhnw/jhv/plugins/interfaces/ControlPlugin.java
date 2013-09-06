/**
 * 
 */
package ch.fhnw.jhv.plugins.interfaces;

import javax.swing.JComponent;

/**
 * ControlPlugin Interface
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public interface ControlPlugin {

    /**
     * Defines all the different types of ControlPlugins
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    public enum ControlPluginType {
        /**
         * VECTOR
         */
        INFORMATION, SETTINGS, VECTOR_LOADER,

        /**
         * PFSS
         */
        PFSS_LOADER,

        /**
         * BOTH
         */
        CAMERA_CHOOSER, PLUGIN_CHOOSER, PFSS_VISUALIZATION_CHOOSER, MOVIE_PANEL;

    }

    /**
     * Return the Title of the Plugin
     * 
     * @return String title
     */
    public String getTitle();

    /**
     * Return the component of the plugin
     * 
     * @return JComponent component
     */
    public JComponent getComponent();

    /**
     * Get the type of the control plugin
     * 
     * @return
     */
    public ControlPluginType getType();

    /**
     * Should the collapsable pane start expanded
     * 
     * @return boolean shouldStartExpanded
     */
    public boolean shouldStartExpanded();

    /**
     * Disable all the components
     * 
     * @param enabled
     *            is it enabled or not
     */
    public void setEnabled(boolean enabled);

    /**
     * Activate a control plugin
     */
    public void activate();

    /**
     * Deactivate a control plugin
     */
    public void deactivate();

    /**
     * Ask for the current status of the plugin. Is it active?
     * 
     * @return boolean isActive
     */
    public boolean isActive();
}
