package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.radio.model.ResolutionSetting;
import org.helioviewer.plugins.eveplugin.radio.test.DataChecker;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View.ReaderMode;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.ViewportAdapter;


public class RadioDataManager implements RadioDownloaderListener{//,ViewListener{//, ZoomControllerListener{
	private static RadioDataManager instance;
	private List<RadioDataManagerListener> listeners;
	private Map<Long, DownloadRequestData> downloadRequestData;
	private RadioImage previousRadioImage;
	private Interval<Date> maxTimeRange;
	private FrequencyInterval maxFrequencyInterval;
	private JHVJP2View currentJP2View;
	private boolean isCurrentJPX;
	private JP2Image currentJP2Image;
	
	private RadioDownloader downloader;
	private boolean eventReceived;
	//private boolean acceptEvents;
	private boolean expectEvent;
	private int latestComplete;
	private int waitingForFrame;
	private DataChecker dataChecker;
	
	private RequestForDataBuffer requestBuffer;
	
	private ZoomController zoomController;	
	
	private LineDataSelectorModel lineDataSelectorModel;
	
	private RadioImageCache cache;
	
	//private RadioImageTestFrame testFrame;
	//private SendDataTestFrame sendDataTestFrame;
	
	private RadioDataManager(){
		//testFrame = new RadioImageTestFrame();
		//sendDataTestFrame = new SendDataTestFrame();
		listeners = new ArrayList<RadioDataManagerListener>();
		downloadRequestData = new HashMap<Long,DownloadRequestData>();
		downloader = RadioDownloader.getSingletonInstance();
		downloader.addRadioDownloaderListener(this);
		currentJP2View = null;
		isCurrentJPX = false;
		currentJP2Image = null;
		//this.acceptEvents = false;
		this.eventReceived = false;
		zoomController = ZoomController.getSingletonInstance();
		//zoomController.addZoomControllerListener(this);
		lineDataSelectorModel = LineDataSelectorModel.getSingletonInstance();
		latestComplete = -1;
		waitingForFrame = -1;
		expectEvent = false;
		dataChecker = new DataChecker();
		//addRadioManagerListener(dataChecker);
		//addRadioManagerListener(sendDataTestFrame);
		cache = RadioImageCache.getInstance();
		requestBuffer = new RequestForDataBuffer();
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
		synchronized (downloadRequestData) {	
			DownloadRequestData drd = this.downloadRequestData.get(ID);
			if (drd != null){
				Map<Long,RadioImage> radioImages = drd.getRadioImages();
				if(!radioImages.isEmpty()){
					Date localMinDate = null; 
					Date localMaxDate = null;
					int localMinFrequency = -1;
					int localMaxFrequency = -1;
					boolean first = true;
					for(RadioImage image : radioImages.values()){
						if(first){
							localMinDate = image.getTimeInterval().getStart();
							localMaxDate = image.getTimeInterval().getEnd();
							localMinFrequency = image.getFreqInterval().getStart();
							localMaxFrequency = image.getFreqInterval().getEnd();
							first = false;
						}else{
							if(image.getTimeInterval().getStart().getTime()<localMinDate.getTime()){
								localMinDate = image.getTimeInterval().getStart();
							}
							if(image.getTimeInterval().getEnd().getTime()>localMaxDate.getTime()){
								localMaxDate = image.getTimeInterval().getEnd();
							}
							if(image.getFreqInterval().getStart()<localMinFrequency){
								localMinFrequency = image.getFreqInterval().getStart();
							}
							if(image.getFreqInterval().getEnd()>localMaxFrequency){
								localMaxFrequency = image.getFreqInterval().getEnd();
							}
						}
					}
					this.maxFrequencyInterval = new FrequencyInterval(localMinFrequency, localMaxFrequency);
					this.maxTimeRange = new Interval<Date>(localMinDate, localMaxDate);
					//}
				}
			}
		}
	}

	public void addRadioManagerListener(RadioDataManagerListener l){
		listeners.add(l);
	}
	
	public void removeRadioManagerListener(RadioDataManagerListener l){
		listeners.remove(l);
	}
	
	public synchronized void dataForIDReceived(byte[] data, Long imageID, Long downloadID){
		//fireClearImageForID(downloadID, imageID);
		fireDataforIDReceived(data, imageID, downloadID);
	}
	
	private void fireClearImageForID(Long downloadID, Long imageID, String plotIdentifier) {
		for(RadioDataManagerListener l : listeners){
			l.clearAllSavedImagesForID(downloadID, imageID, plotIdentifier);
		}
		
	}

	private void fireDataforIDReceived(byte[] data, Long imageID,
			Long downloadID) {
		synchronized(downloadRequestData){
			DownloadRequestData drd = downloadRequestData.get(downloadID);
			if(drd != null){
				RadioImage image = drd.getRadioImages().get(imageID);
				if(image != null){
					
					for (RadioDataManagerListener l : listeners){
						l.newDataForIDReceived(data, image.getTimeInterval(),image.getFreqInterval(), image.getLastUsedResolutionSetting().getRectangleRepresentation(), downloadID,drd.getPlotIdentifier(), imageID);
					}
				}else{
					Log.debug("The image was null");
				}
			}else{
				Log.debug("Download request data was null");
			}
		}
		
	}

	//@Override
	/*public void viewChanged(View sender, ChangeEvent aEvent) {
		Log.debug("Sender object = " + sender);
		Log.debug("Event number : "+ aEvent.hashCode());
		RadioRequestReason rrr = aEvent.getLastChangedReasonByType(RadioRequestReason.class);
		if (rrr != null){
			Log.debug("------------------ ID of request " + rrr.getID() + "------------------");
		}else{
			Log.debug("------------------ rrr was null ---------------------");
		}
		CacheStatusChangedReason cscr = aEvent.getLastChangedReasonByType(CacheStatusChangedReason.class);
		if (cscr != null){
			if (cscr.getType() == CacheType.COMPLETE){
				if(cscr.getValue()>latestComplete){
					latestComplete = cscr.getValue();
				}
				Log.debug("Latest complete is : " + latestComplete);
				/*if (waitingForFrame == cscr.getValue()){
					eventReceived = true;
					expectEvent = false;
				}*/
			/*}
		}
		if (expectEvent){
			JHVJPXView jpxView = sender.getAdapter(JHVJPXView.class);
			if (jpxView != null){
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
				//if(acceptEvents){ //we only accept events if we changed the framenumber
				//if (true){	
					if(aEvent.reasonOccurred(SubImageDataChangedReason.class)){// && aEvent.reasonOccurred(TimestampChangedReason.class)){ 
						Log.info("new view changed event");
						Log.info(sender.toString());
						Log.info(aEvent.toString());
						eventReceived = true;
						expectEvent = false;
					}else{
						Log.info("ignore event");
						Log.info(sender.toString());
						Log.info(aEvent.toString());
					}*/
				//}else{
					//Log.debug("Event not excepted. Not changed the framenumber");
					//Log.info(sender.toString());
					//Log.info(aEvent.toString());
				//}
				/*eventReceived = true;
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			/*}else{
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
				//if(acceptEvents){ //we only accept events if we changed the framenumber
				//if (true){	
					if(aEvent.reasonOccurred(SubImageDataChangedReason.class) && aEvent.reasonOccurred(TimestampChangedReason.class)){ 
						Log.info("new view changed event");
						Log.info(sender.toString());
						Log.info(aEvent.toString());
						eventReceived = true;
						expectEvent = false;
					}else{
						Log.info("ignore event");
						Log.info(sender.toString());
						Log.info(aEvent.toString());
					}*/
				//}else{
				//	Log.debug("Event not excepted. Not changed the framenumber");
				//	Log.info(sender.toString());
				//	Log.info(aEvent.toString());
				//}
				/*eventReceived = true;
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			/*}
		}else{
			Log.debug("We didn't expect the event");
		}
	}*/
	
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
			Date requestedStartTime, Date requestedEndTime, long ID, String identifier){
		Log.debug("Init the downloadrequestdata");
		synchronized(downloadRequestData){
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
					//jpxView.addViewListener(this);
					//jpxView.addViewListener(testFrame);
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
							RadioImage tempRs = new RadioImage(null,ID, Math.round(1000000*Math.random()),dateInterval,fi,i,rs,resolutionSettings,identifier, true);
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
					//jp2View.addViewListener(this);
					//jp2View.addViewListener(testFrame);
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
							RadioImage tempRs = new RadioImage(null,ID, Math.round(1000000*Math.random()),dateInterval,fi,i,rs,resolutionSettings,identifier, true);
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
		
	}

	private void fireNewDataAvailable(DownloadRequestData drd, long iD) {
		for(RadioDataManagerListener l : listeners){
			l.newDataAvailable(drd, iD);
		}	
		
	}

	public void removeDownloadRequestData(DownloadRequestData drd){
		this.downloadRequestData.remove(drd.getDownloadID());
		lineDataSelectorModel.removeLineData(drd);
		for(Long imageID : drd.getRadioImages().keySet()){
			cache.remove(imageID,drd.getPlotIdentifier());
		}
		fireDownloadRequestDataRemoved(drd);
	}
	
	private void fireDownloadRequestDataRemoved(DownloadRequestData drd) {
		for (RadioDataManagerListener l : listeners){
			l.downloadRequestDataRemoved(drd, drd.getDownloadID());
		}		
	}

	public void downloadRequestDataVisibilityChanged(DownloadRequestData drd){
		fireDownloadRequestDataVisibilityChanged(drd);
	}
	
	private void fireDownloadRequestDataVisibilityChanged(DownloadRequestData drd) {
		for (RadioDataManagerListener l : listeners){
			l.downloadRequestDataVisibilityChanged(drd, drd.getDownloadID());
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

	public void requestForData(Date xStart, Date xEnd, double yStart, double yEnd,
			double xRatio, double yRatio, List<Long> iDs, String plotIdentifier) {
		Log.debug("Request for data");
		try {
			throw new Exception();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		if(!requestBuffer.hasData()){
			requestBuffer.addRequestConfig(new RequestConfig(xStart, xEnd, yStart,yEnd, xRatio,yRatio,iDs));
			while(requestBuffer.hasData()){
				RequestConfig requestConfig = requestBuffer.getData();
				synchronized (downloadRequestData) {		
					Log.debug("Request for data in interval "+ requestConfig.getxStart() + " - "+ requestConfig.getxEnd());
					if(requestConfig.getxEnd().getTime() - requestConfig.getxStart().getTime() > EVESettings.MAXIMUM_INTERVAL_RANGE_MILLI_SEC_REQ){
						// TODO handle to big interval
						Log.debug("Interval too big");
						for(Long id : requestConfig.getIDs()){
							fireIntervalTooBig(id, plotIdentifier);
						}
					}else{
						RadioImageCacheResult result = cache.getRadioImageCacheResultForInterval(requestConfig.getxStart(), requestConfig.getxEnd(), 24L*60*60*1000, plotIdentifier);
						for(Long id : requestConfig.getIDs()){
							DownloadRequestData drd = downloadRequestData.get(id);
							if(drd != null){
								Log.debug("Dowload request data plotIdentifier : " + drd.getPlotIdentifier());
								Log.debug("Downloaded id: "+id);
								Log.debug("Request new data not yet downloaded");
								Log.debug(result.getMissingInterval().size());
								downloader.requestAndOpenIntervals(result.getMissingInterval(), id,drd.getPlotIdentifier(), requestConfig.getxRatio(), requestConfig.getyRatio());
							}else{
								Log.debug("drd is null");
							}
						}
						for(Long id : requestConfig.getIDs()){
							DownloadRequestData drd = downloadRequestData.get(id);
							if(drd != null){
								fireClearSavedImages(id, result.getToRemove(), drd.getPlotIdentifier());
								for(DownloadedJPXData jpxData : result.getAvailableData()){
									RadioImage ri = drd.getRadioImages().get(jpxData.getImageID());
									if(ri != null){
										ResolutionSetting rs = ri.defineBestResolutionSetting(xRatio, yRatio);
										if(rs != ri.getLastUsedResolutionSetting()){
											Log.debug("Other resolution setting: update the viewport for "+ jpxData.getImageID());
											Log.debug("radio image last resolutionsetting : "+ ri.getLastUsedResolutionSetting());
											Log.debug("new resolution setting : " + rs);
											View v = jpxData.getView();
											JHVJP2View jp2View = v.getAdapter(JHVJP2View.class);
											if(jp2View != null){
												jp2View.setViewport(new ViewportAdapter(new StaticViewport(rs.getVec2dIntRepresentation())), new ChangeEvent());
												ri.setLastUsedResolutionSetting(rs);
											}
										}else{
											Log.debug("Same resolution setting don't update viewport");
											fireDataNotChanged(ri.getTimeInterval(),ri.getFreqInterval(), new Rectangle(rs.getWidth(),rs.getHeight()), id, drd.getPlotIdentifier(),ri.getRadioImageID());
										}
									}
								}
							}
						}
					}
				}
			}
		}else{
			Log.debug("Add request to buffer");
			requestBuffer.addRequestConfig(new RequestConfig(xStart, xEnd, yStart,yEnd, xRatio,yRatio,iDs));
		}
		
		/*synchronized (downloadRequestData) {		
			Log.debug("Request for data in interval "+ xStart + " - "+ xEnd);
			if(xEnd.getTime() - xStart.getTime() > EVESettings.MAXIMUM_INTERVAL_RANGE_MILLI_SEC){
				// TODO handle to big interval
			}else{
				RadioImageCacheResult result = cache.getRadioImageCacheResultForInterval(xStart, xEnd, 24L*60*60*1000);
				for(Long id : iDs){
					DownloadRequestData drd = downloadRequestData.get(id);
					if(drd != null){
						downloader.requestAndOpenIntervals(result.getMissingInterval(), id,drd.getPlotIdentifier(), xRatio, yRatio);
					}
				}
				for(Long id : iDs){
					DownloadRequestData drd = downloadRequestData.get(id);
					if(drd != null){
						for(DownloadedJPXData jpxData : result.getAvailableData()){
							RadioImage ri = drd.getRadioImages().get(jpxData.getImageID());
							if(ri != null){
								ResolutionSetting rs = ri.defineBestResolutionSetting(xRatio, yRatio);
								if(rs != ri.getLastUsedResolutionSetting()){
									View v = jpxData.getView();
									JHVJP2View jp2View = v.getAdapter(JHVJP2View.class);
									if(jp2View != null){
										jp2View.setViewport(new ViewportAdapter(new StaticViewport(rs.getVec2dIntRepresentation())), new ChangeEvent());
										ri.setLastUsedResolutionSetting(rs);
									}
								}else{
									fireDataNotChanged(ri.getTimeInterval(),ri.getFreqInterval(), new Rectangle(rs.getWidth(),rs.getHeight()), id, drd.getPlotIdentifier(),ri.getRadioImageID());
								}
							}
						}
					}
				}
			}			
		}
		
		
		
		/*if (currentDownloader != null){
			if(currentDownloader.isDone()){
				RadioDataDownloadHandler handler = new RadioDataDownloadHandler(
						currentJP2View, isCurrentJPX, xStart, xEnd, yStart, yEnd, xRatio, yRatio, iDs, downloadRequestData, listeners);
				handler.execute();
				currentDownloader = handler;
			}
		}else{
			RadioDataDownloadHandler handler = new RadioDataDownloadHandler(
					currentJP2View, isCurrentJPX, xStart, xEnd, yStart, yEnd, xRatio, yRatio, iDs, downloadRequestData, listeners);
			handler.execute();
			currentDownloader = handler;
		}
		
		
		/*if(isCurrentJPX){
			JHVJPXView tempCurrent = currentJP2View.getAdapter(JHVJPXView.class);
			if(!(xRatio < 0) && !(yRatio < 0)){
				fireClearAllSavedImages();
				for (Long id : iDs){
					DownloadRequestData drd = this.downloadRequestData.get(id);
					if (drd != null){
						drd.setDownloading(true);
						lineDataSelectorModel.downloadStarted(downloadRequestData.get(id));
						if(currentJP2Image != null && tempCurrent != null){
							Log.debug("Received request for data jpxview: ");
							Log.debug("x Start : "+ xStart.toString());
							Log.debug("x End : "+ xEnd.toString());
							Log.debug("y Start : "+ yStart);
							Log.debug("y End : " + yEnd);
							Log.debug("x Ratio : " + xRatio);
							Log.debug("y Ratio : " + yRatio);
							Log.debug("Search best resolution");
							ChangeEvent e = new ChangeEvent();
							e.addReason(new RadioRequestReason(tempCurrent, id));
							Interval<Date> completeInterval = new Interval<Date>(xStart,xEnd);
							FrequencyInterval completeFreqInterval = new FrequencyInterval((int)Math.round(yStart),(int)Math.round(yEnd));
							List<RadioImage> radioImages = downloadRequestData.get(id).getRadioImages();
							for (RadioImage tempIm : radioImages){
								if(tempIm.withinInterval(completeInterval,completeFreqInterval)){
									ResolutionSetting rs = tempIm.defineBestResolutionSetting(xRatio, yRatio);
									//testFrame.setResolutionSetting(rs);
									//sendDataTestFrame.setResolutionSetting(rs);
									Log.debug("Resolution level : "+ rs.getResolutionLevel());
									Log.debug("Ratio x : " + rs.getxRatio());
									Log.debug("Ratio y : " + rs.getyRatio());
									
									if (rs.equals(tempIm.getLastUsedResolutionSetting())){
										fireDataNotChanged(tempIm.getTimeInterval(),tempIm.getFreqInterval(), new Rectangle(rs.getWidth(),rs.getHeight()), id, drd.getPlotIdentifier(),tempIm.getRadioImageID());
									}else{
										tempIm.setLastUsedResolutionSetting(rs);
										currentJP2View.setViewport(new ViewportAdapter(new StaticViewport(rs.getVec2dIntRepresenation())), e);
										Log.debug("Viewport set");
										//acceptEvents = true;
										Log.debug("Will set the timestamp to " + tempIm.getFrameInJPX());
										expectEvent = true;
										waitingForFrame = tempIm.getFrameInJPX();
										tempCurrent.setCurrentFrame(tempIm.getFrameInJPX(), e, true);
										//testFrame.changeToFrame(tempIm.getFrameInJPX());
										//sendDataTestFrame.changeToFrame(tempIm.getFrameInJPX());
										Log.debug("Changed the viewport and the timestamp wait for the event :-)");
										byte[] data = new byte[0];
										while(latestComplete<tempIm.getFrameInJPX() || !eventReceived){
											Log.debug("Wait for event");
											try {
												Thread.sleep(10);
											} catch (InterruptedException ex) {
												// TODO Auto-generated catch block
												ex.printStackTrace();
											}
										}
										eventReceived = false;
										//acceptEvents = false;
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
										fireNewDataReceived(data, tempIm.getTimeInterval(),tempIm.getFreqInterval(), new Rectangle(rs.getWidth(),rs.getHeight()), id, drd.getPlotIdentifier(), tempIm.getRadioImageID());
									}
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
							Log.debug("Received request for data jp2view: ");
							Log.debug("x Start : "+ xStart.toString());
							Log.debug("x End : "+ xEnd.toString());
							Log.debug("y Start : "+ yStart);
							Log.debug("y End : " + yEnd);
							Log.debug("x Ratio : " + xRatio);
							Log.debug("y Ratio : " + yRatio);
							Log.debug("Search best resolution");
							ChangeEvent e = new ChangeEvent();
							e.addReason(new RadioRequestReason(currentJP2View, id));
							Interval<Date> completeInterval = new Interval<Date>(xStart,xEnd);
							FrequencyInterval completeFreqInterval = new FrequencyInterval((int)Math.round(yStart),(int)Math.round(yEnd));
							List<RadioImage> radioImages = downloadRequestData.get(id).getRadioImages();
							for (RadioImage tempIm : radioImages){
								
								if(tempIm.withinInterval(completeInterval,completeFreqInterval)){
									ResolutionSetting rs = tempIm.defineBestResolutionSetting(xRatio, yRatio);
									//testFrame.setResolutionSetting(rs);
									//sendDataTestFrame.setResolutionSetting(rs);
									Log.debug("Resolution level : "+ rs.getResolutionLevel());
									Log.debug("Ratio x : " + rs.getxRatio());
									Log.debug("Ratio y : " + rs.getyRatio());
								
									if (rs.equals(tempIm.getLastUsedResolutionSetting())){
										fireDataNotChanged(tempIm.getTimeInterval(),tempIm.getFreqInterval(), new Rectangle(rs.getWidth(),rs.getHeight()), id, drd.getPlotIdentifier(), tempIm.getRadioImageID());
									}else{
										tempIm.setLastUsedResolutionSetting(rs);									
										currentJP2View.setViewport(new ViewportAdapter(new StaticViewport(rs.getVec2dIntRepresenation())), e);
										Log.debug("Viewport set");
										//acceptEvents = true;
										Log.debug("Changed the viewport");
										byte[] data = new byte[0];
										//while(!eventReceived){
											Log.debug("Wait for event");
											try {
												Thread.sleep(100);
											} catch (InterruptedException ex) {
												// TODO Auto-generated catch block
												ex.printStackTrace();
											}
										//}
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
										fireNewDataReceived(data, tempIm.getTimeInterval(),tempIm.getFreqInterval(), new Rectangle(rs.getWidth(),rs.getHeight()), id, drd.getPlotIdentifier(), tempIm.getRadioImageID());
									}
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
		}*/
	}

	private void fireClearSavedImages(Long downloadID,List<Long> toRemove, String plotIdentifier) {
		Log.debug("Clear images for downloadID "+  downloadID + " and plotIdentifier "+ plotIdentifier + " nr of elements to remove: "+ toRemove.size());
		for(Long imageID : toRemove){
			for(RadioDataManagerListener l : listeners){
				l.clearAllSavedImagesForID(downloadID, imageID, plotIdentifier);
			}
		}
		
	}

	private void fireDataNotChanged(Interval<Date> timeInterval,
			FrequencyInterval freqInterval, Rectangle rectangle, Long id,
			String plotIdentifier, long imageID) {
		List<Long> tempList = new ArrayList<Long>();
		tempList.add(id);
		for(RadioDataManagerListener l : listeners){
			l.dataNotChanged(timeInterval, freqInterval, rectangle, tempList, plotIdentifier,imageID);
		}		
		
	}

	private void fireClearAllSavedImages(String plotIdentifier) {
		for(RadioDataManagerListener l : listeners){
			l.clearAllSavedImages(plotIdentifier);
		}	
	}

	private void fireNewDataReceived(byte[] data, Interval<Date> timeInterval,FrequencyInterval freqInterval, Rectangle areaSize, Long ID, String identifier, long imageID) {
		List<Long> tempList = new ArrayList<Long>();
		tempList.add(ID);
		for(RadioDataManagerListener l : listeners){
			l.newDataReceived(data, timeInterval, freqInterval, areaSize, tempList, identifier, imageID);
		}		
	}

	@Override
	public void intervalTooBig(Date requestedStartTime,
			Date requestedEndTime, long ID, String identifier) {
		DownloadRequestData drd = new DownloadRequestData(ID, identifier);
		downloadRequestData.put(ID, drd);
		lineDataSelectorModel.addLineData(drd);
		fireIntervalTooBig(ID, identifier);
		fireNewDataAvailable(drd, ID);
		fireDownloadRequestAnswered(new FrequencyInterval(0,400), new Interval<Date>(requestedStartTime,requestedEndTime),ID, identifier);
		
	}

	private void fireIntervalTooBig(long ID, String plotIdentifier) {
		for(RadioDataManagerListener l : listeners){
			l.intervalTooBig(ID, plotIdentifier);
		}
		
	}

	@Override
	public void newJPXFilesDownloaded(List<DownloadedJPXData> jpxFiles, Date requestedStartTime, Date requestedEndTime,Long downloadID, String plotIdentifier) {
		Log.debug("Init the download request data in radio data manager");
		synchronized (downloadRequestData) {		
			//Log.debug("received "+ jpxFiles.size() + " new files");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			DownloadRequestData drd = new DownloadRequestData(downloadID,plotIdentifier);
			drd.setDownloading(true);
			lineDataSelectorModel.addLineData(drd); 
			for (DownloadedJPXData djd : jpxFiles){
				//Log.debug("Handling jpx file with ID "+ djd.getImageID()); 
				//JHVJPXView jpxView = djd.getView().getAdapter(JHVJPXView.class);
				JHVJP2View jpxView = djd.getView().getAdapter(JHVJP2View.class);
				if (jpxView != null){
					//Log.debug("Setting the Reader mode");
					jpxView.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
					//jpxView.addViewListener(this);
					//jpxView.addViewListener(testFrame);
					JP2Image image = jpxView.getJP2Image();
					//currentJP2View = jpxView;
					//isCurrentJPX = true;
					//currentJP2Image = currentJP2View.getJP2Image();
					ResolutionSet rs = image.getResolutionSet();
					//Log.debug("the resolution set : " +rs.toString());
					for (int i = 0; i <= rs.getMaxResolutionLevels();i++){
						//Log.debug("resolution level " + i + " : " + rs.getResolutionLevel(i));
					}
					//Log.debug("++++++++++++++++++++++++++++++++++++++");
					Interval<Integer> interval = image.getCompositionLayerRange();
					//Log.debug("the interval is : " + interval);
					//Log.debug("the start of the interval : " + interval.getStart());
					//Log.debug("the end of the interval : "+ interval.getEnd());
					LineDataSelectorModel.getSingletonInstance().downloadStarted(drd);
					for(int i=interval.getStart(); i<= interval.getEnd(); i++){
						try{
							/*Log.debug("BITPIX : " + image.get("BITPIX",i));
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
							Log.debug("CRPIX2 : " + image.get("CRPIX2",i));*/
							SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
							FrequencyInterval fi = new FrequencyInterval(
									Integer.parseInt(image.get("STARTFRQ",i)),
									Integer.parseInt(image.get("END-FREQ",i)));
							//Log.debug("Ratios : ");
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
								for (int j = 0; j <= rs.getMaxResolutionLevels();j++){
									ResolutionSetting tempResSet = new ResolutionSetting(
											(1.0*(end.getTime()-start.getTime())/rs.getResolutionLevel(j).getResolutionBounds().width), 
											((freqEnd-freqStart)/rs.getResolutionLevel(j).getResolutionBounds().height), 
											j, 
											rs.getResolutionLevel(j).getResolutionBounds().width,
											rs.getResolutionLevel(j).getResolutionBounds().height, 
											rs.getResolutionLevel(j).getZoomLevel());
									resolutionSettings.add(tempResSet);
									/*Log.debug("resolution level " + j + " : " +rs.getResolutionLevel(j));
									Log.debug("Frequency Ratio : " + ((freqEnd-freqStart)/rs.getResolutionLevel(j).getResolutionBounds().height));
									Log.debug("Date ratio: " + (1.0*(end.getTime()-start.getTime())/rs.getResolutionLevel(j).getResolutionBounds().width));
									Log.debug("************************************");*/
								}
								int highestLevel = -1;
								ResolutionSetting lastUsedResolutionSetting = null;
								for(ResolutionSetting rst : resolutionSettings){
									if(rst.getResolutionLevel()>highestLevel){
										highestLevel = rst.getResolutionLevel();
										lastUsedResolutionSetting = rst;
									}
								}
								jpxView.setViewport(new ViewportAdapter(new StaticViewport(lastUsedResolutionSetting.getVec2dIntRepresentation())), new ChangeEvent());
								RadioImage tempRs = new RadioImage(djd, downloadID, djd.getImageID(),dateInterval,fi,i,rs,resolutionSettings,plotIdentifier, true);
								tempRs.setLastUsedResolutionSetting(lastUsedResolutionSetting);
								drd.addRadioImage(tempRs);
							}else{
								//Log.debug("Start and/or stop is null");
							}
						}catch( IOException e){
							Log.error("Some of the metadata could not be read aborting...");
							return;
						}
						//Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					}
					//Log.debug("Finished processing image with ID "+ djd.getImageID());
				}
			}
			this.downloadRequestData.put(downloadID, drd);
			Log.debug("Size of download request data" + downloadRequestData.size());
			//Log.debug("Define the max bounds");
			defineMaxBounds(downloadID);
			//Log.debug("Max bounds defined");
				
					//fireNewGlobalFrequencyInterval(maxFrequencyInterval);
			fireNewDataAvailable(drd, downloadID);
			fireDownloadRequestAnswered(maxFrequencyInterval, new Interval<Date>(requestedStartTime,requestedEndTime), downloadID, plotIdentifier);
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
	
	public void finishedDownloadingID(Long imageID, Long downloadID){
		synchronized (downloadRequestData) {			
			DownloadRequestData drd = this.downloadRequestData.get(downloadID);
			if(drd !=  null){
				//Log.debug("Set downloading false for ID "+ imageID);
				RadioImage image  = drd.getRadioImages().get(imageID);
				if(image != null){
					image.setDownloading(false);
				}else {
					//Log.debug("Image was null");
				}
				boolean isDownloading = false;
				for (RadioImage im : drd.getRadioImages().values()){
					if(im.isDownloading()){
						isDownloading = true;
						break;
					}
				}
				if (!isDownloading){
					drd.setDownloading(false);
					lineDataSelectorModel.downloadFinished(drd);
				}
			}else{
				//Log.debug("drd was null");
			}
			
		}
	}

	@Override
	public void newAdditionalDataDownloaded(List<DownloadedJPXData> jpxFiles,
			Long downloadID, String plotIdentifier, double ratioX, double ratioY) {
		//Log.debug("additional data received");
		synchronized (downloadRequestData) {		
			//Log.debug("received "+ jpxFiles.size() + " new files");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			DownloadRequestData drd = downloadRequestData.get(downloadID);
			drd.setDownloading(true);
			lineDataSelectorModel.downloadStarted(drd); 
			for (DownloadedJPXData djd : jpxFiles){
				//Log.debug("Handling jpx file with ID "+ djd.getImageID()); 
				//JHVJPXView jpxView = djd.getView().getAdapter(JHVJPXView.class);
				JHVJP2View jpxView = djd.getView().getAdapter(JHVJP2View.class);
				if (jpxView != null){
					//Log.debug("Setting the Reader mode");
					jpxView.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
					//jpxView.addViewListener(this);
					//jpxView.addViewListener(testFrame);
					JP2Image image = jpxView.getJP2Image();
					//currentJP2View = jpxView;
					//isCurrentJPX = true;
					//currentJP2Image = currentJP2View.getJP2Image();
					ResolutionSet rs = image.getResolutionSet();
					//Log.debug("the resolution set : " +rs.toString());
					for (int i = 0; i <= rs.getMaxResolutionLevels();i++){
						//Log.debug("resolution level " + i + " : " + rs.getResolutionLevel(i));
					}
					//Log.debug("++++++++++++++++++++++++++++++++++++++");
					Interval<Integer> interval = image.getCompositionLayerRange();
					/*Log.debug("the interval is : " + interval);
					Log.debug("the start of the interval : " + interval.getStart());
					Log.debug("the end of the interval : "+ interval.getEnd());*/
					LineDataSelectorModel.getSingletonInstance().downloadStarted(drd);
					for(int i=interval.getStart(); i<= interval.getEnd(); i++){
						try{
							/*Log.debug("BITPIX : " + image.get("BITPIX",i));
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
							Log.debug("CRPIX2 : " + image.get("CRPIX2",i));*/
							SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
							FrequencyInterval fi = new FrequencyInterval(
									Integer.parseInt(image.get("STARTFRQ",i)),
									Integer.parseInt(image.get("END-FREQ",i)));
							//Log.debug("Ratios : ");
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
								for (int j = 0; j <= rs.getMaxResolutionLevels();j++){
									ResolutionSetting tempResSet = new ResolutionSetting(
											(1.0*(end.getTime()-start.getTime())/rs.getResolutionLevel(j).getResolutionBounds().width), 
											((freqEnd-freqStart)/rs.getResolutionLevel(j).getResolutionBounds().height), 
											j, 
											rs.getResolutionLevel(j).getResolutionBounds().width,
											rs.getResolutionLevel(j).getResolutionBounds().height, 
											rs.getResolutionLevel(j).getZoomLevel());
									resolutionSettings.add(tempResSet);
									/*Log.debug("resolution level " + j + " : " +rs.getResolutionLevel(j));
									Log.debug("Frequency Ratio : " + ((freqEnd-freqStart)/rs.getResolutionLevel(j).getResolutionBounds().height));
									Log.debug("Date ratio: " + (1.0*(end.getTime()-start.getTime())/rs.getResolutionLevel(j).getResolutionBounds().width));
									Log.debug("************************************");*/
								}
								RadioImage tempRs = new RadioImage(djd, downloadID,djd.getImageID(),dateInterval,fi,i,rs,resolutionSettings,plotIdentifier, true);
								ResolutionSetting lastUsedResolutionSetting = tempRs.defineBestResolutionSetting(ratioX, ratioY);
								jpxView.setViewport(new ViewportAdapter(new StaticViewport(lastUsedResolutionSetting.getVec2dIntRepresentation())), new ChangeEvent());		
								tempRs.setLastUsedResolutionSetting(lastUsedResolutionSetting);
								drd.addRadioImage(tempRs);
							}else{
								//Log.debug("Start and/or stop is null");
							}
						}catch( IOException e){
							//Log.error("Some of the metadata could not be read aborting...");
							return;
						}
						//Log.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					}
					//Log.debug("Finished processing image with ID "+ djd.getImageID());
				}
			}
			this.downloadRequestData.put(downloadID, drd);
			//Log.debug("Define the max bounds");
			defineMaxBounds(downloadID);
			//Log.debug("Max bounds defined");
				
					//fireNewGlobalFrequencyInterval(maxFrequencyInterval);
			fireNewDataAvailable(drd, downloadID);
			fireAdditionalDownloadRequestAnswered(downloadID);
		}		
	}

	private void fireAdditionalDownloadRequestAnswered(Long downloadID) {
		for(RadioDataManagerListener l : listeners){
			l.additionDownloadRequestAnswered(downloadID);
		}
	}
}

	