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
import java.io.File;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * Visual list entry for each plug-in. Provides functions to
 * enable/disable a plug-in and to display additional information about the
 * plug-in.
 * 
 * @author Stephan Pagel
 * */
@SuppressWarnings("serial")
class PluginsListEntry extends JPanel implements MouseListener {

    private final PluginContainer plugin;
    private final PluginsList list;

    private final LinkLabel infoLabel = new LinkLabel("More");

    private final JLabel preferencesLabel = new JLabel();
    private final JLabel enableLabel = new JLabel();
    private final JLabel removeLabel = new JLabel();

    public PluginsListEntry(PluginContainer plugin, PluginsList list) {
        this.plugin = plugin;
        this.list = list;

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
        // buttonPane.add(preferencesLabel);
        buttonPane.add(enableLabel);
        // buttonPane.add(removeLabel);

        preferencesLabel.setIcon(IconBank.getIcon(JHVIcon.ADD));
        preferencesLabel.setToolTipText("Shows up the preference dialog of the plug-in.");

        removeLabel.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
        removeLabel.setToolTipText("Removes the plug-in from JHelioviewer and your file system.");

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
        preferencesLabel.addMouseListener(this);
        enableLabel.addMouseListener(this);
        removeLabel.addMouseListener(this);
    }

    private String getTitleText() {
        Font font = new JPanel().getFont();
        return "<html><font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">" +
                "<b>" + plugin.getName() + "</b></font></html>";
    }

    private String getDescriptionText() {
        return plugin.getDescription() == null ? "" : plugin.getDescription();
    }

    /**
     * Updates the icon and tool tip text of the activation "button".
     * */
    private void updateEnableLabel() {
        if (plugin.isActive()) {
            enableLabel.setIcon(IconBank.getIcon(JHVIcon.CONNECTED));
            enableLabel.setToolTipText("Disable plug-in");
        } else {
            enableLabel.setIcon(IconBank.getIcon(JHVIcon.DISCONNECTED));
            enableLabel.setToolTipText("Enable plug-in");
        }
    }

    /**
     * Return the corresponding plug-in object.
     * */
    public PluginContainer getPluginContainer() {
        return plugin;
    }

    /**
     * Shows up the about dialog containing additional information about the
     * corresponding plug-in.
     * */
    private void showAboutDialog() {
        PluginAboutDialog.showDialog(plugin.getPlugin());
    }

    /**
     * Shows up the preferences dialog of the corresponding plug-in.
     * */
    private void showPreferencesDialog() {
    }

    /**
     * Enables or disables the corresponding plug-in.
     * */
    private void setPluginActive(boolean active) {
        if (plugin.isActive() == active) {
            return;
        }

        plugin.setActive(active);
        plugin.changeSettings();

        updateEnableLabel();
        list.fireItemChanged();
    }

    /**
     * Remove the plug-in and deletes the plug-in file of selected entry.
     */
    private void deletePlugin() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure to delete the selected plug-in permanently from your system?", "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (!PluginManager.getSingletonInstance().deletePlugin(plugin, new File(JHVDirectory.PLUGINS.getPath() + JHVGlobals.TEMP_FILENAME_DELETE_PLUGIN_FILES))) {
                Message.err("An error occured while deleting the plugin file!", "Please check manually!", false);
            }
            list.removeEntry(plugin.getName());
            list.fireItemChanged();
        }
    }

    // Mouse Listener

    public void mouseClicked(MouseEvent e) {
        list.selectItem(plugin.getName());

        if (e.getSource().equals(infoLabel)) {
            showAboutDialog();
        } else if (e.getSource().equals(preferencesLabel)) {
            showPreferencesDialog();
        } else if (e.getSource().equals(enableLabel)) {
            setPluginActive(!plugin.isActive());
        } else if (e.getSource().equals(removeLabel)) {
            deletePlugin();
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    // Link Label
    private static class LinkLabel extends JLabel {

        public LinkLabel(String text) {
            setText("<html><u>" + text + "</u></html>");
            setForeground(Color.BLUE);

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                public void mouseExited(MouseEvent e) {
                    setCursor(Cursor.getDefaultCursor());
                }
            });
        }

    }

}
