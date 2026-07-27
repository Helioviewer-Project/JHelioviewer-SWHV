package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.MainFrame;

// This panel acts as a container for the GUI elements which are shown in the
// main area of the application. Usually it contains the main image area. Below
// the main image area plug-ins are able to display their GUI components.
@SuppressWarnings("serial")
public final class MainContentPanel extends JPanel {

    private static final int DIVIDER_SIZE = 5;
    private static final double NORMAL_RESIZE_WEIGHT = 0.75;

    private final ArrayList<Interfaces.MainContentPanelPlugin> pluginList = new ArrayList<>();

    private final JSplitPane splitPane;
    private final JPanel pluginContainer;
    private final CollapsiblePane collapsiblePane;
    private final CollapsiblePaneButton maximizeButton;

    private boolean pluginMaximized;
    private int normalDividerLocation;

    public MainContentPanel(Component mainComponent) {
        pluginContainer = new JPanel(new BorderLayout());
        collapsiblePane = new CollapsiblePane("Plugins", pluginContainer, !"false".equals(Settings.getProperty("display.plugins")));
        collapsiblePane.toggleButton.addActionListener(e -> updateLayout());

        maximizeButton = new CollapsiblePaneButton();
        maximizeButton.setFont(Buttons.getMaterialFont(12));
        maximizeButton.setHorizontalAlignment(SwingConstants.CENTER);
        maximizeButton.addActionListener(e -> togglePluginMaximized());
        collapsiblePane.addHeaderComponent(maximizeButton);
        updateMaximizeButton();

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(0);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
        splitPane.setResizeWeight(NORMAL_RESIZE_WEIGHT);

        splitPane.setTopComponent(mainComponent);

        setLayout(new BorderLayout());
        setMinimumSize(new Dimension());
        add(splitPane, BorderLayout.CENTER);
    }

    // Adds a plug-in and the associated GUI to the container. The GUI will be displayed below the main component.
    public void addPlugin(Interfaces.MainContentPanelPlugin plugin) {
        if (plugin == null || pluginList.contains(plugin) || plugin.getVisualInterfaces().isEmpty()) {
            return;
        }
        ComponentUtils.setVisible(plugin.getVisualInterfaces().getFirst(), collapsiblePane.toggleButton.isSelected());
        pluginList.add(plugin);
        updateLayout();
    }

    // Removes a plug-in and the associated GUI from the container
    public void removePlugin(Interfaces.MainContentPanelPlugin plugin) {
        if (pluginList.remove(plugin)) {
            if (!plugin.getVisualInterfaces().isEmpty())
                ComponentUtils.setVisible(plugin.getVisualInterfaces().getFirst(), false);
            updateLayout();
        }
    }

    // Updates the layout of the container and its subcomponents. Plug-ins will
    // be displayed, if available, in separated tabs below the main component
    // area. A split pane will be provided, if necessary, to readjust the
    // height of the components.
    private void updateLayout() {
        if ((pluginList.isEmpty() || !collapsiblePane.toggleButton.isSelected()) && pluginMaximized)
            restorePluginSize();

        splitPane.remove(collapsiblePane);
        remove(collapsiblePane);
        splitPane.setDividerSize(0);

        if (pluginList.isEmpty()) {
            pluginContainer.removeAll();
            revalidate();
            repaint();
            return;
        }

        boolean isSelected = collapsiblePane.toggleButton.isSelected();
        boolean onePlugin = pluginList.size() == 1 && pluginList.getFirst().getVisualInterfaces().size() == 1;
        collapsiblePane.setTitle(onePlugin ? pluginList.getFirst().getTabName() : "Plugins");

        if (isSelected) {
            pluginContainer.removeAll();

            if (onePlugin) {
                pluginContainer.add(pluginList.getFirst().getVisualInterfaces().getFirst(), BorderLayout.CENTER);
            } else {
                JTabbedPane tabbedPane = new JTabbedPane();
                for (Interfaces.MainContentPanelPlugin plugin : pluginList) {
                    for (JComponent component : plugin.getVisualInterfaces()) {
                        tabbedPane.addTab(plugin.getTabName(), component);
                    }
                }
                pluginContainer.add(tabbedPane, BorderLayout.CENTER);
            }
            splitPane.setBottomComponent(collapsiblePane);
            if (pluginMaximized) {
                splitPane.setDividerLocation(0);
            } else {
                splitPane.setDividerSize(DIVIDER_SIZE);
            }
        } else {
            add(collapsiblePane, BorderLayout.PAGE_END);
        }
        maximizeButton.setVisible(isSelected);
        Settings.setProperty("display.plugins", Boolean.toString(isSelected));

        revalidate();
        repaint();
    }

    private void togglePluginMaximized() {
        if (pluginMaximized) {
            restorePluginSize();
        } else {
            normalDividerLocation = splitPane.getDividerLocation();
            MainFrame.setRenderSurfaceVisible(false);
            splitPane.setResizeWeight(0);
            splitPane.setDividerSize(0);
            splitPane.setDividerLocation(0);
            pluginMaximized = true;
            updateMaximizeButton();
            revalidate();
            repaint();
        }
    }

    private void restorePluginSize() {
        splitPane.setResizeWeight(NORMAL_RESIZE_WEIGHT);
        splitPane.setDividerSize(DIVIDER_SIZE);
        splitPane.setDividerLocation(normalDividerLocation);
        pluginMaximized = false;
        updateMaximizeButton();
        revalidate();
        repaint();
        EventQueue.invokeLater(() -> {
            if (!pluginMaximized)
                MainFrame.setRenderSurfaceVisible(true);
        });
    }

    private void updateMaximizeButton() {
        maximizeButton.setSelected(true);
        maximizeButton.setText((pluginMaximized ? MaterialDesign.CHEVRON_DOWN : MaterialDesign.CHEVRON_UP).toString());
        maximizeButton.setToolTipText(pluginMaximized ? "Restore panel size" : "Maximize panel");
    }

}
