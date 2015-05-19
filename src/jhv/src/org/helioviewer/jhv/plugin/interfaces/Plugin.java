package org.helioviewer.jhv.plugin.interfaces;

/**
 * This interface holds the basic methods of every JHV plug-in. JHV identifies a
 * plug-in by this interface.
 * <p>
 * A class which implements this interface has to be located in the
 * org.helioviewer.jhv.plugins package of the java project and the class name
 * has to be the same as the exported JAR file. When JHV is searching for
 * plug-ins it checks the JAR files if there is a class with the name of the JAR
 * file and if the class implements this interface.
 * 
 * @author Stephan Pagel
 */
public interface Plugin {

    /**
     * This method returns a user friendly name of the plug-in which will be
     * displayed in the plug-in overview for instance.
     * 
     * @return A user friendly name of the plug-in.
     */
    public String getName();

    /**
     * This method returns a short description of the plug-in which gives a
     * short review about the functionality.
     * 
     * @return Short description of the plug-in.
     */
    public String getDescription();

    /**
     * This method will be called by the JHV application when the user want's to
     * activate the plug-in. The plug-in has to do all necessary things to
     * register and initialize itself in JHV here.
     */
    public void installPlugin();

    /**
     * This method will be called by the JHV application when the user want's to
     * deactivate the plug-in. The plug-in has to do all necessary things to
     * deregister itself in JHV here.
     */
    public void uninstallPlugin();

    /**
     * Sets the plug-in state.
     * 
     * The format of the state is determined by the plug-in itself. It should
     * encode all necessary values to restore the behavior of the plug-in from
     * earlier sessions.
     * 
     * @param state
     *            The new filter state
     * @see #getState()
     */
    public void setState(String state);

    /**
     * Gets the plug-in state.
     * 
     * The format of the state is determined by the plug-in itself. It should
     * encode all necessary values to restore the behavior of the plug-in from
     * earlier sessions.
     * 
     * @param state
     *            The new filter state
     * @see #setState()
     */
    public String getState();

    /**
     * This method is used to display licenses and other information about the
     * plugin in the about dialog of jhv.
     */
    public String getAboutLicenseText();

}
