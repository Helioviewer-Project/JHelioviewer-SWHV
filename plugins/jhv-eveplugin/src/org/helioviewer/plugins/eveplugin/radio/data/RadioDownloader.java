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
	
	private RadioDownloader(){
		this.listeners = new ArrayList<RadioDownloaderListener>();
	}
	
	public static RadioDownloader getSingletonInstance(){
		if (instance == null){
			instance = new RadioDownloader();			
		}
		return instance;
	}
	
	public void requestAndOpenRemoteFile(String startTime, String endTime, String identifier){
		Thread thread = new Thread(new Runnable() {
			private String startTime;
			private String endTime;
			private String identifier;
			
			public Runnable init(String startTime, String endTime, String identifier){
				this.startTime = startTime;
				this.endTime = endTime;
				this.identifier = identifier;
				Log.debug("Starttime : "+ startTime);
				Log.debug("Endtime : "+ endTime);
				return this;				
			}
			
            public void run() {

                try {
                    ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false, null, startTime, endTime, "ROB-Humain", "CALLISTO","CALLISTO", "RADIOGRAM");
                    //dataManager.addNewView(v);
                    fireNewImageViewDownloaded(v, startTime, endTime, Math.round(1000000*Math.random()), identifier);
                } catch (IOException e) {
                    Log.error("An error occured while opening the remote file!", e);
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                } 
            }
            
            
        }.init(startTime, endTime, identifier), "LoadNewImage");

        thread.start();
	}
	
	public void addRadioDownloaderListener(RadioDownloaderListener l){
		listeners.add(l);
	}
	
	public void removeDownloaderListener(RadioDownloaderListener l){
		listeners.remove(l);
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
	
	
}
