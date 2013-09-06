package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Represents a visual list which manages inherited components from {@link AbstractListEntry). 
 * 
 * @author Stephan Pagel
 * */
public class List extends JScrollPane {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private final LinkedList<ListEntryChangeListener> listeners = new LinkedList<ListEntryChangeListener>();

    private final Color selectionBackgroundColor = new JList().getSelectionBackground();
    private final Color selectionForegroundColor = new JList().getSelectionForeground();
    private final HashMap<String, AbstractListEntry> entryMap = new HashMap<String, AbstractListEntry>();

    private final JPanel contentPane = new JPanel();

    private String selectedEntryName = null;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    public List() {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        initVisualComponents();
    }

    /**
     * Initialize the visual parts of the component.
     */
    private void initVisualComponents() {
        setViewportView(contentPane);

        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBackground(Color.WHITE);
    }

    /**
     * Adds the given {@link ListEntryChangeListener} to the list.
     * */
    public void addListEntryChangeListener(final ListEntryChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given {@link ListEntryChangeListener} from the list.
     * */
    public void removeListEntryChangeListener(final ListEntryChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Removes all entries from the list, sorts the items in alphabetical order
     * and puts for each item a new entry to the list.
     * */
    private void updateList() {
        contentPane.removeAll();

        final String[] pluginNames = entryMap.keySet().toArray(new String[0]);
        Arrays.sort(pluginNames);

        for (final String name : pluginNames) {
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
    public void addEntry(final String name, final AbstractListEntry entry) {
        if (name == null || entry == null) {
            return;
        }

        entryMap.put(name, entry);

        updateList();
    }

    /**
     * Removes an item from the list.
     * */
    public void removeEntry(final String name) {
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
    public void selectItem(final String pluginName) {
        String newSelectedEntryName = pluginName;

        if (pluginName == null || !entryMap.keySet().contains(pluginName)) {
            final String[] pluginNames = entryMap.keySet().toArray(new String[0]);
            Arrays.sort(pluginNames);

            if (pluginNames.length == 0) {
                return;
            }

            newSelectedEntryName = pluginNames[0];
        }

        // deselect all
        for (final AbstractListEntry entry : entryMap.values()) {
            entry.setForeground(Color.BLACK);
            entry.setBackground(Color.WHITE);
        }

        // select given one
        selectedEntryName = newSelectedEntryName;
        final AbstractListEntry selected = entryMap.get(newSelectedEntryName);

        if (selected != null) {
            selected.setForeground(selectionForegroundColor);
            selected.setBackground(selectionBackgroundColor);
        }

        contentPane.revalidate();
    }

    /**
     * Returns the selected entry.
     * */
    public AbstractListEntry getSelectedEntry() {
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
        for (final ListEntryChangeListener listener : listeners) {
            listener.itemChanged();
        }
    }

    /**
     * Informs all registered listeners that the list has changed.
     * */
    public void fireListChanged() {
        for (final ListEntryChangeListener listener : listeners) {
            listener.listChanged();
        }
    }
}
