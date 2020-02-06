package org.helioviewer.jhv.layers.fov;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.layers.fov.FOVTreeElement.FOVType;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

@SuppressWarnings("serial")
public class FOVTreePane extends JScrollPane {

    private final FOVTreeRoot[] roots = new FOVTreeRoot[]{
            new FOVTreeRoot("SOLO"),
            new FOVTreeRoot("SDO"),
            new FOVTreeRoot("PROBA-2")
    };

    public FOVTreePane() {
        roots[0].add(new FOVTreeElement("EUI/HRI", FOVType.RECTANGULAR, 0, 16.6 / 60., 16.6 / 60., Colors.Blue));
        roots[0].add(new FOVTreeElement("EUI/FSI", FOVType.RECTANGULAR, 0, 228 / 60., 228 / 60., Colors.Blue));
        roots[0].add(new FOVTreeElement("METIS", FOVType.CIRCULAR, 3, 5.8, 5.8, Colors.Blue));
        roots[0].add(new FOVTreeElement("PHI/HRT", FOVType.RECTANGULAR, 0, 0.28, 0.28, Colors.Blue));
        roots[0].add(new FOVTreeElement("PHI/FDT", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue));
        roots[0].add(new FOVTreeElement("SPICE", FOVType.RECTANGULAR, 0, 16 / 60., 11 / 60., Colors.Blue));
        roots[0].add(new FOVTreeElement("STIX", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue));

        roots[1].add(new FOVTreeElement("AIA", FOVType.RECTANGULAR, 0, (0.6 * 4096) / 3600., (0.6 * 4096) / 3600., Colors.Blue));
        roots[1].add(new FOVTreeElement("HMI", FOVType.RECTANGULAR, 0, (0.6 * 4096) / 3600., (0.6 * 4096) / 3600., Colors.Blue));

        roots[2].add(new FOVTreeElement("SWAP", FOVType.RECTANGULAR, 0, (3.1646941 * 1024) / 3600., (3.1646941 * 1024) / 3600., Colors.Blue));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

        for (FOVTreeRoot root : roots) {
            panel.add(new FOVTree(new DefaultTreeModel(root)), gc);
            gc.gridy++;
        }

        setViewportView(panel);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        getVerticalScrollBar().setUnitIncrement(20); // ugh
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(-1, 90));
    }

    public void putFOV(FOVShape f, double distance, BufVertex lineBuf, BufVertex centerBuf, JhvTextRenderer renderer) {
        for (FOVTreeRoot root : roots) {
            root.children().asIterator().forEachRemaining(c -> ((FOVTreeElement) c).putFOV(f, distance, lineBuf, centerBuf, renderer));
        }
    }

    public boolean hasEnabled() {
        for (FOVTreeRoot root : roots) {
            for (Enumeration<TreeNode> e = root.children(); e.hasMoreElements(); ) {
                if (((FOVTreeElement) e.nextElement()).isEnabled())
                    return true;
            }
        }
        return false;
    }

}
