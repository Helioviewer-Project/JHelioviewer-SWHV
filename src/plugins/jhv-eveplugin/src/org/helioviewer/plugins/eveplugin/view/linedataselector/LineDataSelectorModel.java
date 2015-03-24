package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineDataSelectorModel {
    private final List<LineDataSelectorModelListener> listeners;
    private final List<LineDataSelectorElement> elements;

    private static LineDataSelectorModel instance;

    private LineDataSelectorModel() {
        listeners = Collections.synchronizedList(new ArrayList<LineDataSelectorModelListener>());
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
        synchronized (elements) {
            return elements == null ? 0 : elements.size();
        }
    }

    public synchronized void addLineData(LineDataSelectorElement element) {
        synchronized (elements) {
            elements.add(element);
            fireLineDataSelectorElementAdded(element);
        }
    }

    public List<LineDataSelectorElement> getAllLineDataSelectorElements() {
        synchronized (elements) {
            return elements;
        }
    }

    public void removeLineData(LineDataSelectorElement element) {
        synchronized (elements) {
            if (elements != null) {
                elements.remove(element);
            }
            fireLineDataSelectorElementRemoved(element);
        }
    }

    public void lineDataElementUpdated(LineDataSelectorElement element) {
        synchronized (elements) {
            fireLineDataSelectorElementUpdated(element);
        }
    }

    public void lineDataGroupChanged() {

    }

    public boolean atLeastOneDownloading() {
        synchronized (elements) {
            for (LineDataSelectorElement el : elements) {
                if (el.isDownloading()) {
                    return true;
                }
            }
            return false;
        }
    }

    private synchronized void fireLineDataSelectorElementRemoved(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataRemoved(element);
        }

    }

    private synchronized void fireLineDataSelectorElementAdded(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataAdded(element);
        }
    }

    private synchronized void fireDownloadStarted(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.downloadStartded(element);
        }
    }

    private synchronized void fireDownloadFinished(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.downloadFinished(element);
        }
    }

    private synchronized void fireLineDataSelectorElementUpdated(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataUpdated(element);
        }
    }
}
