package org.helioviewer.jhv.base.plugin.interfaces;

/**
 * A container gives the possibility to add additional information to its
 * content and allows to administer it.
 * <p>
 * Actually a container in the viewmodelplugin project can hold two different
 * kind of contents. On the one hand it undertakes the task of administer a
 * plug-in and on the other hand it manages the availability of a filter.
 * 
 * @author Stephan Pagel
 */
public interface Container {

    /**
     * This method returns a user friendly name of the content this container
     * contains.
     * 
     * @return User friendly name of the content this container contains. A null
     *         value as return value is possible but should be avoided!
     */
    public String getName();

    /**
     * Returns a short description what the content of the container does.
     * 
     * @return Short description what the content of the container does. A null
     *         value as return value is possible.
     */
    public String getDescription();

    /**
     * This method returns if the content is currently activated or not.
     * 
     * @return True if the content is currently used; false otherwise.
     */
    public boolean isActive();

    /**
     * Sets the content activated or not.
     * 
     * @param active
     *            true if content has to be activated or false if it has to be
     *            deactivated.
     */
    public void setActive(boolean active);

    /**
     * If this method is called an update to the settings file will be executed.
     */
    public void changeSettings();

}
