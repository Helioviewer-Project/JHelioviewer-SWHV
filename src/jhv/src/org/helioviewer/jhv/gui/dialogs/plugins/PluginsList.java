package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
class PluginsList extends JScrollPane {

    private static final Color selectionBackgroundColor = new JList<JPanel>().getSelectionBackground();
    private static final Color selectionForegroundColor = new JList<JPanel>().getSelectionForeground();

    private final LinkedList<PluginsListEntryChangeListener> listeners = new LinkedList<>();
    private final HashMap<String, PluginsListEntry> entryMap = new HashMap<>();

    private final JPanel contentPane = new JPanel();

    private String selectedEntryName = null;

    public PluginsList() {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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

    /**
     * Removes all entries from the list, sorts the items in alphabetical order
     * and puts for each item a new entry to the list.
     * */
    private void updateList() {
        contentPane.removeAll();

        String[] pluginNames = entryMap.keySet().toArray(new String[0]);
        Arrays.sort(pluginNames);

        for (String name : pluginNames) {
            contentPane.add(entryMap.get(name));
        }

        if (pluginNames.length > 0) {
            selectItem(selectedEntryName);
        }

        fireListChanged();
    }

    /**
     * Adds a new item to the list.
     * */
    public void addEntry(String name, PluginsListEntry entry) {
        if (name == null || entry == null) {
            return;
        }

        entryMap.put(name, entry);
        updateList();
    }

    /**
     * Removes an item from the list.
     * */
    public void removeEntry(String name) {
        if (name == null) {
            return;
        }

        entryMap.remove(name);
        updateList();
    }

    /**
     * Removes all entries from the list.
     * */
    public void removeAllEntries() {
        entryMap.clear();
        updateList();
    }

    /**
     * Selects the first entry of the list.
     * */
    public void selectFirstItem() {
        selectItem(null);
    }

    /**
     * Selects the corresponding entry of the given name.
     * */
    public void selectItem(String pluginName) {
        String newSelectedEntryName = pluginName;

        if (pluginName == null || !entryMap.keySet().contains(pluginName)) {
            String[] pluginNames = entryMap.keySet().toArray(new String[0]);
            Arrays.sort(pluginNames);

            if (pluginNames.length == 0) {
                return;
            }
            newSelectedEntryName = pluginNames[0];
        }

        // deselect all
        for (PluginsListEntry entry : entryMap.values()) {
            entry.setForeground(Color.BLACK);
            entry.setBackground(Color.WHITE);
        }

        // select given one
        selectedEntryName = newSelectedEntryName;
        PluginsListEntry selected = entryMap.get(newSelectedEntryName);
        if (selected != null) {
            selected.setForeground(selectionForegroundColor);
            selected.setBackground(selectionBackgroundColor);
        }

        contentPane.revalidate();
    }

    /**
     * Returns the selected entry.
     * */
    public PluginsListEntry getSelectedEntry() {
        return entryMap.get(selectedEntryName);
    }

    /**
     * Returns the number of items within the list.
     * */
    public int getNumberOfItems() {
        return entryMap.size();
    }

    /**
     * Informs all registered listeners that an item has changed.
     * */
    public void fireItemChanged() {
        for (PluginsListEntryChangeListener listener : listeners) {
            listener.itemChanged();
        }
    }

    /**
     * Informs all registered listeners that the list has changed.
     * */
    private void fireListChanged() {
        for (PluginsListEntryChangeListener listener : listeners) {
            listener.listChanged();
        }
    }

}
