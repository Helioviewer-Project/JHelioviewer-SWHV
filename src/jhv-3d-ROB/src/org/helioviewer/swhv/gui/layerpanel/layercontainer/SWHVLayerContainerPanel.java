package org.helioviewer.swhv.gui.layerpanel.layercontainer;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.swhv.gui.GUISettings;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerController;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerPanel;
import org.helioviewer.swhv.mvc.SWHVPanel;

public class SWHVLayerContainerPanel extends JScrollPane implements SWHVPanel, SWHVLayerContainerModelListener{
	
	private SWHVLayerContainerController controller;
	private JPanel gridPanel;
	private JPanel containerPanel;
	private JPanel bottomPanel;

	
	public SWHVLayerContainerPanel(){
		 SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gridPanel = new JPanel();
				containerPanel = new JPanel();
				containerPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
				bottomPanel = new JPanel();
				gridPanel.setLayout(new GridLayout(0, 1, 0, 0));
				gridPanel.setOpaque(true);
				containerPanel.add(gridPanel);
				containerPanel.add(bottomPanel);
				setViewportView(containerPanel);
				containerPanel.setPreferredSize(new Dimension(GUISettings.LEFTPANELWIDTH, GUISettings.LEFTPANELHEIGHT));	
				//containerPanel.setMaximumSize(new Dimension(GUISettings.LEFTPANELWIDTH, GUISettings.LEFTPANELHEIGHT));	
				//gridPanel.setPreferredSize(new Dimension(GUISettings.LEFTPANELWIDTH, GUISettings.LEFTPANELHEIGHT));	
				gridPanel.getMaximumSize().width = GUISettings.LEFTPANELWIDTH;				
			 }
		 });
	}
	
	private void updateLayers(final SWHVLayerContainerModel layerContainerModel) {
		 SwingUtilities.invokeLater(new Runnable() {
			 public void run() {		
				synchronized(GlobalStateContainer.getSingletonInstance().getLayerContainerModel()){
					gridPanel.removeAll();
					SWHVLayerModel[] layers = layerContainerModel.getLayers();
					int len = layers.length;
					for( int i=0; i<len; i++){
						if(layers[i].isVisible()){
							SWHVLayerModel layer = layers[i];
							SWHVLayerController ctr = layer.getController();
							SWHVLayerPanel panel = ctr.getPanel();
							gridPanel.add(panel);
						}
					}
					gridPanel.revalidate();
					gridPanel.repaint();
				}
			 }
		 });
	}

	@Override
	public void layerAdded(SWHVLayerContainerModel model, SWHVLayerModel child, int i) {
		updateLayers(model);
	}

	@Override
	public void layerAdded(SWHVLayerContainerModel model, SWHVLayerModel child) {
		updateLayers(model);

	}

	@Override
	public void layerRemoved(SWHVLayerContainerModel model, int position) {
		updateLayers(model);
	}
	
	@Override
	public void layerFolded(SWHVLayerContainerModel model) {
		updateLayers(model);		
	}
	
	public void setController( SWHVLayerContainerController controller) {
		this.controller = controller;		
	}

	@Override
	public void layerActivated(int position) {
	}

	@Override
	public SWHVLayerContainerController getController() {
		// TODO Auto-generated method stub
		return this.controller;
	}
	
}
