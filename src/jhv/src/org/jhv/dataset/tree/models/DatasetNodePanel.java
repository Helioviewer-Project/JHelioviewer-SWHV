package org.jhv.dataset.tree.models;

import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class DatasetNodePanel extends JPanel {

    private static final long serialVersionUID = 7341425049867146441L;
    JComponent[] components;

    DatasetNodePanel(JComponent[] components) {
        super();
        this.components = components;

        this.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        for (int i = 0; i < components.length; i++) {
            this.add(components[i]);
        }
    }

}
