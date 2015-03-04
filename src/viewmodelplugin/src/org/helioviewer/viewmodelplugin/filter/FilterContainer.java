package org.helioviewer.viewmodelplugin.filter;

import java.net.URI;

import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodelplugin.controller.PluginSettings;
import org.helioviewer.viewmodelplugin.interfaces.Container;

/**
 * The basic class which manages the interface between JHV and the contained
 * filter.
 * <p>
 * It handles the installation process of a contained filter and manages its
 * current status.
 * 
 * @author Stephan Pagel
 */
public abstract class FilterContainer implements Container {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private boolean active;
    private int position;
    private URI pluginLocation;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Method will be called from JHV to add a filter to a {@link FilterView} of
     * the view chain and add the visual part of the filter to the GUI.
     * 
     * @param filterView
     *            FilterView where to add the contained filter.
     * @param tabList
     *            List which manages the locations for the visual GUI elements
     *            of the filters.
     */
    public final void installFilter(FilterView filterView, FilterTabList tabList) {
        installFilterImpl(filterView, tabList);
        saveFilterSettings();
    }

    /**
     * This method installs the corresponding filter and adds the visual filter
     * control to the GUI.
     * 
     * @param filterView
     *            FilterView where to add the contained filter.
     * @param tabList
     *            List which manages the locations for the visual GUI elements
     *            of the filters.
     */
    protected abstract void installFilterImpl(FilterView filterView, FilterTabList tabList);

    /**
     * Adds or updates the current status to the settings.
     * <p>
     * By calling this method the settings file will not be rewritten! This will
     * be done by the
     * {@link org.helioviewer.viewmodelplugin.controller.PluginManager}.
     */
    private void saveFilterSettings() {
        PluginSettings.getSingletonInstance().filterSettingsToXML(pluginLocation, this);
    }

    /**
     * This method returns the class of the contained filter.
     * 
     * @return Class of the contained filter.
     */
    public abstract Class<? extends Filter> getFilterClass();

    /**
     * {@inheritDoc}
     */
    public boolean isActive() {
        return active;
    }

    /**
     * {@inheritDoc}
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns the current order position of the filter. The position is related
     * to the position of the {@link FilterView}s among each other in the
     * viewchain. The position 0 is the closest position to the
     * {@link org.helioviewer.viewmodel.view.LayeredView}, the position n is the
     * closest one to the {@link org.helioviewer.viewmodel.view.ImageInfoView}.
     * 
     * @return Position of the filter among each other.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the position of the filter among each other.
     * 
     * @param position
     *            New position of the filter among all other filters.
     * @see #getPosition()
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * {@inheritDoc}
     */
    public void changeSettings() {
        saveFilterSettings();
    }

    /**
     * This method returns the user friendly name of the contained filter.
     */

    public String toString() {
        return getName();
    }

    /**
     * Sets the location of the corresponding plug-in.
     * 
     * @param pluginLocation
     *            Location of corresponding plug-in.
     */
    public final void setPluginLocation(URI pluginLocation) {
        this.pluginLocation = pluginLocation;
    }
}
