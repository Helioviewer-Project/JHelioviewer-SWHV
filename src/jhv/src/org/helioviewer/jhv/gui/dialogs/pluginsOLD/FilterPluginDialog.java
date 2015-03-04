package org.helioviewer.jhv.gui.dialogs.pluginsOLD;

import java.awt.event.ActionEvent;
import java.util.AbstractList;

import javax.swing.JButton;

import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.SOHOLUTFilter;
import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.filter.FilterContainer;

/**
 * This class provides a dialog which allows to manage the filters of plug-ins.
 * Available filter can be activated so they will be included to the view chains
 * and filter can be disabled. It is possible to rearrange the order of
 * activated filters.
 *
 * @author Stephan Pagel
 */
public class FilterPluginDialog extends AbstractPluginDialog {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private final JButton activatedPluginsUpButton = new JButton("Up");
    private final JButton activatedPluginsDownButton = new JButton("Down");

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     */
    public FilterPluginDialog() {
        super("Filter Manager");

        initVisualComponents();

        initData();
    }

    /**
     * Initialize the visual parts of the component.
     */
    private void initVisualComponents() {

        // set tool tip to activate and deactivate filter buttons
        super.setActivateButtonToolTipText("Enable filter");
        super.setDeactivateButtonToolTipText("Disable filter");

        // add control buttons the the activated filter list to change the order
        // of filters.
        activatedPluginsUpButton.addActionListener(this);
        activatedPluginsDownButton.addActionListener(this);

        super.addActivatedPluginsControlButton(activatedPluginsUpButton);
        super.addActivatedPluginsControlButton(activatedPluginsDownButton);
    }

    /**
     * Fills the lists with all available and activated filters.
     */
    private void initData() {

        // show activated filter in corresponding list
        AbstractList<FilterContainer> activatedFilters = PluginManager.getSingletonInstance().getFilterContainers(true);

        for (FilterContainer container : activatedFilters) {
            activatedPluginsListModel.addElement(container);
        }

        // show available filter in corresponding list
        AbstractList<FilterContainer> availableFilters = PluginManager.getSingletonInstance().getFilterContainers(false);

        for (FilterContainer container : availableFilters)
            availablePluginsListModel.addElement(container);
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void actionPerformed(ActionEvent arg0) {

        super.actionPerformed(arg0);

        if (arg0.getSource() == activatedPluginsUpButton) {
            moveFilterUp();
        } else if (arg0.getSource() == activatedPluginsDownButton) {
            moveFilterDown();
        }
    }

    /**
     * Moves the selected activated filter by one position in the view chain
     * upward.
     */
    private void moveFilterUp() {
        setChanged(true);
        Object selected = activatedPluginsList.getSelectedValue();
        int index = activatedPluginsList.getSelectedIndex();

        if (selected != null && selected instanceof FilterContainer && index > 0) {
            activatedPluginsListModel.remove(index);
            activatedPluginsListModel.add(index - 1, selected);

            activatedPluginsList.setSelectedIndex(index - 1);
        }
    }

    /**
     * Moves the selected activated filter by one position in the view chain
     * downward.
     */
    private void moveFilterDown() {
        setChanged(true);
        Object selected = activatedPluginsList.getSelectedValue();
        int index = activatedPluginsList.getSelectedIndex();

        if (selected != null && selected instanceof FilterContainer && index < activatedPluginsListModel.size() - 1) {
            activatedPluginsListModel.remove(index);
            activatedPluginsListModel.add(index + 1, selected);

            activatedPluginsList.setSelectedIndex(index + 1);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Resets the current rank position in the view chain for all filters.
     */

    @Override
    protected void beforeSaveAndClose() {

        // put SOHOLUTFilter last (such that it is applied first); otherwise
        // there might be problems with other filters, eg OpacityFilter
        // this is because the SOHOLUTFilter expects single channel image data
        // and OpacityFilter has to output ARGB data
        // as a result the image can become gray if applying opacity first
        for (int i = 0; i < activatedPluginsListModel.size(); i++) {
            FilterContainer filterContainer = (FilterContainer) activatedPluginsListModel.get(i);
            if (filterContainer.getFilterClass() == SOHOLUTFilter.class) {
                activatedPluginsListModel.remove(i);
                activatedPluginsListModel.addElement(filterContainer);
                break;
            }
        }

        // set rank for all activated filters
        for (int i = 0; i < activatedPluginsListModel.size(); i++) {
            ((FilterContainer) activatedPluginsListModel.get(i)).setPosition(i);
            ((FilterContainer) activatedPluginsListModel.get(i)).changeSettings();
        }

        // set rank to -1 to all not activated filters
        for (int i = 0; i < availablePluginsListModel.size(); i++) {
            ((FilterContainer) availablePluginsListModel.get(i)).setPosition(-1);
            ((FilterContainer) availablePluginsListModel.get(i)).changeSettings();
        }
    }

    @Override
    public void init() {
    }
}
