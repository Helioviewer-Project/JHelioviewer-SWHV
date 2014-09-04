package org.helioviewer.plugins.eveplugin.settings;

import java.util.List;
import java.util.ArrayList;

public class BandGroup {
    private String groupLabel;
    private String key;
    public List<BandType> bandtypes = new ArrayList<BandType>();

    public void add(BandType bandtype) {
        bandtypes.add(bandtype);
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String label) {
        this.groupLabel = label;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String toString() {
        return this.groupLabel;
    }
}
