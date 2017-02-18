package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.CloseButtonPanel;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class PluginsDialog extends StandardDialog implements ShowableDialog, PluginsListEntryChangeListener {

    private final PluginsList pluginList = new PluginsList();
    private final CardLayout listLayout = new CardLayout();
    private final JPanel listContainerPane = new JPanel(listLayout);

    public PluginsDialog() {
        super(ImageViewerGui.getMainFrame(), "Plug-in Manager", true);
        setResizable(false);

        JLabel emptyLabel = new JLabel("No plug-ins available", JLabel.CENTER);
        JScrollPane emptyScrollPane = new JScrollPane(emptyLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
        emptyLabel.setHorizontalTextPosition(JLabel.CENTER);
        emptyLabel.setOpaque(true);
        emptyLabel.setBackground(Color.WHITE);

        listContainerPane.add(emptyScrollPane, "empty");
        listContainerPane.add(pluginList, "list");

        pluginList.addListEntryChangeListener(this);
        updatePluginList();
    }

    @Override
    public ButtonPanel createButtonPanel() {
        return new CloseButtonPanel(this);
    }

    @Override
    public JComponent createContentPanel() {
        listContainerPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return listContainerPane;
    }

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    private void updatePluginList() {
        pluginList.removeAllEntries();
        for (PluginContainer plugin : PluginManager.getSingletonInstance().getAllPlugins()) {
            pluginList.addEntry(plugin.getName(), new PluginsListEntry(plugin, pluginList));
        }
        pluginList.updateList();
        if (pluginList.getNumberOfItems() > 0) {
            listLayout.show(listContainerPane, "list");
        } else {
            listLayout.show(listContainerPane, "empty");
        }
    }

    @Override
    public void listChanged() {
        updatePluginList();
    }

}
