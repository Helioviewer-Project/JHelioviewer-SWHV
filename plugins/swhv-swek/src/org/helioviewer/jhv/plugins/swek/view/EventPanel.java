package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTree;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelEventType;

/**
 * Panel display one event type
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class EventPanel extends JPanel {
    /** seriolVersionUID */
    private static final long serialVersionUID = 1057300852220893978L;

    /** The event type for which the event panel is created */
    private final SWEKEventType eventType;

    /** Tree containing the event type and it's sources. */
    private JTree eventTypeTree;

    /**
     * Creates a event panel for a certain
     */
    public EventPanel(SWEKEventType eventType) {
        this.eventType = eventType;
        initVisisualComponents();
    }

    /**
     * Initializes the visual components
     */
    private void initVisisualComponents() {
        setLayout(new BorderLayout());
        this.eventTypeTree = new JTree(new SWEKTreeModel(new SWEKTreeModelEventType(this.eventType)));
        this.eventTypeTree.setCellRenderer(new SWEKEventTreeRenderer());
        add(this.eventTypeTree, BorderLayout.CENTER);
    }
}
