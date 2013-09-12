package org.jhv.dataset.tree.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.jhv.dataset.tree.models.DatasetLayer;
import org.jhv.dataset.tree.models.LayersToDatasetLayers;
import org.jhv.dataset.tree.views.FixedHeightButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LayerPanel extends DatasetPanel{

	private static final long serialVersionUID = 7214631588320087038L;
	
	private DatasetLayer model;
	private JLabel iconLabel;
	private JLabel timestampLabel;
	private JLabel titleLabel;
	private JLabel deleteIconLabel;
	private MouseAdapter adapter;
		
	public LayerPanel( DatasetLayer model ) {
		super();
		this.model = model;
		this.updateChange();
	}
	public void updateChangeFast(){
		ImageIcon icon = IconBank.getIcon( this.getIcon( this.model.getDescriptor()) );
		iconLabel.setIcon( icon );
		titleLabel.setText( this.model.getDescriptor().title);
		timestampLabel.setText( this.model.getDescriptor().timestamp );
		ImageIcon deleteIcon = IconBank.getIcon(JHVIcon.REMOVE_LAYER );
		deleteIconLabel = new JLabel( deleteIcon );
		deleteIconLabel.setToolTipText("Remove this layer");
		adapter = new MouseAdapter()  
		{  
		    public void mouseReleased(MouseEvent e)  
		    {  
		        LayersModel.getSingletonInstance().removeLayer(LayersToDatasetLayers.getSingletonInstance().getView(model.getDescriptor()));
		    }  
		};
		deleteIconLabel.addMouseListener(adapter); 		
	}
	
	public void updateChange(){
		this.removeAll();
		this.setPreferredSize(new Dimension(250, 25));
		this.setBorder(BorderFactory.createEmptyBorder(2,5,2,5)) ;

		this.setLayout(new BoxLayout( this, BoxLayout.LINE_AXIS));
		
		//Icon
		ImageIcon icon = IconBank.getIcon( this.getIcon( this.model.getDescriptor()) );
		iconLabel = new JLabel( icon );
		iconLabel.setToolTipText(this.getTooltipText( this.model.getDescriptor() ));
		adapter = new MouseAdapter()  
		{  
		    public void mouseReleased(MouseEvent e)  
		    {  
		    	LayersToDatasetLayers layersModel = LayersToDatasetLayers.getSingletonInstance();
		        View view = layersModel.getView(model.getDescriptor());
		        int index = layersModel.getIndex(model.getDescriptor());

		        LayersModel.getSingletonInstance().setVisible(view,!LayersModel.getSingletonInstance().isVisible(index));
		    }  
		}; 
		iconLabel.addMouseListener(adapter);

		
		
		this.add(iconLabel);
		//whitespace
		this.add(Box.createRigidArea(new Dimension(5,5)));
		//Title
		titleLabel = new JLabel( model.getDescriptor().title);
		titleLabel.setToolTipText("Name of the Layer");
		this.add(titleLabel);
		//whitespace
		this.add(Box.createRigidArea(new Dimension(5,5)));
		//timestamp
		timestampLabel = new JLabel( this.model.getDescriptor().timestamp );
		timestampLabel.setToolTipText("Shown observation time (UTC) of this layer.");
		this.add(timestampLabel);

		//Icon
		ImageIcon deleteIcon = IconBank.getIcon(JHVIcon.REMOVE_LAYER );
		deleteIconLabel = new JLabel( deleteIcon );
		deleteIconLabel.setToolTipText("Remove this layer");
		deleteIconLabel.addMouseListener(new MouseAdapter()  
		{  
		    public void mouseReleased(MouseEvent e)  
		    {  
		        LayersModel.getSingletonInstance().removeLayer(LayersToDatasetLayers.getSingletonInstance().getView(model.getDescriptor()));
		    }  
		});  
		this.add(deleteIconLabel);
		//whitespace
		this.add(Box.createRigidArea(new Dimension(5,5)));		
		this.invalidate();
		this.repaint();
	}
	
    private String getTooltipText(LayerDescriptor descriptor) {
        boolean isMovie = descriptor.isMovie;
        boolean isMaster = descriptor.isMaster;
        boolean isTimed = descriptor.isTimed;
        boolean isVisible = descriptor.isVisible;

        String tooltip = "";

        if (isMovie && isTimed && isVisible && isMaster)
            tooltip = "Master Layer (Movie)";
        if (isMovie && isTimed && isVisible && !isMaster)
            tooltip = "Slave Layer (Movie)";
        if (isMovie && isTimed && !isVisible)
            tooltip = "Invisible Layer (Movie)";
        if (isMovie && !isTimed && isVisible)
            tooltip = "Layer (Movie, No timing Information)";
        if (isMovie && !isTimed && !isVisible)
            tooltip = "Invisible Layer (Movie, No timing Information)";

        if (!isMovie && isTimed && isVisible && isMaster)
            tooltip = "Master Layer (Image)";
        if (!isMovie && isTimed && isVisible && !isMaster)
            tooltip = "Slave Layer (Image)";
        if (!isMovie && isTimed && !isVisible)
            tooltip = "Invisible Layer (Image)";
        if (!isMovie && !isTimed && isVisible)
            tooltip = "Layer (Image, No timing Information)";
        if (!isMovie && !isTimed && !isVisible)
            tooltip = "Invisible Layer (Image, No timing Information)";

        if (isVisible) {
            tooltip = tooltip + " - Click to Hide";
        } else {
            tooltip = tooltip + " - Click to Unhide";
        }

        return tooltip;
    }

    /**
     * Choose the right icon for the given LayerDescriptor
     * 
     * @param descriptor
     *            - LayerDescriptor to base the selection on
     * @return the JHVIcon to draw
     */
    public JHVIcon getIcon(LayerDescriptor descriptor) {
        boolean isMovie = descriptor.isMovie;
        boolean isMaster = descriptor.isMaster;
        boolean isTimed = descriptor.isTimed;
        boolean isVisible = descriptor.isVisible;

        JHVIcon icon = null;

        if (isMovie && isTimed && isVisible && isMaster)
            icon = JHVIcon.LAYER_MOVIE_TIME_MASTER;
        if (isMovie && isTimed && isVisible && !isMaster)
            icon = JHVIcon.LAYER_MOVIE_TIME;
        if (isMovie && isTimed && !isVisible)
            icon = JHVIcon.LAYER_MOVIE_TIME_OFF;
        if (isMovie && !isTimed && isVisible)
            icon = JHVIcon.LAYER_MOVIE;
        if (isMovie && !isTimed && !isVisible)
            icon = JHVIcon.LAYER_MOVIE_OFF;

        if (!isMovie && isTimed && isVisible && isMaster)
            icon = JHVIcon.LAYER_IMAGE_TIME_MASTER;
        if (!isMovie && isTimed && isVisible && !isMaster)
            icon = JHVIcon.LAYER_IMAGE_TIME;
        if (!isMovie && isTimed && !isVisible)
            icon = JHVIcon.LAYER_IMAGE_TIME_OFF;
        if (!isMovie && !isTimed && isVisible)
            icon = JHVIcon.LAYER_IMAGE;
        if (!isMovie && !isTimed && !isVisible)
            icon = JHVIcon.LAYER_IMAGE_OFF;

        return icon;
    }	
	
}
