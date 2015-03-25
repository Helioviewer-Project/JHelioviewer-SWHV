package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.util.ArrayList;
import java.util.List;

public class LineDataSelectorModel {
    private final List<LineDataSelectorModelListener> listeners;
    private final List<LineDataSelectorElement> elements;

    private static LineDataSelectorModel instance;

    private LineDataSelectorModel() {
        listeners = new ArrayList<LineDataSelectorModelListener>();
        elements = new ArrayList<LineDataSelectorElement>();
    }

    public static LineDataSelectorModel getSingletonInstance() {
        if (instance == null) {
            instance = new LineDataSelectorModel();
        }
        return instance;
    }

    public void addLineDataSelectorModelListener(LineDataSelectorModelListener listener) {
        listeners.add(listener);
    }

    public void removeLineDataSelectorModelListener(LineDataSelectorModelListener listener) {
        listeners.remove(listener);
    }

    public void downloadStarted(LineDataSelectorElement element) {
        fireDownloadStarted(element);
    }

    public void downloadFinished(LineDataSelectorElement element) {
        fireDownloadFinished(element);
    }

    public int getNumberOfAvailableLineData() {
        return elements == null ? 0 : elements.size();
    }

    public void addLineData(LineDataSelectorElement element) {
        elements.add(element);
        fireLineDataSelectorElementAdded(element);
    }

    public List<LineDataSelectorElement> getAllLineDataSelectorElements() {
        return elements;
    }

    public void removeLineData(LineDataSelectorElement element) {
        if (elements != null) {
            elements.remove(element);
        }
        fireLineDataSelectorElementRemoved(element);
    }

    public void lineDataElementUpdated(LineDataSelectorElement element) {
        fireLineDataSelectorElementUpdated(element);
    }

    public void lineDataGroupChanged() {

    }

    public boolean atLeastOneDownloading() {
        for (LineDataSelectorElement el : elements) {
            if (el.isDownloading()) {
                return true;
            }
        }
        return false;
    }

    private void fireLineDataSelectorElementRemoved(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataRemoved(element);
        }

    }

    private void fireLineDataSelectorElementAdded(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataAdded(element);
        }
    }

    private void fireDownloadStarted(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.downloadStartded(element);
        }
    }

    private void fireDownloadFinished(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.downloadFinished(element);
        }
    }

    private void fireLineDataSelectorElementUpdated(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataUpdated(element);
        }
    }
}
