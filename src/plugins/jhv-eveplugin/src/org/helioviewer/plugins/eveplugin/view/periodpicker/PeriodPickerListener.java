package org.helioviewer.plugins.eveplugin.view.periodpicker;

import java.util.Date;

import org.helioviewer.base.interval.Interval;

/**
 * @author Stephan Pagel
 * */
public interface PeriodPickerListener {

    public void intervalChanged(final Interval<Date> interval);
}
