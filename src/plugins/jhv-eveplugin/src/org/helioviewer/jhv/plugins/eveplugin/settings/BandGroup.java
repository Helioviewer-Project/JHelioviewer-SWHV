package org.helioviewer.jhv.plugins.eveplugin.settings;

import java.util.ArrayList;
import java.util.List;

public class BandGroup {
    private String groupLabel;
    public List<BandType> bandtypes = new ArrayList<BandType>();

    public void add(BandType bandtype) {
        bandtypes.add(bandtype);
    }

    public void setGroupLabel(String label) {
        this.groupLabel = label;
    }

    @Override
    public String toString() {
        return this.groupLabel;
    }
}
