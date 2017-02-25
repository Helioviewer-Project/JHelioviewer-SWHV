package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

@SuppressWarnings("serial")
class PluginsListEntry extends JPanel implements MouseListener, HyperlinkListener {

    private final PluginContainer plugin;
    private final PluginsList list;

    private final JLabel enableLabel = new JLabel();
    private final JTextPane pane = new JTextPane();

    public PluginsListEntry(PluginContainer _plugin, PluginsList _list) {
        plugin = _plugin;
        list = _list;

        pane.setContentType("text/html");
        pane.setText("<b>" + plugin.getName() + "</b><br>" + plugin.getDescription() + " <a href=''>More...</a>");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.addHyperlinkListener(this);
        pane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        updateEnableLabel();

        // general
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pane, BorderLayout.LINE_START);
        add(enableLabel, BorderLayout.LINE_END);

        pane.addMouseListener(this);
        enableLabel.addMouseListener(this);
    }

    @Override
    public void setBackground(Color c) {
        super.setBackground(c);
        if (pane != null)
            pane.setBackground(c);
        if (enableLabel != null)
            enableLabel.setBackground(c);
    }

    @Override
    public void setForeground(Color c) {
        super.setForeground(c);
        if (pane != null)
            pane.setForeground(c);
        if (enableLabel != null)
            enableLabel.setForeground(c);
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
        updateEnableLabel();
        PluginManager.getSingletonInstance().saveSettings();
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                Plugin p = plugin.getPlugin();
                String text = "<center><p><big><b>" + p.getName() + "</b></big></p><p><b>Plug-in description</b><br>" + p.getDescription() +
                              "</p><p><b>Plug-in license information</b><br>" + p.getAboutLicenseText();
                new TextDialog("About", text, false).showDialog();
            } else {
                JHVGlobals.openURL(e.getURL().toString());
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        list.selectItem(plugin.getName());
        if (e.getSource().equals(enableLabel)) {
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

}
