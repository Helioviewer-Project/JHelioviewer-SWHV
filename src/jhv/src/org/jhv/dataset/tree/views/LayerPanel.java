package org.jhv.dataset.tree.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayerDescriptor;
import org.jhv.dataset.tree.models.DatasetLayer;
import org.jhv.dataset.tree.views.FixedHeightButton;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LayerPanel extends DatasetPanel{

	private static final long serialVersionUID = 7214631588320087038L;
	
	private DatasetLayer model;
		
	public LayerPanel( DatasetLayer model ) {
		super();
		this.model = model;
		this.update();
	}
	
	public void update(){
		this.removeAll();
		this.setLayout(new BorderLayout());
		JLabel label = new JLabel( model.getDescriptor().title);
		label.setMinimumSize(new Dimension(0, 10));
		this.add(label, BorderLayout.CENTER);
		JLabel leftButton = new JLabel( IconBank.getIcon( this.getIcon( this.model.getDescriptor()) ) );
		leftButton.setToolTipText(this.getTooltipText( this.model.getDescriptor() ));
		this.add(leftButton, BorderLayout.WEST);
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
