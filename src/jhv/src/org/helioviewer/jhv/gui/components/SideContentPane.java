package org.helioviewer.jhv.gui.components;

import java.awt.Component;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;

/**
 * Panel managing multiple {@link CollapsiblePane}s.
 * 
 * This panel hides the use of the {@link CollapsiblePane} and allows accessing
 * the children of the {@link CollapsiblePane} directly.
 * 
 * @author Markus Langenberg
 */
public class SideContentPane extends JComponent {

    private static final long serialVersionUID = 1L;

    private HashMap<Component, CollapsiblePane> map = new HashMap<Component, CollapsiblePane>();

    /**
     * Default constructor.
     */
    public SideContentPane() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createGlue());
    }

    /**
     * Add new component into a new {@link CollapsiblePane}.
     * 
     * @param title
     *            Text on the toggle button
     * @param component
     *            Component to manage
     * @param index
     *            the position in the container's list at which to insert the
     *            component; -1 means insert at the end component
     */
    public void add(String title, Component component, int index) {
        add(title, component, index, true);
    }

    /**
     * Add new component into a new {@link CollapsiblePane} at the end.
     * 
     * @param title
     *            Text on the toggle button
     * @param component
     *            Component to manage
     * @param startExpanded
     *            if true, the component will be visible right from the start
     */
    public void add(String title, Component component, boolean startExpanded) {
        add(title, component, -1, startExpanded);
    }
    /**
     * Add new component into a new {@link CollapsiblePane} at the end.
     * 
     * @param title
     *            Text on the toggle button
     * @param component
     *            Component to manage
     * @param startExpanded
     *            if true, the component will be visible right from the start
     */
    public void addWithButton(String title, Component component, boolean startExpanded) {
    	addWithButton(title, component, -1, startExpanded);
    }
    /**
     * Add new component into a new {@link CollapsiblePane}.
     * 
     * @param title
     *            Text on the toggle button
     * @param component
     *            Component to manage
     * @param index
     *            the position in the container's list at which to insert the
     *            component; -1 means insert at the end component
     * @param startExpanded
     *            if true, the component will be visible right from the start
     */
    public void add(String title, Component component, int index, boolean startExpanded) {
        CollapsiblePane newPane = new CollapsiblePane(title, component, startExpanded);
        map.put(component, newPane);

        if (index < 0) {
            add(newPane, getComponentCount() - 1);
        } else {
            add(newPane, index);
        }
    }
    /**
     * Add new component into a new {@link CollapsiblePane}.
     * 
     * @param title
     *            Text on the toggle button
     * @param component
     *            Component to manage
     * @param index
     *            the position in the container's list at which to insert the
     *            component; -1 means insert at the end component
     * @param startExpanded
     *            if true, the component will be visible right from the start
     */
    public void addWithButton(String title, Component component, int index, boolean startExpanded) {
        CollapsiblePane newPane = new CollapsiblePaneWithButton(title, component, startExpanded);
        map.put(component, newPane);

        if (index < 0) {
            add(newPane, getComponentCount() - 1);
        } else {
            add(newPane, index);
        }
    }
    /**
     * Expands the given component.
     * 
     * This function works if the given component is either a
     * {@link CollapsiblePane} itself or is the child of a
     * {@link CollapsiblePane}.
     * 
     * @param component
     *            Component to expand
     */
    public void expand(Component component) {
        if (map.containsKey(component)) {
            map.get(component).expand();
        } else {
            for (Component member : getComponents()) {
                if (member == component && component instanceof CollapsiblePane) {
                    ((CollapsiblePane) component).expand();
                }
            }
        }
    }

    /**
     * Expands all children of {@link CollapsiblePane}s of the given class.
     * 
     * @param c
     *            Pattern, which members should be expanded
     */
    public <T extends Component> void expand(Class<T> c) {
        for (Component member : map.keySet()) {
            if (c.isInstance(member)) {
                expand(member);
            }
        }
    }

    /**
     * Collapses the given component.
     * 
     * This function works if the given component is either a
     * {@link CollapsiblePane} itself or is the child of a
     * {@link CollapsiblePane}.
     * 
     * @param component
     *            Component to collapse
     */
    public void collapse(Component component) {
        if (map.containsKey(component)) {
            map.get(component).collapse();
        } else {
            for (Component member : getComponents()) {
                if (member == component && component instanceof CollapsiblePane) {
                    ((CollapsiblePane) component).collapse();
                }
            }
        }
    }

    /**
     * Collapses all children of {@link CollapsiblePane}s of the given class.
     * 
     * @param c
     *            Pattern, which members should be collapsed
     */
    public <T extends Component> void collapse(Class<T> c) {
        for (Component member : map.keySet()) {
            if (c.isInstance(member)) {
                collapse(member);
            }
        }
    }

    /**
     * Removes all CollapsiblePanes which includes an instance of the given
     * class.
     * 
     * @param c
     *            Pattern, which members should be removed
     */
    public <T extends Component> void remove(Class<T> c) {
        // go through map to find all effected collapsible panes
        Object[] components = map.keySet().toArray();
        for (int i = 0; i < components.length; i++) {
            if (c.isInstance(components[i])) {
                remove((Component) components[i]);
            }
        }

        // go through sub components of the side panel in case a component is
        // not added to a collapsible pane
        for (Component member : getComponents()) {
            if (c.isInstance(member))
                remove(member);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(Component component) {
        if (map.containsKey(component)) {
            super.remove(map.get(component));
            map.remove(component);
        } else {
            super.remove(component);
        }
    }
}
