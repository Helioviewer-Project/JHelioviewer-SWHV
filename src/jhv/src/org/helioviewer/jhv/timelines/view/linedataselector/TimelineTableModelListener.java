package org.helioviewer.jhv.timelines.view.linedataselector;

public interface TimelineTableModelListener {

    void lineDataAdded(TimelineRenderable element);

    void lineDataRemoved();

    void lineDataVisibility();

}
