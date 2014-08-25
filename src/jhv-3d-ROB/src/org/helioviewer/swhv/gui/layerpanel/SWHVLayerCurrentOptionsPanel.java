package org.helioviewer.swhv.gui.layerpanel;

import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerModel;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerModelListener;

public class SWHVLayerCurrentOptionsPanel extends JPanel  implements SWHVLayerContainerModelListener{
	private static final long serialVersionUID = 1L;
	SWHVAbstractOptionPanel currentPanel;

	public SWHVLayerCurrentOptionsPanel(){
		setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
	}
	
	public void updateActive(SWHVLayerModel model) {
		
	}

	@Override
	public void layerAdded(SWHVLayerContainerModel model, SWHVLayerModel layer, int i) {		
	}

	@Override
	public void layerAdded(SWHVLayerContainerModel model, SWHVLayerModel layer) {	
	}

	@Override
	public void layerRemoved(SWHVLayerContainerModel model, int position) {
	}

	@Override
	public void layerFolded(SWHVLayerContainerModel model) {
	}

	@Override
	public void layerActivated(final int position) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				currentPanel = (SWHVAbstractOptionPanel)GlobalStateContainer.getSingletonInstance().getLayerContainerModel().getLayer(position).getOptionPanel();
				removeAll();
				add(currentPanel);
				validate();
				repaint();
			 }
		});		
	}
	
}
