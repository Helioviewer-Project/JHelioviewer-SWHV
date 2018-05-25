package org.helioviewer.jhv.plugins;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.CloseButtonPanel;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class PluginsDialog extends StandardDialog implements ShowableDialog {

    public PluginsDialog() {
        super(ImageViewerGui.getMainFrame(), "Plug-in Manager", true);
        setResizable(false);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        return new CloseButtonPanel(this);
    }

    @Override
    public JComponent createContentPanel() {
        PluginsList pluginList = new PluginsList();
        JComponent component = pluginList.isEmpty() ? new JLabel("No plug-ins available") : new JScrollPane(pluginList);
        component.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return component;
    }

    @Nullable
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

}
