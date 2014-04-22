package org.helioviewer.plugins.eveplugin.radio.data;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.message.Message;
import org.helioviewer.plugins.eveplugin.download.DataDownloader;
import org.helioviewer.plugins.eveplugin.download.DownloadedData;
import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.viewmodel.view.ImageInfoView;

public class RadioDownloader implements DataDownloader{
	//Make connection with server to request the jpx
	//Make structure indicating the start and stop for every jpx in the file
	//give the data for a certain zoomlevel and timerange
	private static RadioDownloader instance;
	private List<RadioDownloaderListener> listeners;
	private RadioImageCache cache;
	
	private final long MAXIMUM_DAYS = 172800000;
	
	private RadioDownloader(){
		this.listeners = new ArrayList<RadioDownloaderListener>();
		this.cache = RadioImageCache.getInstance();
	}
	
	public static RadioDownloader getSingletonInstance(){
		if (instance == null){
			instance = new RadioDownloader();			
		}
		return instance;
	}
	
	public void requestAndOpenRemoteFile(String startDateString, String endDateString, String identifier){
		Thread thread = new Thread(new Runnable() {
			private String startDataString;
			private String endDateString;
			private String identifier;
			
			public Runnable init(String startTime, String endTime, String identifier){
				this.startDataString = startTime;
				this.endDateString = endTime;
				this.identifier = identifier;
				//Log.debug("Starttime : "+ startTime);
				//Log.debug("Endtime : "+ endTime);
				return this;				
			}
			
            public void run() {

                try {
                	Log.debug("Request for date "+ startDataString + " - " + endDateString);
                	long duration = calculateFrequencyDuration(startDataString, endDateString);
                	long downloadID = Math.round(1000000*Math.random());
                	Log.debug("DownloadID: "+ downloadID);
                	Log.debug("plotidentifier: "+ identifier);
                	Date startDate = parseDate(startDataString);
                	Date endDate = parseDate(endDateString);
                	if(duration >= 0 && duration<= MAXIMUM_DAYS){                		
                		Date requestedStartDate = new Date(startDate.getTime());                		
                		if(endDate != null && startDate != null){
                			//GregorianCalendar startGreg = new GregorianCalendar();
                			//startGreg.setTime(startDate);
                			endDate.setTime(endDate.getTime());
                			//case there were not more than three days
                			List<DownloadedJPXData> jpxList = new ArrayList<DownloadedJPXData>();
                			while(startDate.before(endDate)||startDate.equals(endDate)){
                				//String startDatePlusOne = calculateOneDayFurtherAsString(startDate);
                				ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false, null, createDateString(startDate), createDateString(startDate), "ROB-Humain", "CALLISTO","CALLISTO", "RADIOGRAM");
                		//dataManager.addNewView(v);
                				if(v != null){
                					Long imageID = Math.round(1000000*Math.random());
                					//Log.debug("bbbbbbbbbbbbbb Image ID "+ imageID + " bbbbbbbbbbbbbbbbbbbbbbbbbbbb");
                					DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID, startDate, endDate, identifier,downloadID);
                					jpxList.add(newJPXData);
                					cache.add(newJPXData);
                				}else{
                					Log.error("Received null view for date "+ startDate+ " and " + endDate);
                				}
                				startDate = calculateOneDayFurtherAsDate(startDate);
                			}
                			fireNewJPXDataAvailable(jpxList, identifier,requestedStartDate, endDate,downloadID);
                		}else{
                			
                		}
                	}else{
                		fireIntervalTooBig(startDate,endDate,downloadID,identifier);
                		//ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false, null, startTime, endTime, "ROB-Humain", "CALLISTO","CALLISTO", "RADIOGRAM");
                		
                	}
                } catch (IOException e) {
                    Log.error("An error occured while opening the remote file!", e);
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                } 
            }

			private void fireIntervalTooBig(Date startDate, Date endDate,
					long downloadID, String plotIdentifier) {
				for(RadioDownloaderListener l: listeners){
					l.intervalTooBig(startDate, endDate, downloadID, identifier);
				}
				
			}           
        }.init(startDateString, endDateString, identifier), "LoadNewImage");

        thread.start();
	}
	
	public void requestAndOpenIntervals(List<Interval<Date>> intervals, Long downloadId, String plotIdentifier, double ratioX, double ratioY){
		for (Interval<Date> interval : intervals){
			Log.debug("Request for data for interval "+ interval.getStart() + " - " + interval.getEnd());
			Thread thread = new Thread(new Runnable() {
				private String startDataString;
				private String endDateString;
				private String identifier;
				private Long downloadID;
				private double ratioX;
				private double ratioY;
				
				public Runnable init(String startTime, String endTime, String identifier, Long downloadID, double ratioX, double ratioY){
					this.startDataString = startTime;
					this.endDateString = endTime;
					this.identifier = identifier;
					this.downloadID = downloadID;
					this.ratioX = ratioX;
					this.ratioY = ratioY;
					//Log.debug("Starttime : "+ startTime);
					//Log.debug("Endtime : "+ endTime);
					return this;				
				}
				
	            public void run() {	
	                try {
	                	
	                	Date startDate = parseDate(startDataString);
	                	Date requestedStartDate = new Date(startDate.getTime());
	                	Date endDate = parseDate(endDateString);
                		if(endDate != null && startDate != null){
                			//GregorianCalendar startGreg = new GregorianCalendar();
                			//startGreg.setTime(startDate);
                			endDate.setTime(endDate.getTime());
                			//case there were not more than three days
                			List<DownloadedJPXData> jpxList = new ArrayList<DownloadedJPXData>();
                			while(startDate.before(endDate)||startDate.equals(endDate)){
                				//String startDatePlusOne = calculateOneDayFurtherAsString(startDate);
                				ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false, null, createDateString(startDate), createDateString(startDate), "ROB-Humain", "CALLISTO","CALLISTO", "RADIOGRAM");
                		//dataManager.addNewView(v);
                				if(v != null){
                					Long imageID = Math.round(1000000*Math.random());
                					//Log.debug("cccccccccccc Image ID "+ imageID + " cccccccccccccccccccccccccccccc");
                					DownloadedJPXData newJPXData = new DownloadedJPXData(v,imageID , startDate, endDate, identifier,downloadID);
                					jpxList.add(newJPXData);
                					cache.add(newJPXData);
                				}else {
                					//Log.error("Received null view for date "+ startDate+ " and " + endDate);
                				}
                				startDate = calculateOneDayFurtherAsDate(startDate);
                			}
                			fireAdditionalJPXDataAvailable(jpxList, identifier,requestedStartDate, endDate,downloadID, ratioX, ratioY);
                		}else{
                			
                		}	                	
	                } catch (IOException e) {
	                    Log.error("An error occured while opening the remote file!", e);
	                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
	                } 
	            }
				          
	        }.init(createDateString(interval.getStart()), createDateString(interval.getEnd()), plotIdentifier, downloadId, ratioX, ratioY), "LoadNewImage");
	
	        thread.start();
		}
	}
	
	private void fireAdditionalJPXDataAvailable(
			List<DownloadedJPXData> jpxList, String plotIdentifier,
			Date startDate, Date endDate, Long downloadID, double ratioX, double ratioY) {
		for(RadioDownloaderListener l : listeners){
			l.newAdditionalDataDownloaded(jpxList, downloadID, plotIdentifier, ratioX, ratioY);
		}
	} 
	
	private void fireNewJPXDataAvailable(List<DownloadedJPXData> jpxList, String plotIdentifier, Date startDate, Date endDate,Long downloadID) {
		for(RadioDownloaderListener l : listeners){
			l.newJPXFilesDownloaded(jpxList, startDate,endDate,downloadID, plotIdentifier);
		}
	}
	
	public void addRadioDownloaderListener(RadioDownloaderListener l){
		listeners.add(l);
	}
	
	public void removeDownloaderListener(RadioDownloaderListener l){
		listeners.remove(l);
	}

	private long calculateFrequencyDuration(String startTime,String endTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date start;
		try {
			start = sdf.parse(startTime);
			Date end = sdf.parse(endTime);
			return end.getTime()-start.getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
	}
	
	private String calculateOneDayFurtherAsString(String date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		try {
			return sdf.format(new Date(sdf.parse(date).getTime()+86400000));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private Date calculateOneDayFurtherAsDate(String date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		try {
			return new Date(sdf.parse(date).getTime()+86400000);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private String calculateOneDayFurtherAsString(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		return sdf.format(new Date(date.getTime()+86400000));
	}
	
	private Date calculateOneDayFurtherAsDate(Date date){
		return new Date(date.getTime()+86400000);
	}
	
	/*private void fireNewDownloadRequested(String startTime, String endTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		for (RadioDownloaderListener l : listeners){
			try {
				l.newDownloadRequested(sdf.parse(startTime), sdf.parse(endTime));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.error("Could not parse the following string: "+ startTime + " or " + endTime);
			}
		}		
	}*/

	private void fireNewImageViewDownloaded(ImageInfoView v, String requestedStartTime, String requestedEndTime, long ID, String identifier) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		for (RadioDownloaderListener l : listeners){
			try {
				l.newImageViewDownloaded(v, sdf.parse(requestedStartTime), sdf.parse(requestedEndTime),ID, identifier);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.error("Could not parse the following string: "+ requestedStartTime + " or " + requestedEndTime);
			}
		}		
	}

	@Override
	public DownloadedData downloadData(Band band, Interval<Date> interval) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Date parseDate(String date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			return sdf.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private String createDateString(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(date);
	}
	
}
