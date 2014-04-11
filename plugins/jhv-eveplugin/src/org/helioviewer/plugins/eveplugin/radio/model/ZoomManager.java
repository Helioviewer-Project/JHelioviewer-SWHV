package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Rectangle;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;
import org.helioviewer.plugins.eveplugin.radio.data.FrequencyInterval;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;

public class ZoomManager implements ZoomControllerListener,PlotAreaSpaceListener{
	private static ZoomManager instance; 
	private Map <String, ZoomManagerData> zoomManagerData;
	//private Map<String,Map<Long,ZoomDataConfig>> zoomDataConfigMap;
	//private List<ZoomManagerListener> zoomManagerListeners;
	//private boolean isAreaInitialized;
	private ZoomController zoomController;
	private Interval<Date> currentInterval;
	private PlotAreaSpaceManager plotAreaSpaceManager; 
	
	//private Map<String, Rectangle> displaySizeMap;
	
	private ZoomManager(){
		
		zoomController = ZoomController.getSingletonInstance();
		zoomController.addZoomControllerListener(this);
		//displaySizeMap = new HashMap<String, Rectangle>();
		zoomManagerData = new HashMap<String, ZoomManagerData>();
		plotAreaSpaceManager = PlotAreaSpaceManager.getInstance();
	}
	
	public static ZoomManager getSingletonInstance(){
		if (instance == null){
			instance = new ZoomManager();
		}
		return instance;
	}	
		
	public Rectangle getDisplaySize(String identifier) {
		ZoomManagerData zmd = getZoomManagerData(identifier);
		return zmd.getDisplaySize();
	}
	
	public void setDisplaySize(Rectangle displaySize, String identifier) {
		ZoomManagerData zmd = getZoomManagerData(identifier);
		Rectangle idenSize = zmd.getDisplaySize();
		if(!idenSize.equals(displaySize)){
			//if(!(displaySize.width <=0 || displaySize.height <= 0)){
			zmd.setDisplaySize(displaySize);
			//Log.debug("Display size for identifier " + identifier + "  is now: "+ displaySize.toString());
			fireDisplaySizeChanged(identifier);
		}	
	}
	
	public void calculateZoomXDirection(){
		
	}
	
	public void calculateZoomYDirection(){
		
	}
	
	public void calculateZoomXYDirection(){
		
	}
	
	public void addZoomDataConfig(FrequencyInterval freqInterval, Interval<Date> interval, ZoomDataConfigListener zoomDataConfigListener, long ID,String identifier){
		ZoomManagerData zmd = getZoomManagerData(identifier);
		if (currentInterval == null){
			this.currentInterval = interval;
		}
		if(freqInterval != null && interval !=null ){
			ZoomDataConfig config;
			if(zmd.isAreaInitialized()){
				config = new ZoomDataConfig(freqInterval.getStart(), freqInterval.getEnd(), currentInterval.getStart(), currentInterval.getEnd(), zmd.getDisplaySize(), ID);	
			}else{
				config = new ZoomDataConfig(freqInterval.getStart(), freqInterval.getEnd(), currentInterval.getStart(), currentInterval.getStart(), null, ID);
			}
			plotAreaSpaceManager.getPlotAreaSpace(identifier).addPlotAreaSpaceListener(config);
			Log.debug("PlotAreaSpaceListener added");
			zmd.addToZoomDataConfigMap(ID, config);
			config.addListener(zoomDataConfigListener);
		}
	}
	
	public void addZoomManagerListener(ZoomManagerListener listener, String identifier){
		ZoomManagerData zmd = getZoomManagerData(identifier);
		zmd.getListeners().add(listener);
	}
	
	public void removeZoomManagerListener(ZoomManagerListener listener, String identifier){
		ZoomManagerData zmd = getZoomManagerData(identifier);
		zmd.getListeners().remove(listener);
	}
	
	public void fireDisplaySizeChanged(String identifier){
		ZoomManagerData zmd = getZoomManagerData(identifier);
		List<ZoomManagerListener> zoomManagerListeners = zmd.getListeners();
		for (ZoomManagerListener l : zoomManagerListeners){
			l.displaySizeChanged(zmd.getDisplaySize());
		}		
	}
	
	public DrawableAreaMap getDrawableAreaMap(Date startDate, Date endDate,int startFrequency, int endFrequency, Rectangle area, long downloadID, String plotIdentifier){
		ZoomManagerData zmd = getZoomManagerData(plotIdentifier);
		ZoomDataConfig zdc = zmd.getZoomDataConfigMap().get(downloadID);
		int sourceX0 = defineXInSourceArea(startDate,startDate,endDate, area);
		int sourceY0 = defineYInSourceArea((int)zdc.getSelectedMaxY(), startFrequency, endFrequency, area,zdc);
		int sourceX1 = defineXInSourceArea(endDate, startDate, endDate, area);
		int sourceY1 = defineYInSourceArea((int)zdc.getSelectedMinY(), startFrequency, endFrequency, area,zdc);
		int destX0 = defineXInDestinationArea(startDate,zdc);
		int destY0 = defineYInDestinationArea(startFrequency,zdc);
		//int destY0 = defineYInDestinationArea((int)Math.floor(zdc.getSelectedMinY()),zdc);
		int destX1 = defineXInDestinationArea(endDate,zdc);
		//int destY1 = defineYInDestinationArea((int)Math.floor(zdc.getSelectedMaxY()),zdc);
		int destY1 = defineYInDestinationArea(endFrequency,zdc);
		return new DrawableAreaMap(sourceX0, sourceY0, sourceX1, sourceY1, destX0, destY0, destX1, destY1, downloadID);
	}

	private int defineYInDestinationArea(int frequencyToFind,ZoomDataConfig zdc) {
		return zdc.getDisplaySize().y + (int) Math.floor((frequencyToFind-zdc.getMinY())/(1.0*(zdc.getMaxY()-zdc.getMinY())/zdc.getDisplaySize().height)) ;
	}

	private int defineXInDestinationArea(Date dateToFind, ZoomDataConfig zdc ) {
		return  zdc.getDisplaySize().x + (int) Math.floor((dateToFind.getTime()-zdc.getMinX().getTime())/(1.0*(zdc.getMaxX().getTime()-zdc.getMinX().getTime())/zdc.getDisplaySize().width));
	}

	private int defineYInSourceArea(int frequencyToFind, int startFrequency,
			int endFrequency, Rectangle area, ZoomDataConfig zdc) {
		return (int) Math.floor((frequencyToFind-startFrequency)/(1.0*(endFrequency-startFrequency)/area.height)) ;
	}

	private int defineXInSourceArea(Date dateToFind, Date startDateArea,
		Date endDateArea, Rectangle area) {
		long timediff = dateToFind.getTime()-startDateArea.getTime();
		long timeOfArea = endDateArea.getTime()-startDateArea.getTime();
		/*Log.debug("timeDiff = " + timediff);
		Log.debug("timeOfArea = " + timeOfArea);
		Log.debug("area width = " + area.width);
		Log.debug("time per pix = " + (1.0*timeOfArea/area.width));
		Log.debug("timediff in pix = " + (timediff / (1.0*timeOfArea/area.width)));*/
		return (int) Math.floor(timediff/(1.0*(timeOfArea)/area.width));		
	}

	@Override
	public void availableIntervalChanged(Interval<Date> newInterval) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void selectedIntervalChanged(Interval<Date> newInterval) {
		currentInterval = newInterval;
		for(ZoomManagerData zmd : zoomManagerData.values()){
			for ( ZoomDataConfig zdc : zmd.getZoomDataConfigMap().values()){
				zdc.setMinX(newInterval.getStart());
				zdc.setMaxX(newInterval.getEnd());
				zdc.update();
			}
		}
	}

	@Override
	public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
		// TODO Auto-generated method stub
		
	}

	private ZoomManagerData getZoomManagerData(String identifier){
		ZoomManagerData zwd = zoomManagerData.get(identifier);
		if(zwd == null){
			zwd = new ZoomManagerData();
			zoomManagerData.put(identifier, zwd);
		}
		return zwd;
	}

	@Override
	public void plotAreaSpaceChanged(double scaledMinValue,
			double scaledMaxValue, double scaledMinTime, double scaledMaxTime,
			double scaledSelectedMinValue, double scaledSelectedMaxValue,
			double scaledSelectedMinTime, double scaledSelectedMaxTime) {
		// TODO Auto-generated method stub
		
	}
}
