package org.jhv.dataset.tree.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;


import org.helioviewer.jhv.gui.actions.RemoveLayerAction;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.AbstractLayeredView.Layer;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;
import org.jhv.dataset.tree.models.DatasetNode;
import org.jhv.dataset.tree.models.DatasetTreeBuilder;
import org.jhv.dataset.tree.models.DatasetTreeCellEditor;
import org.jhv.dataset.tree.models.DatasetTreeModel;
import org.jhv.dataset.tree.models.DatasetNodeRenderer;
import org.helioviewer.viewmodel.view.ImageInfoView;

import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.DefaultMutableTreeNode;


/**
* @author Freek Verstringe
*
*/
public class DatasetTreePanel extends JPanel implements TreeSelectionListener, LayersListener {
    
	private static final long serialVersionUID = -8780381858126770318L;
	private JTree tree;
    private DatasetNode rootNode;
    private DatasetTreeModel model;
    
    //Nodes that represent the layers
    private ArrayList<DatasetNode> nodes;
    
    //Nodes that represent the dateRangesNodes, a hashmap between the interval and the nodes
    private HashMap<ImageInfoView, DatasetNode> dateRangeNodes;

    public JTree getTree() {
		return tree;
	}

	public DatasetTreePanel() {
        super();
        setLayout(new BorderLayout());
        nodes = new ArrayList<DatasetNode>();

        rootNode = new DatasetNode(new JButton("Layers"));
        model = new DatasetTreeModel(rootNode);   
        dateRangeNodes = new HashMap<ImageInfoView, DatasetNode>();
        tree = new JTree(model);
        tree.setCellRenderer(new DatasetNodeRenderer());
        //Make the tree editable by adding a custom Cell editor. This ensures the events are propagated to the underlying buttons.
        tree.addTreeSelectionListener(this);
        tree.setCellEditor(new DatasetTreeCellEditor(tree, (DefaultTreeCellRenderer) tree.getCellRenderer()));
        tree.setEditable(true);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        
        // expand all nodes in the tree to be visible
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        LayersModel.getSingletonInstance().addLayersListener(this);
        setSize(280, 300);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {

    }
	/*
	 * The new layers are inserted at position idx. This means all layers with higher or equal idx are shifted.
	 */
	@Override
	public void layerAdded(int idx) {
		final DatasetNode node;
		View view = LayersModel.getSingletonInstance().getLayer(idx);
		ImageInfoView imageInfoView = view.getAdapter(ImageInfoView.class);

		JButton[] buttons = new JButton[3];
		buttons[0] = new JButton(new RemoveLayerAction(imageInfoView));

		Interval<Date> range =  imageInfoView.getDateRange();
		Calendar cal = Calendar.getInstance();
		cal.setTime(range.getStart());
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		String beginDateString = format.format(range.getStart());
		String endDateString = format.format(range.getEnd());
		
		buttons[1] = new JButton(beginDateString + "-" + endDateString);
		
		//String title = LayersModel.getSingletonInstance().getDescriptor(imageInfoView).title;
		buttons[2] = new JButton(" zer");
		node = new DatasetNode(buttons);
		rootNode.insert(node, idx);
		if(node !=null){
			dateRangeNodes.put(imageInfoView, node);
		}
		tree.updateUI();
	}
	
	/*
	 * This puts the layer with index oldIdx to null.
	 */
	@Override
	public void layerRemoved(View oldView, int oldIdx) {				
		ImageInfoView imageInfoView = oldView.getAdapter(ImageInfoView.class);
		if(dateRangeNodes.containsKey(imageInfoView)){
			dateRangeNodes.get(imageInfoView).removeFromParent();
			dateRangeNodes.remove(imageInfoView);
		}
		tree.updateUI();
			
	}

	@Override
	public void layerChanged(int idx) {

	}

	@Override
	public void activeLayerChanged(int idx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void viewportGeometryChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void timestampChanged(int idx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void subImageDataChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void layerDownloaded(int idx) {
		// TODO Auto-generated method stub
		
	}
}