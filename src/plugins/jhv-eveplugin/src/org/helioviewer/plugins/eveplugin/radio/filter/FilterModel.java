package org.helioviewer.plugins.eveplugin.radio.filter;

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.HashSet;
import java.util.Set;

import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.LUT;

public class FilterModel {
    private static FilterModel instance;

    private LUT lut;

    private IndexColorModel indexColorModel;

    private final Set<FilterModelListener> listeners;

    private FilterModel() {
        lut = LUT.getStandardList().get("Rainbow 2");
        indexColorModel = createIndexColorModelFromLUT(lut);
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
        lut = newLUT;
        indexColorModel = createIndexColorModelFromLUT(lut);
        fireLUTChanged();
    }

    private void fireLUTChanged() {
        for (FilterModelListener l : listeners) {
            l.colorLUTChanged();
        }
    }

    public IndexColorModel getColorModel() {
        return indexColorModel;
    }

    private IndexColorModel createIndexColorModelFromLUT(LUT lut2) {
        return new IndexColorModel(8, lut2.getLut8().length, lut2.getLut8(), 0, false, -1, DataBuffer.TYPE_BYTE);
    }
}
