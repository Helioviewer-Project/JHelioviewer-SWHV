package org.helioviewer.plugins.eveplugin.view.linedataselector;

public interface LineDataSelectorModelListener {
    public abstract void downloadStartded(LineDataSelectorElement element);

    public abstract void downloadFinished(LineDataSelectorElement element);

    public abstract void lineDataAdded(LineDataSelectorElement element);

    public abstract void lineDataRemoved(LineDataSelectorElement element);

    public abstract void lineDataUpdated(LineDataSelectorElement element);
}
