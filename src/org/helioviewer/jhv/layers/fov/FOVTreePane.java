package org.helioviewer.jhv.layers.fov;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.layers.fov.FOVTreeElement.FOVType;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

@SuppressWarnings("serial")
public class FOVTreePane extends JScrollPane {

    private final FOVTreeRoot root;

    public FOVTreePane() {
        root = new FOVTreeRoot("SOLO");
        root.add(new FOVTreeElement("EUI/HRI", FOVType.RECTANGULAR, 0, 16.6 / 60., 16.6 / 60., Colors.Blue));
        root.add(new FOVTreeElement("EUI/FSI", FOVType.RECTANGULAR, 0, 228 / 60., 228 / 60., Colors.Blue));
        root.add(new FOVTreeElement("METIS", FOVType.CIRCULAR, 3, 5.8, 5.8, Colors.Blue));
        root.add(new FOVTreeElement("PHI/HRT", FOVType.RECTANGULAR, 0, 0.28, 0.28, Colors.Blue));
        root.add(new FOVTreeElement("PHI/FDT", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue));
        root.add(new FOVTreeElement("SPICE", FOVType.RECTANGULAR, 0, 16 / 60., 11 / 60., Colors.Blue));
        root.add(new FOVTreeElement("STIX", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue));

        DefaultTreeModel dtm = new DefaultTreeModel(root);
        FOVTreeRenderer ftr = new FOVTreeRenderer();

        JTree tree = new JTree(dtm);
        tree.setEditable(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(ftr);
        tree.setCellEditor(new MyTreeCellEditor(tree, ftr));
        tree.setRowHeight(0); // force calculation of nodes heights

        setViewportView(tree);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(-1, 90));
    }

    public void putFOV(FOVShape f, double distance, BufVertex lineBuf, BufVertex centerBuf, JhvTextRenderer renderer) {
        root.children().asIterator().forEachRemaining(c -> ((FOVTreeElement) c).putFOV(f, distance, lineBuf, centerBuf, renderer));
    }

    public int countEnabled() {
        int count = 0;
        for (Enumeration<TreeNode> e = root.children(); e.hasMoreElements();) {
            if (((FOVTreeElement) e.nextElement()).isEnabled())
                count++;
        }
        return count;
    }

    private static class MyTreeCellEditor extends DefaultTreeCellEditor {

        MyTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
            super(tree, renderer);
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
            return renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }

    }

}
