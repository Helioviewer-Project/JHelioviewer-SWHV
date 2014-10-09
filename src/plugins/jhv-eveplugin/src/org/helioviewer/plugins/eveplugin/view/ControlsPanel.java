package org.helioviewer.plugins.eveplugin.view;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class ControlsPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 3639870635351984819L;

    private static ControlsPanel singletongInstance;

    private ControlsPanel() {
        initVisualComponents();
    }

    private void initVisualComponents() {
        this.setPreferredSize(new Dimension(100, 300));
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public static ControlsPanel getSingletonInstance() {
        if (singletongInstance == null) {
            singletongInstance = new ControlsPanel();
        }

        return singletongInstance;
    }
}
