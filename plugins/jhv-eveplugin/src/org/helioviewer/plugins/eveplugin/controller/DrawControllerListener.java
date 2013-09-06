package org.helioviewer.plugins.eveplugin.controller;

import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;

/**
 * @author Stephan Pagel
 * */
public interface DrawControllerListener {

    public void drawRequest(final Interval<Date> interval, final Band[] bands, final EVEValues[] values, final Range availableRange, final Range selectedRange);
    public void drawRequest(final Date movieTimestamp);
}
