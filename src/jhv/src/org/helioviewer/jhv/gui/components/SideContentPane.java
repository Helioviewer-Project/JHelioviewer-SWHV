package org.helioviewer.jhv.gui.components;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Panel managing multiple {@link CollapsiblePane}s.
 *
 * This panel hides the use of the {@link CollapsiblePane} and allows accessing
 * the children of the {@link CollapsiblePane} directly.
 */
@SuppressWarnings("serial")
public class SideContentPane extends JComponent {

    private final HashMap<Component, CollapsiblePane> map = new HashMap<Component, CollapsiblePane>();
    private final JPanel dummy = new JPanel();

    public SideContentPane() {
        setLayout(new GridBagLayout());
        add(dummy);
    }

    public void add(String title, Component component, boolean startExpanded) {
        remove(dummy);

        CollapsiblePane newPane = new CollapsiblePane(title, component, startExpanded);
        map.put(component, newPane);

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0.;
        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        add(newPane, c);

        c.weighty = 1.;
        add(dummy, c);
    }

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
