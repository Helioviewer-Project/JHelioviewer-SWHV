package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.plugins.eveplugin.download.DataDownloader;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class DownloadRequestData implements LineDataSelectorElement{

	private List<RadioImage> radioImages;
	private Long ID;
	private boolean isDownloading;
	private String plotIdentifier;
	private boolean isVisible;
	
	private RadioDataManager radioDataManager;
	
	public DownloadRequestData(long ID, String plotIdentifier) {
		this.radioDataManager = RadioDataManager.getSingletonInstance();
		this.ID = ID;
		this.radioImages = new ArrayList<RadioImage>();
		this.plotIdentifier = plotIdentifier;
		this.isVisible = true;
	}

	public DownloadRequestData(long ID, List<RadioImage> radioImages, String plotIdentifier) {
		this.ID = ID;
		this.radioImages = radioImages;
		this.plotIdentifier = plotIdentifier;
		this.isVisible = true;
	}
	
	public void addRadioImage(RadioImage radioImage){
		this.radioImages.add(radioImage);
	}

	public List<RadioImage> getRadioImages() {
		return radioImages;
	}

	public void setRadioImages(List<RadioImage> radioImages) {
		this.radioImages = radioImages;
	}

	public Long getID() {
		return ID;
	}

	public void setID(Long iD) {
		ID = iD;
	}

	@Override
	public void removeLineData() {
		radioDataManager.removeDownloadRequestData(this);
		
	}

	@Override
	public void setVisibility(boolean visible) {
		this.isVisible = visible;
		radioDataManager.downloadRequestDataVisibilityChanged(this);
		
	}

	@Override
	public boolean isVisible() {
		return this.isVisible;
	}

	@Override
	public String getName() {
		return "Callisto radiogram";
	}

	@Override
	public Color getDataColor() {
		return Color.black;
	}

	@Override
	public void setDataColor(Color c) {}

	@Override
	public boolean isDownloading() {
		return isDownloading;
	}

	@Override
	public String getPlotIdentifier() {
		return plotIdentifier;
	}

	@Override
	public void setPlotIndentifier(String plotIdentifier) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isAvailable() {
		// TODO Auto-generated method stub
		return true;
	}
	
	public void setDownloading(boolean isDownloading){
		this.isDownloading = isDownloading;
	}
}
