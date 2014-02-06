package org.helioviewer.filter.softwaremode;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.filter.StandardFilter;
import org.helioviewer.viewmodel.imagedata.ImageData;

/**
 * Filter in software mode to switch the view chain in software mode.
 * <p>
 * Also allow some logging about the filtering
 * 
 * @author Helge Dietert
 * 
 */
public class SoftwareModeFilter implements StandardFilter {
    /**
     * Flag to indicate whether this filter should log
     */
    private boolean isLogging = true;

    public ImageData apply(ImageData data) {
        if (isLogging) {
            Log.info("SoftwareModeFilter:: Pass through data " + data);
        }
        return data;
    }

    /**
     * @see org.helioviewer.viewmodel.filter.Filter#forceRefilter()
     */
    public void forceRefilter() {
        // Never do anything
    }

    /**
     * @return the isLogging
     */
    public boolean isLogging() {
        return isLogging;
    }

    /**
     * @see org.helioviewer.viewmodel.filter.Filter#isMajorFilter()
     */
    public boolean isMajorFilter() {
        return true;
    }

    /**
     * @param isLogging
     *            the isLogging to set
     */
    public void setLogging(boolean isLogging) {
        this.isLogging = isLogging;
    }

	@Override
	public void setState(String state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getState() {
		// TODO Auto-generated method stub
		return null;
	}
}
