package org.helioviewer.plugins.eveplugin.radio.filter;

import java.util.HashSet;
import java.util.Set;

import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.LUT;
import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.SOHOLUTFilter;
import org.helioviewer.viewmodel.imagedata.ImageData;

public class FilterModel {
    private static FilterModel instance;

    private SOHOLUTFilter lutFilter;

    private final Set<FilterModelListener> listeners;

    private FilterModel() {
        lutFilter = new SOHOLUTFilter(LUT.getStandardList().get("Rainbow 2"));
        listeners = new HashSet<FilterModelListener>();
    }

    public static FilterModel getInstance() {
        if (instance == null) {
            instance = new FilterModel();
        }
        return instance;
    }

    public void addFilterModelListener(FilterModelListener listener) {
        listeners.add(listener);
    }

    public void removeFilterModelListener(FilterModelListener listener) {
        listeners.remove(listener);
    }

    public void setLUT(LUT newLUT) {
        lutFilter = new SOHOLUTFilter(newLUT);
        fireLUTChanged();
    }

    public ImageData colorFilter(ImageData data) {
        return lutFilter.apply(data);
    }

    private void fireLUTChanged() {
        for (FilterModelListener l : listeners) {
            l.colorLUTChanged();
        }
    }

}
