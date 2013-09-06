package org.helioviewer.plugins.eveplugin.controller;

import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;

/**
 * 
 * @author Stephan Pagel
 * */
public interface ZoomControllerListener {

    public void availableIntervalChanged(final Interval<Date> newInterval);
    
    public void selectedIntervalChanged(final Interval<Date> newInterval);
    
    public void selectedResolutionChanged(final API_RESOLUTION_AVERAGES newResolution);
}
