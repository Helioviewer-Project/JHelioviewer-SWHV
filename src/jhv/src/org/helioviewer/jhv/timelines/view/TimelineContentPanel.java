package org.helioviewer.jhv.timelines.view;

import javax.swing.JComponent;

public interface TimelineContentPanel {

    void loadButtonPressed();

    JComponent getTimelineContentPanel();

    void setupDatasets();

    void updateGroupValues();

}
