package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

/**
 * Visual list entry for each plug-in. Provides functions to enable/disable
 * a plug-in and to display additional information about the plug-in.
 * */
@SuppressWarnings("serial")
class PluginsListEntry extends JPanel implements MouseListener {

    private final PluginContainer plugin;
    private final PluginsList list;

    private final LinkLabel infoLabel = new LinkLabel("More");
    private final JLabel enableLabel = new JLabel();

    public PluginsListEntry(PluginContainer _plugin, PluginsList _list) {
        plugin = _plugin;
        list = _list;

        // title
        JEditorPane titlePane = new JEditorPane("text/html", getTitleText());
        titlePane.setEditable(false);
        titlePane.setOpaque(false);

        // description
        JLabel descLabel = new JLabel(getDescriptionText());
        JPanel descPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descPane.setOpaque(false);
        descPane.add(descLabel);
        descPane.add(infoLabel);

        // "buttons"
        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPane.setOpaque(false);
        buttonPane.add(enableLabel);

        updateEnableLabel();

        // general
        setLayout(new BorderLayout());
        add(titlePane, BorderLayout.PAGE_START);
        add(descPane, BorderLayout.LINE_START);
        add(buttonPane, BorderLayout.LINE_END);

        int height = titlePane.getPreferredSize().height + buttonPane.getPreferredSize().height;
        setMinimumSize(new Dimension(getMinimumSize().width, height));
        setMaximumSize(new Dimension(getMaximumSize().width, height));

        addMouseListener(this);
        titlePane.addMouseListener(this);
        descPane.addMouseListener(this);
        descLabel.addMouseListener(this);
        infoLabel.addMouseListener(this);
        buttonPane.addMouseListener(this);
        enableLabel.addMouseListener(this);
    }

    private String getTitleText() {
        Font font = new JPanel().getFont();
        return "<html><font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">" +
                "<b>" + plugin.getName() + "</b></font></html>";
    }

    private String getDescriptionText() {
        return plugin.getDescription() == null ? "" : plugin.getDescription();
    }

    private void updateEnableLabel() {
        if (plugin.isActive()) {
            enableLabel.setText(Buttons.plugOn);
            enableLabel.setToolTipText("Disable plug-in");
        } else {
            enableLabel.setText(Buttons.plugOff);
            enableLabel.setToolTipText("Enable plug-in");
        }
    }

    private void setPluginActive(boolean active) {
        if (plugin.isActive() == active)
            return;

        plugin.setActive(active);
        plugin.changeSettings();
        PluginManager.getSingletonInstance().saveSettings();

        updateEnableLabel();
        list.fireListChanged();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        list.selectItem(plugin.getName());

        if (e.getSource().equals(infoLabel)) {
            Plugin p = plugin.getPlugin();
            String name = p.getName() == null ? "Unknown plug-in name" : p.getName();
            String desc = p.getDescription() == null ? "No description available" : p.getDescription();
            String license = p.getAboutLicenseText() == null ? "Unknown license" : p.getAboutLicenseText();
            String text = "<center><p><big><b>" + name + "</b></big></p><p><b>Plug-in description</b><br>" + desc + "</p><p><b>Plug-in license information</b><br>" + license;
            new TextDialog("About", text, false).showDialog();
        } else if (e.getSource().equals(enableLabel)) {
            setPluginActive(!plugin.isActive());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    private static class LinkLabel extends JLabel {

        public LinkLabel(String text) {
            setText("<html><u>" + text + "</u></html>");
            setForeground(Color.BLUE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setCursor(Cursor.getDefaultCursor());
                }
            });
        }

    }

}
