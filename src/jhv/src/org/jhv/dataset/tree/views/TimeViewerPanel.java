package org.jhv.dataset.tree.views;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTree;

public class TimeViewerPanel extends JPanel {
    private static final long serialVersionUID = -2808981935328885943L;

    /**
     * Create the panel.
     */
    public TimeViewerPanel() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        add(panel, BorderLayout.CENTER);

        JTree tree = new JTree();
        panel.add(tree);

    }

}
