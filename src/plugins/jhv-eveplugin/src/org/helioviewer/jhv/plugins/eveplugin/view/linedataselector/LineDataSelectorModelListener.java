package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

public interface LineDataSelectorModelListener {

    void lineDataAdded(LineDataSelectorElement element);

    void lineDataRemoved(LineDataSelectorElement element);

    void lineDataVisibility(LineDataSelectorElement element, boolean flag);

}
