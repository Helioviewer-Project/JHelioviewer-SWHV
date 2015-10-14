package org.helioviewer.jhv.gui.components;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Panel managing multiple {@link CollapsiblePane}s.
 *
 * This panel hides the use of the {@link CollapsiblePane} and allows accessing
 * the children of the {@link CollapsiblePane} directly.
 *
 * @author Markus Langenberg
 */
@SuppressWarnings("serial")
public class SideContentPane extends JComponent {

    private final HashMap<Component, CollapsiblePane> map = new HashMap<Component, CollapsiblePane>();
    private final JPanel dummy = new JPanel();

    public SideContentPane() {
        setLayout(new GridBagLayout());
        add(dummy);
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
        remove(dummy);
        CollapsiblePane newPane = new CollapsiblePane(title, component, startExpanded);
        map.put(component, newPane);
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 0.;
        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        if (index < 0) {
            c.gridy = getComponentCount();
            add(newPane, c);
        } else {
            c.gridy = index;
            add(newPane, c);
        }
        c.weighty = 1.;
        c.gridy = c.gridy + 1;
        add(dummy, c);
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
    @Override
    public void remove(Component component) {
        if (map.containsKey(component)) {
            super.remove(map.get(component));
            map.remove(component);
        } else {
            super.remove(component);
        }
    }

}
