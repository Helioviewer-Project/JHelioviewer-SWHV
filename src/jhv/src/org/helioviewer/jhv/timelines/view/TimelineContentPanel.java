package org.helioviewer.jhv.timelines.view;

import javax.swing.JComponent;

import org.helioviewer.jhv.timelines.view.linedataselector.TimelineTableModelListener;

public interface TimelineContentPanel extends TimelineTableModelListener {
    public abstract void loadButtonPressed();

    public abstract JComponent getTimelineContentPanel();

    public abstract void setupDatasets();
}
