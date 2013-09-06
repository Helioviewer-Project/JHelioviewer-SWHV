package org.helioviewer.jhv.gui.dialogs.plugins;

/**
 * Listener of changes within {@link List}.
 * 
 * @author Stephan Pagel
 * */
public interface ListEntryChangeListener {

    /**
     * Something changed within an instance of {@link List}.
     * */
    public void listChanged();

    /**
     * An item of an instance of {@link List} has been changed.
     * */
    public void itemChanged();
}
