package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.List;
import java.util.Date;

import org.helioviewer.base.math.Interval;

public class RadioImageCacheResult {
	private List<DownloadedJPXData> availableData;
	private List<Interval<Date>> missingIntervalt;
	private List<Long> toRemove;
	
	public RadioImageCacheResult(List<DownloadedJPXData> availableData,
			List<Interval<Date>> missingIntervalt, List<Long> toRemove) {
		super();
		this.availableData = availableData;
		this.missingIntervalt = missingIntervalt;
		this.toRemove = toRemove;
	}

	public List<DownloadedJPXData> getAvailableData() {
		return availableData;
	}
	public void setAvailableData(List<DownloadedJPXData> availableData) {
		this.availableData = availableData;
	}
	public List<Interval<Date>> getMissingInterval() {
		return missingIntervalt;
	}
	public void setMissingIntervalt(List<Interval<Date>> missingIntervalt) {
		this.missingIntervalt = missingIntervalt;
	}

	public List<Long> getToRemove() {
		return toRemove;
	}

	public void setToRemove(List<Long> toRemove) {
		this.toRemove = toRemove;
	}
}
