package org.helioviewer.jhv.gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JPanel;

// Panel managing multiple CollapsiblePanes
// This panel hides the use of the CollapsiblePane and allows accessing
// the children of the CollapsiblePane directly.
@SuppressWarnings("serial")
public class SideContentPane extends JComponent {

    private final HashMap<JComponent, CollapsiblePane> map = new HashMap<>();
    private final JPanel dummy = new JPanel();

    public SideContentPane() {
        setLayout(new GridBagLayout());
        dummy.setOpaque(false);
        add(dummy);
    }

    public void add(String title, JComponent managed, boolean startExpanded) {
        remove(dummy);

        CollapsiblePane newPane = new CollapsiblePane(title, managed, startExpanded);
        map.put(managed, newPane);

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(newPane, c);

        c.weighty = 1;
        add(dummy, c);
    }

    public void remove(JComponent component) {
        if (map.containsKey(component)) {
            super.remove(map.get(component));
            map.remove(component);
        } else {
            super.remove(component);
        }
    }

}
