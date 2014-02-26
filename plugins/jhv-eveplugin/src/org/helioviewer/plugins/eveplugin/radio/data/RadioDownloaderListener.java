package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.Date;

import org.helioviewer.viewmodel.view.ImageInfoView;

public interface RadioDownloaderListener {
	public abstract void newImageViewDownloaded(ImageInfoView v, Date requestedStartTime, Date requestedEndTime, long ID, String identifier);
	
}
