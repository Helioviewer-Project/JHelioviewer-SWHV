package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTree;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;

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
        /**
         * DefaultMutableTreeNode eventNode = new
         * DefaultMutableTreeNode(this.eventType.getEventName()); for
         * (SWEKSupplier supplier : this.eventType.getSuppliers()) {
         * DefaultMutableTreeNode supplierNode = new
         * DefaultMutableTreeNode(supplier.getSupplierName());
         * eventNode.add(supplierNode); }
         */
        this.eventTypeTree = new JTree(new SWEKTreeModel(this.eventType));
        this.eventTypeTree.setCellRenderer(new SWEKEventTreeRenderer());
        add(this.eventTypeTree, BorderLayout.CENTER);
    }
}
