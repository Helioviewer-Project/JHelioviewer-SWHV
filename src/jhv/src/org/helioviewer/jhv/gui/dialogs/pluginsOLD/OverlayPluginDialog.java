package org.helioviewer.jhv.gui.dialogs.pluginsOLD;

import java.awt.event.ActionEvent;
import java.util.AbstractList;

import javax.swing.JButton;

import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;

/**
 * This class provides a dialog which allows to manage the overlays of plug-ins.
 * Available overlays can be activated so they will be included to the view
 * chains and overlays can be disabled. It is possible to rearrange the order of
 * activated overlays.
 *
 * @author Stephan Pagel
 */
public class OverlayPluginDialog extends AbstractPluginDialog {

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
    public OverlayPluginDialog() {
        super("Overlay Manager");

        initVisualComponents();
        initData();
    }

    /**
     * Initialize the visual parts of the component.
     */
    private void initVisualComponents() {

        // set tool tip to activate and deactivate filter buttons
        super.setActivateButtonToolTipText("Enable overlay");
        super.setDeactivateButtonToolTipText("Disable overlay");

        // available plug ins controll buttons
        activatedPluginsUpButton.addActionListener(this);
        activatedPluginsDownButton.addActionListener(this);

        super.addActivatedPluginsControlButton(activatedPluginsUpButton);
        super.addActivatedPluginsControlButton(activatedPluginsDownButton);
    }

    /**
     * Fills the lists with all available and activated overlays.
     */
    private void initData() {
        // show activated overlays in corresponding list
        AbstractList<OverlayContainer> activatedOverlays = PluginManager.getSingeltonInstance().getOverlayContainers(true);

        for (OverlayContainer container : activatedOverlays) {
            activatedPluginsListModel.addElement(container);
        }

        // show available filter in corresponding list
        AbstractList<OverlayContainer> availableOverlays = PluginManager.getSingeltonInstance().getOverlayContainers(false);

        for (OverlayContainer container : availableOverlays)
            availablePluginsListModel.addElement(container);
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void actionPerformed(ActionEvent arg0) {

        super.actionPerformed(arg0);

        if (arg0.getSource() == activatedPluginsUpButton) {
            moveOverlayUp();
        } else if (arg0.getSource() == activatedPluginsDownButton) {
            moveOverlayDown();
        }
    }

    /**
     * Moves the selected activated overlay by one position in the view chain
     * upward.
     */
    private void moveOverlayUp() {
        setChanged(true);
        Object selected = activatedPluginsList.getSelectedValue();
        int index = activatedPluginsList.getSelectedIndex();

        if (selected != null && selected instanceof OverlayContainer && index > 0) {
            activatedPluginsListModel.remove(index);
            activatedPluginsListModel.add(index - 1, selected);

            activatedPluginsList.setSelectedIndex(index - 1);
        }
    }

    /**
     * Moves the selected activated filter by one position in the view chain
     * downward.
     */
    private void moveOverlayDown() {
        setChanged(true);

        Object selected = activatedPluginsList.getSelectedValue();
        int index = activatedPluginsList.getSelectedIndex();

        if (selected != null && selected instanceof OverlayContainer && index < activatedPluginsListModel.size() - 1) {
            activatedPluginsListModel.remove(index);
            activatedPluginsListModel.add(index + 1, selected);

            activatedPluginsList.setSelectedIndex(index + 1);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Resets the current rank position in the view chain for all overlays.
     */

    @Override
    protected void beforeSaveAndClose() {

        // set rank for all activated overlays
        for (int i = 0; i < activatedPluginsListModel.size(); i++) {
            ((OverlayContainer) activatedPluginsListModel.get(i)).setPosition(i);
            ((OverlayContainer) activatedPluginsListModel.get(i)).changeSettings();
        }

        // set rank to -1 to all not activated overlays
        for (int i = 0; i < availablePluginsListModel.size(); i++) {
            ((OverlayContainer) availablePluginsListModel.get(i)).setPosition(-1);
            ((OverlayContainer) availablePluginsListModel.get(i)).changeSettings();
        }
    }

    @Override
    public void init() {
    }
}
