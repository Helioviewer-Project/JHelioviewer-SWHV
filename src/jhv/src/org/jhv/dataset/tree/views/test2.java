package org.jhv.dataset.tree.views;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersModel;
import org.jhv.dataset.tree.models.DatasetIntervals;
import org.jhv.dataset.tree.models.DatasetNodeRenderer;
import org.jhv.dataset.tree.models.DatasetTreeCellEditor;

public class test2 extends JFrame {
    private static final long serialVersionUID = 1L;
    DatasetIntervals modeltest;
    LayersModel layersModel;
    ArrayList<LayerDescriptor> descriptors;
    ArrayList<String> intervals;
    IntervalsPanel intervalspanel;

    public test2() {
        modeltest = new DatasetIntervals();
        descriptors = new ArrayList<LayerDescriptor>();
        intervals = new ArrayList<String>();
        intervals.add(0, "first");
        intervals.add(1, "second");
        intervals.add(2, "third");

        descriptors.add(0, new LayerDescriptor(intervals.get(0), "imageInfoView"));
        descriptors.get(0).title = "AIA0";

        descriptors.add(1, new LayerDescriptor(intervals.get(1), "imageInfoView"));
        descriptors.get(1).title = "AIA1";

        descriptors.add(2, new LayerDescriptor(intervals.get(2), "imageInfoView"));
        descriptors.get(2).title = "AIA2";

        descriptors.add(3, new LayerDescriptor(intervals.get(0), "imageInfoView"));
        descriptors.get(3).title = "AIA3";

        descriptors.add(4, new LayerDescriptor(intervals.get(1), "imageInfoView"));
        descriptors.get(4).title = "AIA4";

        descriptors.add(5, new LayerDescriptor(intervals.get(2), "imageInfoView"));
        descriptors.get(5).title = "AIA5";

        descriptors.add(6, new LayerDescriptor(intervals.get(2), "imageInfoView"));
        descriptors.get(6).title = "AIA6";

        descriptors.add(7, new LayerDescriptor(intervals.get(2), "image"));
        descriptors.get(7).title = "AIA7";

        modeltest.addLayerDescriptor(descriptors.get(0), 0);
        modeltest.addLayerDescriptor(descriptors.get(1), 0);
        modeltest.addLayerDescriptor(descriptors.get(2), 0);
        modeltest.addLayerDescriptor(descriptors.get(3), 0);
        modeltest.addLayerDescriptor(descriptors.get(4), 0);
        modeltest.addLayerDescriptor(descriptors.get(5), 0);
        modeltest.addLayerDescriptor(descriptors.get(6), 0);
        JTree tree = new JTree(modeltest);
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        tree.setCellRenderer(new DatasetNodeRenderer());
        tree.setCellEditor(new DatasetTreeCellEditor(tree, (DefaultTreeCellRenderer) tree.getCellRenderer()));
        tree.setRowHeight(30);
        tree.setEditable(true);
        tree.updateUI();
        this.getContentPane().add(tree);

    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                test2 ex = new test2();
                ex.pack();
                ex.setVisible(true);
            }
        });
    }
}
