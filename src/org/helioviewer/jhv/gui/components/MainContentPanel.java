package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;

// This panel acts as a container for the GUI elements which are shown in the
// main area of the application. Usually it contains the main image area. Below
// the main image area plug-ins are able to display their GUI components.
@SuppressWarnings("serial")
public class MainContentPanel extends JPanel {

    private static final int DIVIDER_SIZE = 3;

    private final ArrayList<MainContentPanelPlugin> pluginList = new ArrayList<>();

    private final JSplitPane splitPane;
    private final JPanel pluginContainer;
    private final CollapsiblePane collapsiblePane;

    public MainContentPanel(Component mainComponent) {
        pluginContainer = new JPanel(new BorderLayout());
        collapsiblePane = new CollapsiblePane("Plugins", pluginContainer, !"false".equals(Settings.getProperty("display.plugins")));
        collapsiblePane.toggleButton.addActionListener(e -> updateLayout());

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        splitPane.setDividerSize(0);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
        splitPane.setResizeWeight(0.75);

        splitPane.setTopComponent(mainComponent);

        setLayout(new BorderLayout());
        setMinimumSize(new Dimension());
        add(splitPane, BorderLayout.CENTER);
    }

    // Adds a plug-in and the associated GUI to the container. The GUI will be displayed below the main component.
    public void addPlugin(MainContentPanelPlugin plugin) {
        if (plugin == null || pluginList.contains(plugin)) {
            return;
        }
        ComponentUtils.setVisible(plugin.getVisualInterfaces().get(0), collapsiblePane.toggleButton.isSelected());
        pluginList.add(plugin);
        updateLayout();
    }

    // Removes a plug-in and the associated GUI from the container
    public void removePlugin(MainContentPanelPlugin plugin) {
        if (pluginList.remove(plugin)) {
            ComponentUtils.setVisible(plugin.getVisualInterfaces().get(0), false);
            updateLayout();
        }
    }

    // Updates the layout of the container and its subcomponents. Plug-ins will
    // be displayed, if available, in separated tabs below the main component
    // area. A split pane will be provided, if necessary, to readjust the
    // height of the components.
    private void updateLayout() {
        splitPane.remove(collapsiblePane);
        remove(collapsiblePane);
        splitPane.setDividerSize(0);

        if (pluginList.isEmpty()) {
            revalidate();
            repaint();
            return;
        }

        boolean isSelected = collapsiblePane.toggleButton.isSelected();
        boolean onePlugin = pluginList.size() == 1 && pluginList.get(0).getVisualInterfaces().size() == 1;
        collapsiblePane.setTitle(onePlugin ? pluginList.get(0).getTabName() : "Plugins");

        if (isSelected) {
            pluginContainer.removeAll();

            if (onePlugin) {
                pluginContainer.add(pluginList.get(0).getVisualInterfaces().get(0), BorderLayout.CENTER);
            } else {
                JTabbedPane tabbedPane = new JTabbedPane();
                for (MainContentPanelPlugin plugin : pluginList) {
                    for (JComponent component : plugin.getVisualInterfaces()) {
                        tabbedPane.addTab(plugin.getTabName(), component);
                    }
                }
                pluginContainer.add(tabbedPane, BorderLayout.CENTER);
            }
            splitPane.setBottomComponent(collapsiblePane);
            splitPane.setDividerSize(DIVIDER_SIZE);
        } else {
            add(collapsiblePane, BorderLayout.PAGE_END);
        }
        Settings.setProperty("display.plugins", Boolean.toString(isSelected));

        revalidate();
        repaint();
    }

}
