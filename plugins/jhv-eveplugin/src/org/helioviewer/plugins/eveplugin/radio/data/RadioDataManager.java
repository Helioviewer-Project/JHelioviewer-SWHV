package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.radio.model.ResolutionSetting;
import org.helioviewer.plugins.eveplugin.radio.model.ZoomManager;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.ViewportAdapter;

public class RadioDataManager implements ViewListener, RadioDownloaderListener{//, ZoomControllerListener{
	private static RadioDataManager instance;
	private List<RadioDataManagerListener> listeners;
	private Map<Long, DownloadRequestData> downloadRequestData;
	private RadioImage previousRadioImage;
	private Interval maxTimeRange;
	private FrequencyInterval maxFrequencyInterval;
	private JHVJP2View currentJP2View;
	private boolean isCurrentJPX;
	private JP2Image currentJP2Image;
	
	private RadioDownloader downloader;
	private boolean eventReceived;
	private boolean acceptEvents;
	
	private ZoomController zoomController;
	
	private LineDataSelectorModel lineDataSelectorModel;
	
	private RadioDataManager(){
		listeners = new ArrayList<RadioDataManagerListener>();
		downloadRequestData = new HashMap<Long,DownloadRequestData>();
		downloader = RadioDownloader.getSingletonInstance();
		downloader.addRadioDownloaderListener(this);
		currentJP2View = null;
		isCurrentJPX = false;
		currentJP2Image = null;
		this.acceptEvents = false;
		this.eventReceived = false;
		zoomController = ZoomController.getSingletonInstance();
		//zoomController.addZoomControllerListener(this);
		lineDataSelectorModel = LineDataSelectorModel.getSingletonInstance();
	}
	
	public static RadioDataManager getSingletonInstance(){
		if(instance == null){
			instance = new RadioDataManager();
		}
		return instance;
	}
	
	public void addNewView(ImageInfoView view){
		
	}

	
	private void defineMaxBounds(Long ID) {
		DownloadRequestData drd = this.downloadRequestData.get(ID);
		if (drd != null){
			List<RadioImage> radioImages = drd.getRadioImages();
			if(!radioImages.isEmpty()){
				Date localMinDate = radioImages.get(0).getTimeInterval().getStart();
				Date localMaxDate = radioImages.get(0).getTimeInterval().getEnd();
				int localMinFrequency = radioImages.get(0).getFreqInterval().getStart();
				int localMaxFrequency = radioImages.get(0).getFreqInterval().getEnd();
				for(int i=1; i<radioImages.size();i++){
					if(radioImages.get(i).getTimeInterval().getStart().getTime()<localMinDate.getTime()){
						localMinDate = radioImages.get(i).getTimeInterval().getStart();
					}
					if(radioImages.get(i).getTimeInterval().getEnd().getTime()>localMaxDate.getTime()){
						localMaxDate = radioImages.get(i).getTimeInterval().getEnd();
					}
					if(radioImages.get(i).getFreqInterval().getStart()<localMinFrequency){
						localMinFrequency = radioImages.get(i).getFreqInterval().getStart();
					}
					if(radioImages.get(i).getFreqInterval().getEnd()>localMaxFrequency){
						localMaxFrequency = radioImages.get(i).getFreqInterval().getEnd();
					}
				}
				this.maxFrequencyInterval = new FrequencyInterval(localMinFrequency, localMaxFrequency);
				this.maxTimeRange = new Interval<Date>(localMinDate, localMaxDate);
			}
		}
	}

	public void addRadioManagerListener(RadioDataManagerListener l){
		listeners.add(l);
	}
	
	public void removeRadioManagerListener(RadioDataManagerListener l){
		listeners.remove(l);
	}
	
	@Override
	public void viewChanged(View sender, ChangeEvent aEvent) {
		Log.debug("Sender object = " + sender);
		try {
			throw new Exception();
		} catch (Exception e1) {
			Log.error("Who send the event", e1);
			//e1.printStackTrace();
		}
		Log.debug("What event" + aEvent);
		try {
			throw new Exception();
		} catch (Exception e1) {
			Log.error("Who send the event", e1);
			//e1.printStackTrace();
		}
		if(acceptEvents){ //we only accept events if we changed the framenumber
		//if (true){	
			if(aEvent.reasonOccurred(SubImageDataChangedReason.class) && aEvent.reasonOccurred(TimestampChangedReason.class)){ 
				Log.info("new view changed event");
				Log.info(sender.toString());
				Log.info(aEvent.toString());
				eventReceived = true;
			}else{
				Log.info("ignore event");
				Log.info(sender.toString());
				Log.info(aEvent.toString());
			}
		}else{
			Log.debug("Event not excepted. Not changed the framenumber");
			Log.info(sender.toString());
			Log.info(aEvent.toString());
		}
		/*eventReceived = true;
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	public void setVisibleParameters(Interval<Date> timeRange, FrequencyInterval freqInterval, Rectangle areaAvailable){
		double timeRatio = 0.0;
		double freqRatio = 0.0;
		if(timeRange !=null){
			timeRatio = (1.0*(timeRange.getEnd().getTime() - timeRange.getStart().getTime()))/areaAvailable.width;
		}else{
			
		}
		if(freqInterval != null){
			freqRatio = (1.0*(freqInterval.getEnd()-freqInterval.getStart())/areaAvailable.height);
		}else{
			
		}
		Log.debug("new time ratio = " + timeRatio + " ms/pixel");
		Log.debug("new freq ratio = " + freqRatio + " mhz/pixel");
	}

	@Override
	public void newImageViewDownloaded(ImageInfoView view,
			Date requestedStartTime, Date requestedEndTime, long ID, String identifier) {
		
		//radioImages.clear();
		Log.debug("new view received : " + view);
		URI uri;
		try {
			/*uri = new URI("jpip://swhv.oma.be:8091/movies/ROB-Humain_CALLISTO_CALLISTO_RADIOGRAM_F2011-09-24T00.00.00Z_T2011-09-26T00.00.00ZB3600.jpx");
			URI downloadURI = new URI("jpip://swhv.oma.be:8091/movies/ROB-Humain_CALLISTO_CALLISTO_RADIOGRAM_F2011-09-24T00.00.00Z_T2011-09-26T00.00.00ZB3600.jpx")
			JP2Image image = new JP2Image(uri, downloadURI);*/
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			JHVJPXView jpxView = view.getAdapter(JHVJPXView.class);
			if (jpxView != null){
				jpxView.addViewListener(this);
				JP2Image image = jpxView.getJP2Image();
				currentJP2View = jpxView;
				isCurrentJPX = true;
				currentJP2Image = currentJP2View.getJP2Image();
				ResolutionSet rs = image.getResolutionSet();
				Log.debug("the resolution set : " +rs.toString());
				for (int i = 0; i < rs.getMaxResolutionLevels();i++){
					Log.debug("resolution level " + i + " : " + rs.getResolutionLevel(i));
				}
				Log.debug("++++++++++++++++++++++++++++++++++++++");
				Interval<Integer> interval = image.getCompositionLayerRange();
				Log.debug("the interval is : " + interval);
				Log.debug("the start of the interval : " + interval.getStart());
				Log.debug("the end of the interval : "+ interval.getEnd());
				DownloadRequestData drd = new DownloadRequestData(ID,identifier);
				drd.setDownloading(true);
				lineDataSelectorModel.addLineData(drd); 
				LineDataSelectorModel.getSingletonInstance().downloadStarted(drd);
				for(int i=interval.getStart(); i<= interval.getEnd(); i++){
					Log.debug("BITPIX : " + image.get("BITPIX",i));
					Log.debug("NAXIS1 : " + image.get("NAXIS1",i));
					Log.debug("NAXIS2 : " + image.get("NAXIS2",i));
					Log.debug("DATE-OBS : " + image.get("DATE-OBS",i));
					Log.debug("TELESCOP : " + image.get("TELESCOP",i));
					Log.debug("INSTRUME : " + image.get("INSTRUME",i));
					Log.debug("DETECTOR : " + image.get("DETECTOR",i));
					Log.debug("WAVELNTH : " + image.get("WAVELNTH",i));
					Log.debug("DATE-END : " + image.get("DATE-END",i));
					Log.debug("TIMEDELT : " + image.get("TIMEDELT",i));
					Log.debug("STARTFREQ : " + image.get("STARTFRQ",i));
					Log.debug("END-FREQ : " + image.get("END-FREQ",i));
					Log.debug("FREQDELT : " + image.get("FREQDELT",i));
					Log.debug("CDELT1 : " + image.get("CDELT1",i));
					Log.debug("CDELT2 : " + image.get("CDELT2",i));
					Log.debug("CRPIX1 : " + image.get("CRPIX1",i));
					Log.debug("CRPIX2 : " + image.get("CRPIX2",i));
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					FrequencyInterval fi = new FrequencyInterval(
							Integer.parseInt(image.get("STARTFRQ",i)),
							Integer.parseInt(image.get("END-FREQ",i)));
					Log.debug("Ratios : ");
					Date start = null;
					Date end = null;
					try {
						start = sdf.parse(image.get("DATE-OBS",i));
						end = sdf.parse(image.get("DATE-END",i));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.error("Could not parse  "+ image.get("DATE-OBS",i) +" or "+ image.get("DATE-END",i));
					}
					List<ResolutionSetting> resolutionSettings = new ArrayList<ResolutionSetting>();
					if(start !=null && end != null){
						Double freqStart = Double.parseDouble(image.get("STARTFRQ",i));
						Double freqEnd = Double.parseDouble(image.get("END-FREQ",i));
						Interval<Date> dateInterval = new Interval<Date>(start,end);
						for (int j = 0; j < rs.getMaxResolutionLevels();j++){
							ResolutionSetting tempResSet = new ResolutionSetting(
									(1.0*(end.getTime()-start.getTime())/rs.getResolutionLevel(j).getResolutionBounds().width), 
									((freqEnd-freqStart)/rs.getResolutionLevel(j).getResolutionBounds().height), 
									j, 
									rs.getResolutionLevel(j).getResolutionBounds().width,
									rs.getResolutionLevel(j).getResolutionBounds().height, 
									rs.getResolutionLevel(j).getZoomLevel());
							resolutionSettings.add(tempResSet);
							Log.debug("resolution level " + j + " : " +rs.getResolutionLevel(j));
							Log.debug("Frequency Ratio : " + ((freqEnd-freqStart)/rs.getResolutionLevel(j).getResolutionBounds().height));
							Log.debug("Date ratio: " + (1.0*(end.getTime()-start.getTime())/rs.getResolutionLevel(j).getResolutionBounds().width));
							Log.debug("************************************");
						}
						RadioImage tempRs = new RadioImage(ID,dateInterval,fi,i,rs,resolutionSettings,identifier);
						drd.addRadioImage(tempRs);
					}else{
						Log.debug("Start and/or stop is null");
					}
					Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					
				}
				this.downloadRequestData.put(ID, drd);
				defineMaxBounds(ID);
				
				//fireNewGlobalFrequencyInterval(maxFrequencyInterval);
				fireNewDataAvailable(drd, ID);
				fireDownloadRequestAnswered(maxFrequencyInterval, new Interval<Date>(requestedStartTime,requestedEndTime), ID, identifier);
				
			}else{
				JHVJP2View jp2View = view.getAdapter(JHVJP2View.class);
				jp2View.addViewListener(this);
				JP2Image image = jp2View.getJP2Image();
				currentJP2View = jp2View;
				currentJP2Image = currentJP2View.getJP2Image();
				ResolutionSet rs = image.getResolutionSet();
				Log.debug("the resolution set : " +rs.toString());
				for (int i = 0; i < rs.getMaxResolutionLevels();i++){
					Log.debug("resolution level " + i + " : " + rs.getResolutionLevel(i));
				}
				Log.debug("++++++++++++++++++++++++++++++++++++++");
				Interval<Integer> interval = image.getCompositionLayerRange();
				Log.debug("the interval is : " + interval);
				Log.debug("the start of the interval : " + interval.getStart());
				Log.debug("the end of the interval : "+ interval.getEnd());
				DownloadRequestData drd = new DownloadRequestData(ID,identifier);
				drd.setDownloading(true);
				lineDataSelectorModel.addLineData(drd); 
				LineDataSelectorModel.getSingletonInstance().downloadStarted(drd);
				for(int i=interval.getStart(); i<= interval.getEnd(); i++){
					Log.debug("BITPIX : " + image.get("BITPIX",i));
					Log.debug("NAXIS1 : " + image.get("NAXIS1",i));
					Log.debug("NAXIS2 : " + image.get("NAXIS2",i));
					Log.debug("DATE-OBS : " + image.get("DATE-OBS",i));
					Log.debug("TELESCOP : " + image.get("TELESCOP",i));
					Log.debug("INSTRUME : " + image.get("INSTRUME",i));
					Log.debug("DETECTOR : " + image.get("DETECTOR",i));
					Log.debug("WAVELNTH : " + image.get("WAVELNTH",i));
					Log.debug("DATE-END : " + image.get("DATE-END",i));
					Log.debug("TIMEDELT : " + image.get("TIMEDELT",i));
					Log.debug("STARTFREQ : " + image.get("STARTFRQ",i));
					Log.debug("END-FREQ : " + image.get("END-FREQ",i));
					Log.debug("FREQDELT : " + image.get("FREQDELT",i));
					Log.debug("CDELT1 : " + image.get("CDELT1",i));
					Log.debug("CDELT2 : " + image.get("CDELT2",i));
					Log.debug("CRPIX1 : " + image.get("CRPIX1",i));
					Log.debug("CRPIX2 : " + image.get("CRPIX2",i));
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					FrequencyInterval fi = new FrequencyInterval(
							Integer.parseInt(image.get("STARTFRQ",i)),
							Integer.parseInt(image.get("END-FREQ",i)));
					Log.debug("Ratios : ");
					Date start = null;
					Date end = null;
					try {
						start = sdf.parse(image.get("DATE-OBS",i));
						end = sdf.parse(image.get("DATE-END",i));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.error("Could not parse  "+ image.get("DATE-OBS",i) +" or "+ image.get("DATE-END",i));
					}
					List<ResolutionSetting> resolutionSettings = new ArrayList<ResolutionSetting>();
					if(start !=null && end != null){
						Double freqStart = Double.parseDouble(image.get("STARTFRQ",i));
						Double freqEnd = Double.parseDouble(image.get("END-FREQ",i));
						Interval<Date> dateInterval = new Interval<Date>(start,end);
						for (int j = 0; j < rs.getMaxResolutionLevels();j++){
							ResolutionSetting tempResSet = new ResolutionSetting(
									(1.0*(end.getTime()-start.getTime())/rs.getResolutionLevel(j).getResolutionBounds().width), 
									((freqEnd-freqStart)/rs.getResolutionLevel(j).getResolutionBounds().height), 
									j, 
									rs.getResolutionLevel(j).getResolutionBounds().width,
									rs.getResolutionLevel(j).getResolutionBounds().height, 
									rs.getResolutionLevel(j).getZoomLevel());
							resolutionSettings.add(tempResSet);
							Log.debug("resolution level " + j + " : " +rs.getResolutionLevel(j));
							Log.debug("Frequency Ratio : " + ((freqEnd-freqStart)/rs.getResolutionLevel(j).getResolutionBounds().height));
							Log.debug("Date ratio: " + (1.0*(end.getTime()-start.getTime())/rs.getResolutionLevel(j).getResolutionBounds().width));
							Log.debug("************************************");
						}
						RadioImage tempRs = new RadioImage(ID,dateInterval,fi,i,rs,resolutionSettings,identifier);
						drd.addRadioImage(tempRs);
					}else{
						Log.debug("Start and/or stop is null");
					}
					Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					
				}
				this.downloadRequestData.put(ID, drd);
				defineMaxBounds(ID);
				
				//fireNewGlobalFrequencyInterval(maxFrequencyInterval);
				fireNewDataAvailable(drd, ID);
				fireDownloadRequestAnswered(maxFrequencyInterval, new Interval<Date>(requestedStartTime,requestedEndTime), ID, identifier);
			}			
		/*} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();*/
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
	}

	private void fireNewDataAvailable(DownloadRequestData drd, long iD) {
		for(RadioDataManagerListener l : listeners){
			l.newDataAvailable(drd, iD);
		}	
		
	}

	public void removeDownloadRequestData(DownloadRequestData drd){
		this.downloadRequestData.remove(drd.getID());
		lineDataSelectorModel.removeLineData(drd);
		fireDownloadRequestDataRemoved(drd);
	}
	
	private void fireDownloadRequestDataRemoved(DownloadRequestData drd) {
		for (RadioDataManagerListener l : listeners){
			l.downloadRequestDataRemoved(drd, drd.getID());
		}		
	}

	public void downloadRequestDataVisibilityChanged(DownloadRequestData drd){
		fireDownloadRequestDataVisibilityChanged(drd);
	}
	
	private void fireDownloadRequestDataVisibilityChanged(DownloadRequestData drd) {
		for (RadioDataManagerListener l : listeners){
			l.downloadRequestDataVisibilityChanged(drd, drd.getID());
		}
		lineDataSelectorModel.lineDataElementUpdated(drd);
	}

	private void fireDownloadRequestAnswered(FrequencyInterval freqInterval, Interval<Date> timeInterval, long ID, String identifier){
		for (RadioDataManagerListener l : listeners){
			l.downloadRequestAnswered(freqInterval, timeInterval, ID, identifier);
		}
	}
	
	/*private void fireNewGlobalFrequencyInterval(
			FrequencyInterval maxFrequencyInterval) {
		for (RadioDataManagerListener l : listeners){
			l.newGlobalFrequencyInterval(maxFrequencyInterval);
		}
		
	}*/

	public synchronized void requestForData(Date xStart, Date xEnd, double yStart, double yEnd,
			double xRatio, double yRatio, List<Long> iDs) {
		if(isCurrentJPX){
			JHVJPXView tempCurrent = currentJP2View.getAdapter(JHVJPXView.class);
			if(!(xRatio < 0) && !(yRatio < 0)){
				fireClearAllSavedImages();
				for (Long id : iDs){
					DownloadRequestData drd = this.downloadRequestData.get(id);
					if (drd != null){
						drd.setDownloading(true);
						lineDataSelectorModel.downloadStarted(downloadRequestData.get(id));
						if(currentJP2Image != null && tempCurrent != null){
							Log.debug("Received request for data: ");
							Log.debug("x Start : "+ xStart.toString());
							Log.debug("x End : "+ xEnd.toString());
							Log.debug("y Start : "+ yStart);
							Log.debug("y End : " + yEnd);
							Log.debug("x Ratio : " + xRatio);
							Log.debug("y Ratio : " + yRatio);
							Log.debug("Search best resolution");
							ChangeEvent e = new ChangeEvent();
							Interval<Date> completeInterval = new Interval<Date>(xStart,xEnd);
							FrequencyInterval completeFreqInterval = new FrequencyInterval((int)Math.round(yStart),(int)Math.round(yEnd));
							List<RadioImage> radioImages = downloadRequestData.get(id).getRadioImages();
							for (RadioImage tempIm : radioImages){
								ResolutionSetting rs = tempIm.defineBestResolutionSetting(xRatio, yRatio);
							
								Log.debug("Resolution level : "+ rs.getResolutionLevel());
								Log.debug("Ratio x : " + rs.getxRatio());
								Log.debug("Ratio y : " + rs.getyRatio());
							
								if(tempIm.withinInterval(completeInterval,completeFreqInterval)){
									currentJP2View.setViewport(new ViewportAdapter(new StaticViewport(rs.getVec2dIntRepresenation())), e);
									Log.debug("Viewport set");
									acceptEvents = true;
									Log.debug("Will set the timestamp to " + tempIm.getFrameInJPX());
									tempCurrent.setCurrentFrame(tempIm.getFrameInJPX(), e, true);
									Log.debug("Changed the viewport and the timestamp wait for the event :-)");
									byte[] data = new byte[0];
									while(!eventReceived){
										Log.debug("Wait for event");
										try {
											Thread.sleep(10);
										} catch (InterruptedException ex) {
											// TODO Auto-generated catch block
											ex.printStackTrace();
										}
									}
									eventReceived = false;
									acceptEvents = false;
									SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData)(tempCurrent.getSubimageData());
									Byte8ImageTransport bytetrs =  (Byte8ImageTransport) imageData.getImageTransport();
									//byte[] data = bytetrs.getByte8PixelData();
									//byte[] newData  = new byte[data.length];
									data = bytetrs.getByte8PixelData();
									Log.debug("Length of the data"+data.length);
									int width = imageData.getWidth();
									int height = imageData.getHeight();
									Log.debug("width = "+ width);
									Log.debug("height = " + height);
									fireNewDataReceived(data, tempIm.getTimeInterval(),tempIm.getFreqInterval(), new Rectangle(rs.getWidth(),rs.getHeight()), id, drd.getPlotIdentifier());
								}
							}
						}
						downloadRequestData.get(id).setDownloading(false);
						lineDataSelectorModel.downloadFinished(downloadRequestData.get(id));
					}
				}
			}else{
				Log.error("One of the ratios where negative");
				Log.error("xratio : " + xRatio);
				Log.error("yratio : " + yRatio);
			}
		}else{
			if(!(xRatio < 0) && !(yRatio < 0)){
				fireClearAllSavedImages();
				for (Long id : iDs){
					DownloadRequestData drd = this.downloadRequestData.get(id);
					if (drd != null){
						drd.setDownloading(true);
						lineDataSelectorModel.downloadStarted(downloadRequestData.get(id));
						if(currentJP2Image != null && currentJP2View != null){
							Log.debug("Received request for data: ");
							Log.debug("x Start : "+ xStart.toString());
							Log.debug("x End : "+ xEnd.toString());
							Log.debug("y Start : "+ yStart);
							Log.debug("y End : " + yEnd);
							Log.debug("x Ratio : " + xRatio);
							Log.debug("y Ratio : " + yRatio);
							Log.debug("Search best resolution");
							ChangeEvent e = new ChangeEvent();
							Interval<Date> completeInterval = new Interval<Date>(xStart,xEnd);
							FrequencyInterval completeFreqInterval = new FrequencyInterval((int)Math.round(yStart),(int)Math.round(yEnd));
							List<RadioImage> radioImages = downloadRequestData.get(id).getRadioImages();
							for (RadioImage tempIm : radioImages){
								ResolutionSetting rs = tempIm.defineBestResolutionSetting(xRatio, yRatio);
							
								Log.debug("Resolution level : "+ rs.getResolutionLevel());
								Log.debug("Ratio x : " + rs.getxRatio());
								Log.debug("Ratio y : " + rs.getyRatio());
							
								if(tempIm.withinInterval(completeInterval,completeFreqInterval)){
									currentJP2View.setViewport(new ViewportAdapter(new StaticViewport(rs.getVec2dIntRepresenation())), e);
									Log.debug("Viewport set");
									acceptEvents = true;
									Log.debug("Changed the viewport");
									byte[] data = new byte[0];
									while(!eventReceived){
										Log.debug("Wait for event");
										try {
											Thread.sleep(10);
										} catch (InterruptedException ex) {
											// TODO Auto-generated catch block
											ex.printStackTrace();
										}
									}
 									SingleChannelByte8ImageData imageData = (SingleChannelByte8ImageData)(currentJP2View.getSubimageData());
									Byte8ImageTransport bytetrs =  (Byte8ImageTransport) imageData.getImageTransport();
									//byte[] data = bytetrs.getByte8PixelData();
									//byte[] newData  = new byte[data.length];
									data = bytetrs.getByte8PixelData();
									Log.debug("Length of the data"+data.length);
									int width = imageData.getWidth();
									int height = imageData.getHeight();
									Log.debug("width = "+ width);
									Log.debug("height = " + height);
									fireNewDataReceived(data, tempIm.getTimeInterval(),tempIm.getFreqInterval(), new Rectangle(rs.getWidth(),rs.getHeight()), id, drd.getPlotIdentifier());
								}
							}
						}
						downloadRequestData.get(id).setDownloading(false);
						lineDataSelectorModel.downloadFinished(downloadRequestData.get(id));
					}
				}
			}else{
				Log.error("One of the ratios where negative");
				Log.error("xratio : " + xRatio);
				Log.error("yratio : " + yRatio);
			}
		}
	}

	private void fireClearAllSavedImages() {
		for(RadioDataManagerListener l : listeners){
			l.clearAllSavedImages();
		}	
	}

	private void fireNewDataReceived(byte[] data, Interval<Date> timeInterval,
			FrequencyInterval freqInterval, Rectangle areaSize, Long ID, String identifier) {
		List<Long> tempList = new ArrayList<Long>();
		tempList.add(ID);
		for(RadioDataManagerListener l : listeners){
			l.newDataReceived(data, timeInterval, freqInterval, areaSize, tempList, identifier);
		}		
	}

	/*@Override
	public void availableIntervalChanged(Interval<Date> newInterval) {}

	@Override
	public void selectedIntervalChanged(Interval<Date> newInterval) {
		//fireDownloadRequestAnswered(maxFrequencyInterval, newInterval, 0);
		requestForData(newInterval.getStart(),newInterval.getEnd(), yStart, yEnd, xRatio, yRatio, ID)
	}

	@Override
	public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
		// TODO Auto-generated method stub
		
	}*/
}
