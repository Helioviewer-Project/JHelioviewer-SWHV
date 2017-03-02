package org.helioviewer.jhv.timelines.data;

import java.util.ArrayList;
import java.util.List;

public class BandGroup {

    private String groupLabel;
    public final List<BandType> bandtypes = new ArrayList<>();

    public void add(BandType bandtype) {
        bandtypes.add(bandtype);
    }

    public void setGroupLabel(String label) {
        groupLabel = label;
    }

    @Override
    public String toString() {
        return groupLabel;
    }

}
