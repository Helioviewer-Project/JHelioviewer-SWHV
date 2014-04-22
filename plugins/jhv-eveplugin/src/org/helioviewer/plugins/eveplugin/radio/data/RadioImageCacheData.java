package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RadioImageCacheData {

	private Map<Long,DownloadedJPXData> dataCache;
	private Map<Long,Long> useCache;
	private Map<Date, DownloadedJPXData> startDates;
	
	public RadioImageCacheData() {
		this.dataCache = new HashMap<Long, DownloadedJPXData>();
		this.useCache = new HashMap<Long,Long>();
		this.startDates = new HashMap<Date, DownloadedJPXData>();
	}

	public Map<Long, DownloadedJPXData> getDataCache() {
		return dataCache;
	}

	public void setDataCache(Map<Long, DownloadedJPXData> dataCache) {
		this.dataCache = dataCache;
	}

	public Map<Long, Long> getUseCache() {
		return useCache;
	}

	public void setUseCache(Map<Long, Long> useCache) {
		this.useCache = useCache;
	}

	public Map<Date, DownloadedJPXData> getStartDates() {
		return startDates;
	}

	public void setStartDates(Map<Date, DownloadedJPXData> startDates) {
		this.startDates = startDates;
	}

	
	
}
