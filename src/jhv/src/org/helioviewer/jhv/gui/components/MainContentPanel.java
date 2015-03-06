package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;

/**
 * This panel acts as a container for the GUI elements which are shown in the
 * main area of the application. Usually it contains the main image area. Below
 * the main image area plug-ins are able to display their GUI components.
 * 
 * @author Stephan Pagel
 * */
public class MainContentPanel extends JPanel implements ActionListener {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private final LinkedList<MainContentPanelPlugin> pluginList = new LinkedList<MainContentPanelPlugin>();

    private final JSplitPane splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false);
    private Component mainComponent = null;
    private final JPanel pluginContainer = new JPanel();
    private final CollapsiblePane collapsiblePane = new CollapsiblePane("Plugins", pluginContainer, true);

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    public MainContentPanel() {
        initVisualComponents();
    }

    /**
     * Initialize the visual parts of the component.
     */
    private void initVisualComponents() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension());
        add(splitpane, BorderLayout.CENTER);

        pluginContainer.setLayout(new BorderLayout());
        pluginContainer.setPreferredSize(new Dimension(pluginContainer.getWidth(), (int) (getHeight() * 0.66)));
        pluginContainer.setSize(pluginContainer.getPreferredSize());

        splitpane.setResizeWeight(0.66);
        splitpane.setOneTouchExpandable(false);
        splitpane.setDividerSize(5);
        // splitpane.setContinuousLayout(true);

        collapsiblePane.toggleButton.addActionListener(this);

        add(splitpane, BorderLayout.CENTER);
    }

    /**
     * Set the main component to the container, e.g. the main image panel.
     * 
     * @param comp
     *            Main component to be displayed.
     * */
    public boolean setMainComponent(final Component comp) {
        if (comp == null || mainComponent == comp) {
            return false;
        }

        if (mainComponent != null) {
            splitpane.remove(mainComponent);
            remove(mainComponent);
        }

        mainComponent = comp;

        if (mainComponent != null) {
            splitpane.setTopComponent(mainComponent);
        }
        return true;
    }

    /**
     * Get the main component.
     * */
    public final Component getMainComponent() {
        return mainComponent;
    }

    /**
     * Adds a plug-in and the associated GUI to the container. The GUI will be
     * displayed below the main component.
     * 
     * @param plugin
     *            Plugin to be added to the container.
     * */
    public boolean addPlugin(final MainContentPanelPlugin plugin) {
        if (plugin == null || pluginList.contains(plugin)) {
            return false;
        }

        if (!pluginList.add(plugin)) {
            return false;
        }

        updateLayout();

        return true;
    }

    /**
     * Removes a plug-in and the associated GUI from the container.
     * 
     * @param plugin
     *            Plugin to be removed from the container.
     * */
    public boolean removePlugin(final MainContentPanelPlugin plugin) {
        if (!pluginList.remove(plugin)) {
            return false;
        }

        updateLayout();

        return true;
    }

    /**
     * Updates the layout of the container and its sub components. Plug-ins will
     * be displayed, if available, in separated tabs below the main component
     * area. An split pane will be provided, if necessary, to readjust the
     * height of the components.
     * */
    private void updateLayout() {
        splitpane.remove(collapsiblePane);
        remove(collapsiblePane);

        splitpane.setDividerSize(0);

        if (collapsiblePane.toggleButton.isSelected()) {
            pluginContainer.removeAll();

            if (pluginList.size() == 1 && pluginList.get(0).getVisualInterfaces().size() == 1) {
                pluginContainer.add(pluginList.get(0).getVisualInterfaces().get(0), BorderLayout.CENTER);
                collapsiblePane.setTitle(pluginList.get(0).getTabName());
                collapsiblePane.setMinimumSize(new Dimension(200, 180));
                splitpane.setBottomComponent(collapsiblePane);
                splitpane.setDividerSize(5);
            } else if (!(pluginList.size() == 1 && pluginList.get(0).getVisualInterfaces().size() == 0) && pluginList.size() > 0) {
                JTabbedPane tabbedPane = new JTabbedPane();

                for (MainContentPanelPlugin plugin : pluginList) {
                    for (JComponent component : plugin.getVisualInterfaces()) {
                        tabbedPane.addTab(plugin.getTabName(), component);
                    }
                }

                pluginContainer.add(tabbedPane, BorderLayout.CENTER);
                collapsiblePane.setTitle("Plugins");

                splitpane.setBottomComponent(collapsiblePane);
                splitpane.setDividerSize(5);
            }
        } else {
            add(collapsiblePane, BorderLayout.PAGE_END);
        }
        revalidate();
        repaint();
    }

    // ////////////////////////////////////////////////////////////////
    // Action Listener
    // ////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * */
    @Override
    public void actionPerformed(ActionEvent e) {
        updateLayout();
        Log.warn("Event " + e);
    }

}
