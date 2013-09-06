package org.helioviewer.viewmodelplugin.overlay;

import java.util.AbstractList;
import java.util.LinkedList;

/**
 * This manager collects all overlay control components. Later on it is possible
 * to get all control elements and add them to a GUI area.
 * 
 * @author Stephan Pagel
 */
public class OverlayControlComponentManager {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private LinkedList<OverlayControlComponent> list = new LinkedList<OverlayControlComponent>();

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Adds a component to the manager.
     * 
     * @param comp
     *            Component to add.
     */
    public void add(OverlayControlComponent comp) {
        list.add(comp);
    }

    /**
     * Returns a list with all control components.
     * 
     * @return list with all control components.
     */
    public AbstractList<OverlayControlComponent> getAllControlComponents() {
        return list;
    }
}
