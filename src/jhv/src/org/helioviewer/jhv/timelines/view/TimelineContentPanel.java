package org.helioviewer.jhv.timelines.view;

import javax.swing.JComponent;

import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorModelListener;

public interface TimelineContentPanel extends LineDataSelectorModelListener {
    public abstract void loadButtonPressed();

    public abstract JComponent getTimelineContentPanel();

    public abstract void setupDatasets();
}
