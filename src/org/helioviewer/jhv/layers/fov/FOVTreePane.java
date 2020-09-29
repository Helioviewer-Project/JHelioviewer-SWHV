package org.helioviewer.jhv.layers.fov;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.JHVTreeCell;
import org.helioviewer.jhv.layers.fov.FOVInstrument.FOVType;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

@SuppressWarnings("serial")
public class FOVTreePane extends JScrollPane {

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

    public FOVTreePane() {
        FOVPlatform plat = new FOVPlatform("SOLO");
        plat.add(new FOVInstrument("EUI/HRI", FOVType.RECTANGULAR, 0, 16.6 / 60., 16.6 / 60., Colors.Blue));
        plat.add(new FOVInstrument("EUI/FSI", FOVType.RECTANGULAR, 0, 228 / 60., 228 / 60., Colors.Blue));
        plat.add(new FOVInstrument("METIS", FOVType.CIRCULAR, 3, 5.8, 5.8, Colors.Blue));
        plat.add(new FOVInstrument("PHI/HRT", FOVType.RECTANGULAR, 0, 0.28, 0.28, Colors.Blue));
        plat.add(new FOVInstrument("PHI/FDT", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue));
        plat.add(new FOVInstrument("SPICE", FOVType.RECTANGULAR, 0, 16 / 60., 11 / 60., Colors.Blue));
        plat.add(new FOVInstrument("STIX", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue));
        root.add(plat);

        plat = new FOVPlatform("SDO");
        plat.add(new FOVInstrument("AIA", FOVType.RECTANGULAR, 0, (0.6 * 4096) / 3600., (0.6 * 4096) / 3600., Colors.Blue));
        plat.add(new FOVInstrument("HMI", FOVType.RECTANGULAR, 0, (0.6 * 4096) / 3600., (0.6 * 4096) / 3600., Colors.Blue));
        root.add(plat);

        plat = new FOVPlatform("PROBA-2");
        plat.add(new FOVInstrument("SWAP", FOVType.RECTANGULAR, 0, (3.1646941 * 1024) / 3600., (3.1646941 * 1024) / 3600., Colors.Blue));
        root.add(plat);

        JTree tree = new JTree(root);
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
        getVerticalScrollBar().setUnitIncrement(20); // ugh
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(-1, 94));

        ComponentUtils.smallVariant(this);
    }

    public void putFOV(FOVShape f, double distance, BufVertex lineBuf, BufVertex centerBuf, JhvTextRenderer renderer) {
        Enumeration<TreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            TreeNode node = e.nextElement();
            if (node.isLeaf()) {
                ((FOVInstrument) node).putFOV(f, distance, lineBuf, centerBuf, renderer);
            }
        }
    }

    public boolean hasEnabled() {
        Enumeration<TreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            TreeNode node = e.nextElement();
            if (node.isLeaf() && ((FOVInstrument) node).isEnabled()) {
                return true;
            }
        }
        return false;
    }

}
