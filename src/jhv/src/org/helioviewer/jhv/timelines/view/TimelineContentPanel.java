package org.helioviewer.jhv.timelines.view;

import javax.swing.JComponent;

import org.helioviewer.jhv.timelines.view.linedataselector.TimelineTableModelListener;

public interface TimelineContentPanel extends TimelineTableModelListener {

    void loadButtonPressed();

    JComponent getTimelineContentPanel();

    void setupDatasets();

}
