package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.Color;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.plugin.PluginContainer;
import org.helioviewer.jhv.base.plugin.PluginManager;

@SuppressWarnings("serial")
class PluginsList extends JPanel {

    private static final Color selectionBackgroundColor = new JList<JPanel>().getSelectionBackground();
    private static final Color selectionForegroundColor = new JList<JPanel>().getSelectionForeground();
    private static final Color backgroundColor = new JList<JPanel>().getBackground();
    private static final Color foregroundColor = new JList<JPanel>().getForeground();

    private final TreeMap<String, PluginsListEntry> pluginsMap = new TreeMap<>();

    PluginsList() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        for (PluginContainer plugin : PluginManager.getSingletonInstance().getAllPlugins())
            pluginsMap.put(plugin.getName(), new PluginsListEntry(plugin, this));

        boolean first = true;
        for (Map.Entry<String, PluginsListEntry> entry : pluginsMap.entrySet()) { // add sorted
            add(entry.getValue());
            if (first) {
                first = false;
                selectItem(entry.getKey());
            }
        }
    }

    void selectItem(String name) {
        // deselect all
        for (PluginsListEntry entry : pluginsMap.values()) {
            entry.setForeground(foregroundColor);
            entry.setBackground(backgroundColor);
        }

        PluginsListEntry selected = pluginsMap.get(name);
        if (selected != null) {
            selected.setForeground(selectionForegroundColor);
            selected.setBackground(selectionBackgroundColor);
        }
    }

    boolean isEmpty() {
        return pluginsMap.isEmpty();
    }

}
