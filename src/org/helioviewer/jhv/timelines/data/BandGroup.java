package org.helioviewer.jhv.timelines.data;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class BandGroup {

    private String groupLabel;
    public final List<BandType> bandtypes = new ArrayList<>();

    public void serialize(JSONObject jo) {
        JSONObject bandGroup = new JSONObject();
        jo.put("bandGroup", bandGroup);
        bandGroup.put("label", groupLabel);
    }

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
