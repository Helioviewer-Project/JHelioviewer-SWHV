package org.jhv.dataset.tree.views;
/**
 * @author Freek Verstringe
 */

import org.helioviewer.jhv.layers.LayersModel;

import org.helioviewer.viewmodel.view.View;
import org.jhv.dataset.tree.models.DatasetLayer;
import org.jhv.dataset.tree.models.DatasetType;
import org.jhv.dataset.tree.models.LayersToDatasetLayers;
import org.jhv.dataset.tree.views.FixedHeightButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.TreePath;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class TypePanel extends DatasetPanel{
	private static final long serialVersionUID = 8669761869598533103L;
	
	private final DatasetType model;
	
	public TypePanel(final DatasetType model) {
		super();
		this.model = model;
		setLayout(new BorderLayout());
		
		JLabel label = new JLabel(model.getTitle());
		label.addMouseListener(new MouseAdapter()  
		{  
		    public void mouseReleased(MouseEvent e)  
		    {  
		    	TreePath path = new TreePath(model.getModel().getPathToRoot(model));
		    	if(! model.getModel().getTree().isCollapsed(path)){
					for( int i=0 ; i< model.datasetLayers.size(); i++){
						DatasetLayer layer = model.datasetLayers.get(i);
						
						LayersToDatasetLayers layersModel = LayersToDatasetLayers.getSingletonInstance();
						View view = layersModel.getView(layer.getDescriptor());
						int index = layersModel.getIndex(layer.getDescriptor());
						LayersModel.getSingletonInstance().setVisible(view,false);
					}
					model.getModel().getTree().collapsePath(path);
		    	}
		    	else{
					for( int i=0 ; i< model.datasetLayers.size(); i++){
						DatasetLayer layer = model.datasetLayers.get(i);
						
						LayersToDatasetLayers layersModel = LayersToDatasetLayers.getSingletonInstance();
						View view = layersModel.getView(layer.getDescriptor());
						int index = layersModel.getIndex(layer.getDescriptor());
						LayersModel.getSingletonInstance().setVisible(view,true);
					}
					model.getModel().getTree().expandPath(path);
		    	}
		    }  
		});
		add( label, BorderLayout.CENTER );		
	}
	
}
