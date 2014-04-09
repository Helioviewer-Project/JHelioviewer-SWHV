package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.helioviewer.base.logging.Log;

public class PlotConfig {
	private BufferedImage image;
	private DrawableAreaMap map;
	private boolean visible;
	private Long downloadID;
	private Long imageId;
	
	public PlotConfig(BufferedImage image, DrawableAreaMap map, boolean visible, Long downloadID, Long imageID) {
		super();
		this.image = image;
		this.map = map;
		this.visible = visible;
		this.downloadID = downloadID;
		this.imageId = imageID;
	}
	
	public void draw(Graphics g){
		if(visible){
			//Log.debug("Will be drawn at " + map.toString());
			g.drawImage(image, map.getDestinationX0(), map.getDestinationY0(), 
					map.getDestinationX1(), map.getDestinationY1(), 
					map.getSourceX0(), map.getSourceY0(), map.getSourceX1(), map.getSourceY1(), null);
		}
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public int getDrawWidth(){
		return map.getDestinationX1() - map.getDestinationX0();
	}

	public Long getDownloadID() {
		return downloadID;
	}

	public void setDownloadID(Long downloadID) {
		this.downloadID = downloadID;
	}

	public Long getImageId() {
		return imageId;
	}

	public void setImageId(Long imageId) {
		this.imageId = imageId;
	}
	
	
}
