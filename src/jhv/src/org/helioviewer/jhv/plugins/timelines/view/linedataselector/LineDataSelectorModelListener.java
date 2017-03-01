package org.helioviewer.jhv.plugins.timelines.view.linedataselector;

public interface LineDataSelectorModelListener {

    void lineDataAdded(LineDataSelectorElement element);

    void lineDataRemoved();

    void lineDataVisibility();

}
