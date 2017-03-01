package org.helioviewer.jhv.plugins.eveplugin.view;

import javax.swing.JComponent;

import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;

public interface TimelineContentPanel extends LineDataSelectorModelListener {
    public abstract boolean loadButtonPressed();

    public abstract JComponent getTimelineContentPanel();

    public abstract void setupDatasets();
}
