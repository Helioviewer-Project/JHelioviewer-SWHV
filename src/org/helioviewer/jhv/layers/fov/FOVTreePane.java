package org.helioviewer.jhv.layers.fov;

import java.awt.Dimension;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.components.base.JHVTreeCell;

@SuppressWarnings("serial")
public final class FOVTreePane extends JScrollPane {

    private final DefaultMutableTreeNode root;
    private final JTree tree;

    public FOVTreePane(FOVCatalog catalog) {
        root = catalog.root();
        tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setEditable(true);
        tree.setShowsRootHandles(true);
        tree.setSelectionModel(null);
        tree.setCellRenderer(new JHVTreeCell.Renderer());
        tree.setCellEditor(new JHVTreeCell.Editor());
        tree.setRowHeight(0); // force calculation of nodes heights

        setViewportView(tree);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().brighter()));
        setPreferredSize(new Dimension(-1, 120));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        tree.setEnabled(enabled);

        Enumeration<?> nodes = root.breadthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            Object node = nodes.nextElement();
            if (node instanceof Interfaces.JHVCell cell)
                ComponentUtils.setEnabled(cell.getComponent(), enabled);
        }
    }

}
