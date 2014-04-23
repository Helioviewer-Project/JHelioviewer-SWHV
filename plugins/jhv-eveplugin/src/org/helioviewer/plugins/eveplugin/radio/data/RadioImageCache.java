package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;

public class RadioImageCache {
	private Map<String, RadioImageCacheData> radioImageCacheData;
	
	private List<RadioImageCacheListener> listeners;  
	
	private static RadioImageCache instance;
	
	private RadioImageCache(){
		this.listeners = new ArrayList<RadioImageCacheListener>();
		radioImageCacheData = new HashMap<String, RadioImageCacheData>();		
	}
	
	public void addRadioImageCacheListener(RadioImageCacheListener listener){
		this.listeners.add(listener);
	}
	
	public void removeRadioImageListener(RadioImageCacheListener listener){
		this.listeners.remove(listener);
	}
	
	public static RadioImageCache getInstance(){
		if(instance==null){
			instance = new RadioImageCache();
		}
		return instance;		
	}
	
	public void add(DownloadedJPXData jpxData){
		Log.debug("Try to add data in cache");
		synchronized (instance) {
			Log.debug("Could add data to cache");
			Log.debug("ImageID : " + jpxData.getImageID());
			Log.debug("plot identifier : "+ jpxData.getPlotIdentifier());
			Log.debug("start time : " + jpxData.getStartDate());
			RadioImageCacheData data = new RadioImageCacheData();
			if(radioImageCacheData.containsKey(jpxData.getPlotIdentifier())){
				data = radioImageCacheData.get(jpxData.getPlotIdentifier());
			}else{
				radioImageCacheData.put(jpxData.getPlotIdentifier(), data);
			}
			data.getDataCache().put(jpxData.getImageID(), jpxData);
			data.getUseCache().put(jpxData.getImageID(), 0L);
			data.getStartDates().put(jpxData.getStartDate(), jpxData);
		}
	}
	
	public void remove(DownloadedJPXData jpxData) {
		remove(jpxData.getImageID(), jpxData.getPlotIdentifier());
	}
	
	public void remove(Long ID, String plotIdentifier){		
		synchronized (instance) {
			if(radioImageCacheData.containsKey(plotIdentifier)){
				RadioImageCacheData cacheData = radioImageCacheData.get(plotIdentifier);
			
				DownloadedJPXData data = cacheData.getDataCache().get(ID);
				cacheData.getDataCache().remove(ID);
				cacheData.getUseCache().remove(ID);
				cacheData.getStartDates().remove(data.getStartDate());
			}
		}
	}
		
	private Date findStartDate(Date start, Long stepsize){
		long divider = 1L;
		if(stepsize < 1000L){
			divider = 1000L;
		}else if (stepsize < 60L * 1000){
			divider = 60L * 1000;
		}else if (stepsize < 60L * 60 * 1000){
			divider = 60L * 60 * 1000;
		}else {
			divider = 24L * 60 * 60 * 1000;
		}
		return new Date(start.getTime() - start.getTime() % divider);
	}
	
	public RadioImageCacheResult getRadioImageCacheResultForInterval(Date start, Date end, Long stepsize, String plotIdentifier){
		if(radioImageCacheData.containsKey(plotIdentifier)){
			RadioImageCacheData cacheData = radioImageCacheData.get(plotIdentifier);
			Date localStart = findStartDate(start, stepsize);
			List<Interval<Date>> intervalList = new ArrayList<Interval<Date>>();
			List<DownloadedJPXData> dataList = new ArrayList<DownloadedJPXData>();
			List<Long> toRemove = new ArrayList<Long>(cacheData.getDataCache().keySet());
			/*Log.debug("IDs in cache: ");
			for (Long temp : dataCache.keySet()){
				Log.debug(temp);
			}
			Log.debug("Values to remove before");
			for (Long temp : toRemove){
				Log.debug(temp);
			}*/
			while (localStart.before(end) || localStart.equals(end)){
				//Log.debug("Execute loop");
				if (!cacheData.getStartDates().containsKey(localStart)){
					intervalList.add(new Interval<Date>(localStart, new Date(localStart.getTime()+stepsize)));
				}else {
					dataList.add(cacheData.getStartDates().get(localStart));
					toRemove.remove(cacheData.getStartDates().get(localStart).getImageID());
				}
				localStart = new Date(localStart.getTime()+stepsize);
			}
			/*Log.debug("Values to remove after");
			for (Long temp : toRemove){
				Log.debug(temp);
			}
			Log.debug("IDs in cache: ");
			for (Long temp : dataCache.keySet()){
				Log.debug(temp);
			}*/
			return new RadioImageCacheResult(dataList, intervalList, new ArrayList<Long>(toRemove));
		}else{
			Date localStart = findStartDate(start, stepsize);
			List<Interval<Date>> intervalList = new ArrayList<Interval<Date>>();
			List<DownloadedJPXData> dataList = new ArrayList<DownloadedJPXData>();
			List<Long> toRemove = new ArrayList<Long>();
			while (localStart.before(end) || localStart.equals(end)){
				//Log.debug("Execute loop");
				intervalList.add(new Interval<Date>(localStart, new Date(localStart.getTime()+stepsize)));
				localStart = new Date(localStart.getTime()+stepsize);
			}
			return new RadioImageCacheResult(dataList, intervalList, new ArrayList<Long>(toRemove));
		}
	}

	public boolean containsDate(Date date, String plotIdentifier) {
		if (radioImageCacheData.containsKey(plotIdentifier)){
			RadioImageCacheData cacheData = radioImageCacheData.get(plotIdentifier);
			return cacheData.getStartDates().containsKey(date);
		}else {
			return false;
		}
	}
}
