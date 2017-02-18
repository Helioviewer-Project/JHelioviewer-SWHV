package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.Color;
import java.util.LinkedList;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
class PluginsList extends JScrollPane {

    private static final Color selectionBackgroundColor = new JList<JPanel>().getSelectionBackground();
    private static final Color selectionForegroundColor = new JList<JPanel>().getSelectionForeground();

    private final LinkedList<PluginsListEntryChangeListener> listeners = new LinkedList<>();
    private final TreeMap<String, PluginsListEntry> entryMap = new TreeMap<>();
    private final JPanel contentPane = new JPanel();

    private String selectedEntryName = null;

    public PluginsList() {
        setViewportView(contentPane);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBackground(Color.WHITE);
    }

    public void addListEntryChangeListener(PluginsListEntryChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListEntryChangeListener(PluginsListEntryChangeListener listener) {
        listeners.remove(listener);
    }

    public void updateList() {
        contentPane.removeAll();

        for (PluginsListEntry entry : entryMap.values()) {
            contentPane.add(entry);
        }
        selectItem(selectedEntryName);

        contentPane.revalidate();
        contentPane.repaint();
    }

    public void addEntry(String name, PluginsListEntry entry) {
        entryMap.put(name, entry);
    }

    public void removeAllEntries() {
        entryMap.clear();
    }

    public void selectItem(String pluginName) {
        if (entryMap.values().isEmpty())
            return;

        // deselect all
        for (PluginsListEntry entry : entryMap.values()) {
            entry.setForeground(Color.BLACK);
            entry.setBackground(Color.WHITE);
        }

        if (pluginName != null && entryMap.containsKey(pluginName)) {
            selectedEntryName = pluginName;
            PluginsListEntry selected = entryMap.get(selectedEntryName);
            selected.setForeground(selectionForegroundColor);
            selected.setBackground(selectionBackgroundColor);
        }
    }

    public int getNumberOfItems() {
        return entryMap.size();
    }

    public void fireListChanged() {
        for (PluginsListEntryChangeListener listener : listeners) {
            listener.listChanged();
        }
    }

}
