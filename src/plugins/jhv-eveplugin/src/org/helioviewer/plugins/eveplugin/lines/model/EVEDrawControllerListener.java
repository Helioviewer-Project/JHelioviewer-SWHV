package org.helioviewer.plugins.eveplugin.lines.model;

import java.util.Date;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.plugins.eveplugin.lines.data.EVEValues;

/**
 * @author Stephan Pagel
 * */
public interface EVEDrawControllerListener {

    public void drawRequest(final Interval<Date> interval, final Band[] bands, final EVEValues[] values, final Range availableRange, final Range selectedRange);

    public void drawRequest(final Date movieTimestamp);
}
