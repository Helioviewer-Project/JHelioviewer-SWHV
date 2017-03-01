package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.Color;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;

@SuppressWarnings("serial")
class PluginsList extends JPanel {

    private static final Color selectionBackgroundColor = new JList<JPanel>().getSelectionBackground();
    private static final Color selectionForegroundColor = new JList<JPanel>().getSelectionForeground();
    private static final Color backgroundColor = new JList<JPanel>().getBackground();
    private static final Color foregroundColor = new JList<JPanel>().getForeground();

    private final TreeMap<String, PluginsListEntry> entryMap = new TreeMap<>();

    public PluginsList() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        PluginContainer[] pluginArray = PluginManager.getSingletonInstance().getAllPlugins();
        for (PluginContainer plugin : pluginArray)
            entryMap.put(plugin.getName(), new PluginsListEntry(plugin, this));
        for (PluginsListEntry entry : entryMap.values())
            add(entry);
        if (pluginArray.length != 0)
            selectItem(pluginArray[0].getName());
    }

    void selectItem(String name) {
        // deselect all
        for (PluginsListEntry entry : entryMap.values()) {
            entry.setForeground(foregroundColor);
            entry.setBackground(backgroundColor);
        }

        if (entryMap.containsKey(name)) {
            PluginsListEntry selected = entryMap.get(name);
            selected.setForeground(selectionForegroundColor);
            selected.setBackground(selectionBackgroundColor);
        }
    }

    boolean isEmpty() {
        return entryMap.isEmpty();
    }

}
