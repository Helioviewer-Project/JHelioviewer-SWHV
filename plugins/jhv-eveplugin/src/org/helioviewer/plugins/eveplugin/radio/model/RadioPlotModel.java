package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.plugins.eveplugin.lines.data.EVEValues;
import org.helioviewer.plugins.eveplugin.lines.model.EVEDrawController;
import org.helioviewer.plugins.eveplugin.lines.model.EVEDrawControllerListener;
import org.helioviewer.plugins.eveplugin.model.ChartModel;
import org.helioviewer.plugins.eveplugin.radio.data.DownloadRequestData;
import org.helioviewer.plugins.eveplugin.radio.data.FrequencyInterval;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDataManager;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDataManagerListener;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDownloaderListener;
import org.helioviewer.plugins.eveplugin.radio.data.RadioImage;
import org.helioviewer.plugins.eveplugin.radio.gui.RadioImagePane;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.viewmodel.view.ImageInfoView;

public class RadioPlotModel implements RadioDataManagerListener,ZoomControllerListener,//EVEDrawControllerListener, 
			ZoomDataConfigListener{
	private static RadioPlotModel instance;
	private Map<Long,DownloadRequestData> downloadRequestData;
	private RadioDataManager radioDataManager;
	//private Rectangle areaAvailable;
	private FrequencyInterval freqInterval;
	private Interval<Date> dateInterval;
	private ZoomManager zoomManager;
	private List<RadioPlotModelListener> listeners;
	private Map<Long,List<PlotConfig>> plotConfigList;
	private YAxisElement yAxisElement;
	private RadioImagePane radioImagePane;
	private DrawController drawController;
	private Map<Long, BufferedImage> bufferedImages;
	private ChartModel chartModel;
	
	
	private RadioPlotModel(){
		ZoomController.getSingletonInstance().addZoomControllerListener(this);
		radioDataManager = RadioDataManager.getSingletonInstance();
		this.radioDataManager.addRadioManagerListener(this);
		this.zoomManager = ZoomManager.getSingletonInstance();
		//zoomManager.addZoomManagerListener(this);
		//areaAvailable = new Rectangle();
		listeners = new ArrayList<RadioPlotModelListener>();
		plotConfigList = new HashMap<Long,List<PlotConfig>>();
		this.downloadRequestData = new HashMap<Long, DownloadRequestData>();
		yAxisElement = new YAxisElement();
		yAxisElement.setColor(Color.BLACK);
		yAxisElement.setLabel("radio");
		radioImagePane = new RadioImagePane();
		drawController = DrawController.getSingletonInstance();
		bufferedImages = new HashMap<Long, BufferedImage>();
		chartModel = ChartModel.getSingletonInstance();
	}
	
	public static RadioPlotModel getSingletonInstance(){
		if (instance == null){
			instance  = new RadioPlotModel();			
		}
		return instance;
	}

	public void addRadioPlotModelListener(RadioPlotModelListener listener){
		this.listeners.add(listener);
	}
	
	public void removeRadioPlotModelListener(RadioPlotModelListener listener){
		this.listeners.remove(listener);
	}
	
	public void newRequestForData(Date startTime, Date endTime){
		
	}
	
	@Override
	public void newDataAvailable(DownloadRequestData data, long ID) {
			this.downloadRequestData.put(ID,data);
	}

	@Override
	public void downloadFinished(long ID) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void availableIntervalChanged(Interval<Date> newInterval) {
		Log.debug("Available interval changed : " + newInterval.toString());
		//radioDataManager.setVisibleParameters(newInterval, freqInterval, areaAvailable);
	}

	@Override
	public void selectedIntervalChanged(Interval<Date> newInterval) {
		Log.debug("Selected interval changed : "+ newInterval.toString());
	}

	@Override
	public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
		Log.debug("Selected resolution changed : "+ newResolution);		
	}

	@Override
	public void dataNotChanged(Interval<Date> timeInterval,
			FrequencyInterval freqInterval, Rectangle area, List<Long> IDList, String identifier, long radioImageID) {
		Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		Log.debug("new data not changed received");
		Log.debug("IDs");
		for(long ID : IDList)
			Log.debug(ID);
		Log.debug("RadioImageID: "+ radioImageID);
		Log.debug("time interval : " + timeInterval.toString());
		Log.debug("Frequency interval" + freqInterval.toString());
		Log.debug("area : " + area.toString());
		for(long ID : IDList){
			BufferedImage newImage = bufferedImages.get(radioImageID);
			bufferedImages.put(ID, newImage);
			DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(),
					freqInterval.getStart(), freqInterval.getEnd(), area, ID, identifier);
			Log.debug("*****************************************************************");
			Log.debug("New image should be drawn with the following characteristics: ");
			Log.debug("source x0 : " + dam.getSourceX0());
			Log.debug("source y0 : " + dam.getSourceY0());
			Log.debug("source x1 : " + dam.getSourceX1());
			Log.debug("source y1 : " + dam.getSourceY1());
			Log.debug("destin x0 : " + dam.getDestinationX0());
			Log.debug("destin y0 : " + dam.getDestinationY0());
			Log.debug("destin x1 : " + dam.getDestinationX1());
			Log.debug("destin y1 : " + dam.getDestinationY1());
			Log.debug("*****************************************************************");
			//warn listeners a new image should be drawn.
			yAxisElement.setMinValue(freqInterval.getStart());
			yAxisElement.setMaxValue(freqInterval.getEnd());
			yAxisElement.setAvailableRange(new Range(freqInterval.getStart(),freqInterval.getEnd()));
			yAxisElement.setSelectedRange(new Range(freqInterval.getStart(),freqInterval.getEnd()));
			PlotConfig pc = new PlotConfig(newImage, dam, downloadRequestData.get(ID).isVisible(),ID );
			if(plotConfigList.containsKey(ID)){
				plotConfigList.get(ID).add(pc);
			}else{
				List<PlotConfig> tempList = new ArrayList<PlotConfig>();
				tempList.add(pc);
				plotConfigList.put(ID,tempList);
			}
			fireDrawNewBufferedImage(newImage, dam, ID, identifier);
		}
		Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}
	
	/*public void setAreaAvailable(Rectangle area){
		this.areaAvailable = area;
	}*/

	/*@Override
	public void drawRequest(Interval<Date> interval, Band[] bands,
			EVEValues[] values, Range availableRange, Range selectedRange) {
		Log.debug("Draw Request: ");
		Log.debug("interval: "+ interval.toString());
		Log.debug("available range: "+availableRange.toString());
		Log.debug("selected range: "+ selectedRange.toString());
		
	}

	@Override
	public void drawRequest(Date movieTimestamp) {
		// TODO Auto-generated method stub
		
	}*/

	
	@Override
	public void newGlobalFrequencyInterval(FrequencyInterval interval) {
		this.freqInterval = interval;
	}	

	@Override
	public void requestData(Date xStart, Date xEnd, double yStart, double yEnd,
			double xRatio, double yRatio, long ID) {
		List<Long> idList = new ArrayList<Long>();
		idList.add(ID);
		radioDataManager.requestForData(xStart,xEnd, yStart, yEnd, xRatio, yRatio, idList); 
		//plotConfigList.clear();
		Log.debug("Size of config list : " + plotConfigList.size());
	}

	@Override
	public void downloadRequestAnswered(FrequencyInterval freqInterval,
			Interval<Date> timeInterval, long ID, String identifier) {
		Log.debug("downloadRequestAnswered called");
		zoomManager.addZoomDataConfig(freqInterval, timeInterval, this, ID, identifier);		
	}

	@Override
	public void newDataReceived(byte[] data, Interval<Date> timeInterval,
			FrequencyInterval freqInterval, Rectangle area, List<Long> IDList, String identifier,Long radioImageID) {
		Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		Log.debug("new Data received");
		for(long ID : IDList)
			Log.debug(ID);
		Log.debug("RadioImageId : " + radioImageID);
		Log.debug("data length : " + data.length);
		Log.debug("time interval : " + timeInterval.toString());
		Log.debug("Frequency interval" + freqInterval.toString());
		Log.debug("area : " + area.toString());
		for(long ID : IDList){
			BufferedImage newImage = createBufferedImage(area.width, area.height, data, ID);
			bufferedImages.put(radioImageID, newImage);
			DrawableAreaMap dam = zoomManager.getDrawableAreaMap(timeInterval.getStart(), timeInterval.getEnd(),
					freqInterval.getStart(), freqInterval.getEnd(), area, ID, identifier);
			Log.debug("*****************************************************************");
			Log.debug("New image should be drawn with the following characteristics: ");
			Log.debug("source x0 : " + dam.getSourceX0());
			Log.debug("source y0 : " + dam.getSourceY0());
			Log.debug("source x1 : " + dam.getSourceX1());
			Log.debug("source y1 : " + dam.getSourceY1());
			Log.debug("destin x0 : " + dam.getDestinationX0());
			Log.debug("destin y0 : " + dam.getDestinationY0());
			Log.debug("destin x1 : " + dam.getDestinationX1());
			Log.debug("destin y1 : " + dam.getDestinationY1());
			Log.debug("*****************************************************************");
			//warn listeners a new image should be drawn.
			yAxisElement.setMinValue(freqInterval.getStart());
			yAxisElement.setMaxValue(freqInterval.getEnd());
			yAxisElement.setAvailableRange(new Range(freqInterval.getStart(),freqInterval.getEnd()));
			yAxisElement.setSelectedRange(new Range(freqInterval.getStart(),freqInterval.getEnd()));
			PlotConfig pc = new PlotConfig(newImage, dam, downloadRequestData.get(ID).isVisible(),ID );
			if(plotConfigList.containsKey(ID)){
				plotConfigList.get(ID).add(pc);
			}else{
				List<PlotConfig> tempList = new ArrayList<PlotConfig>();
				tempList.add(pc);
				plotConfigList.put(ID,tempList);
			}
			fireDrawNewBufferedImage(newImage, dam, ID, identifier);
		}
		Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}
	
	private void fireDrawNewBufferedImage(BufferedImage newImage,
			DrawableAreaMap dam, long iD, String identifier) {
		for (RadioPlotModelListener l : listeners){
			l.drawBufferedImage(newImage, dam);			
		}
		this.radioImagePane.setYAxisElement(yAxisElement);
		drawController.updateDrawableElement(this.radioImagePane, identifier);
		chartModel.chartRedrawRequest();
	}

	public Collection<PlotConfig> getPlotConfigurations(){
		List<PlotConfig> tempAllConfig = new ArrayList<PlotConfig>();
		for(List <PlotConfig> pc : plotConfigList.values()){
			tempAllConfig.addAll(pc);
		}
		return tempAllConfig;
		
	}
	
	private BufferedImage createBufferedImage( int width, int height, byte[] data, long ID) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        DataBufferByte dataBuffer = new DataBufferByte(data, width * height);
        //Log.debug("databuffer = " +  Arrays.toString(dataBuffer.getData()));
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

	@Override
	public void clearAllSavedImages() {
		this.plotConfigList.clear();
		
	}

	@Override
	public void downloadRequestDataRemoved(DownloadRequestData drd, long ID) {
		plotConfigList.remove(ID);
		downloadRequestData.remove(ID);
		drawController.removeDrawableElement(radioImagePane, drd.getPlotIdentifier());
		fireRemoveRadioImage(ID);		
	}

	private void fireRemoveRadioImage(long ID) {
		for (RadioPlotModelListener l : listeners){
			l.removeDownloadRequestData(ID);			
		}
		chartModel.chartRedrawRequest();
	}

	
	@Override
	public void downloadRequestDataVisibilityChanged(DownloadRequestData drd,
			long ID) {
		downloadRequestData.put(ID, drd);
		for(PlotConfig pc : this.plotConfigList.get(ID)){
			pc.setVisible(drd.isVisible());
		}
		fireChangeVisibility(ID, drd.isVisible());
	}
	
	private void fireChangeVisibility(long ID, boolean visible) {
		for (RadioPlotModelListener l : listeners){
			l.changeVisibility(ID);			
		}
		chartModel.chartRedrawRequest();
	}


	
}
