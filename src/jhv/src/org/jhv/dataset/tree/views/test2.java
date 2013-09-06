package org.jhv.dataset.tree.views;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersModel;
import org.jhv.dataset.tree.models.DatasetLayerTreeModel;

public class test2 extends JFrame {
	
	DatasetLayerTreeModel modeltest;
	LayersModel layersModel;
	ArrayList<LayerDescriptor> descriptors;
    ArrayList<String> intervals;
    IntervalsPanel intervalspanel;
    
    public test2() {
        
		modeltest = new DatasetLayerTreeModel();
		descriptors = new ArrayList<LayerDescriptor>();
		intervals = new ArrayList<String>();
		intervals.add(0, "first");
		intervals.add(1, "second");
		intervals.add(2, "third");
		
		descriptors.add( 0, new LayerDescriptor( "imageInfoView", intervals.get(0) ) );
		descriptors.add( 1, new LayerDescriptor( "imageInfoView", intervals.get(1) ) );
		descriptors.add( 2, new LayerDescriptor( "imageInfoView", intervals.get(2) ) );
		
		descriptors.add( 3, new LayerDescriptor( "imageInfoView", intervals.get(0) ) );
		descriptors.add( 4, new LayerDescriptor( "imageInfoView", intervals.get(1) ) );
		descriptors.add( 5, new LayerDescriptor( "imageInfoView", intervals.get(2) ) );
		
		descriptors.add( 6, new LayerDescriptor( "imageInfoView", intervals.get(2) ) );
		
		descriptors.add( 7, new LayerDescriptor( "image", intervals.get(2) ) );
		modeltest.addLayerDescriptor( descriptors.get(0), 0);
		modeltest.addLayerDescriptor( descriptors.get(1), 0);
		modeltest.addLayerDescriptor( descriptors.get(2), 0);
		modeltest.addLayerDescriptor( descriptors.get(3), 0);
		modeltest.addLayerDescriptor( descriptors.get(4), 0);
		modeltest.addLayerDescriptor( descriptors.get(5), 0);
		modeltest.addLayerDescriptor( descriptors.get(6), 0);
	    intervalspanel = new IntervalsPanel(modeltest);
		this.add(intervalspanel);
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
