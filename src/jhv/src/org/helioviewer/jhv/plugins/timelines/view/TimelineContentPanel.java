package org.helioviewer.jhv.plugins.timelines.view;

import javax.swing.JComponent;

import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorModelListener;

public interface TimelineContentPanel extends LineDataSelectorModelListener {
    public abstract boolean loadButtonPressed();

    public abstract JComponent getTimelineContentPanel();

    public abstract void setupDatasets();
}
