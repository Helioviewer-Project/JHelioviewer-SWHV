package org.helioviewer.jhv.timelines.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.json.JSONArray;

public class BandTypeAPI {

    private static final HashMap<String, ArrayList<BandType>> groups = new HashMap<>();

    static void updateBandTypes(JSONArray jo) {
        for (int i = 0; i < jo.length(); i++) {
            BandType bandtype = new BandType(jo.getJSONObject(i));
            String group = bandtype.getGroup();
            if (!groups.containsKey(group)) {
                groups.put(group, new ArrayList<>(Collections.singletonList(bandtype)));
            } else
                groups.get(group).add(bandtype);
        }
    }

    static BandType getBandType(String name) {
        for (ArrayList<BandType> list : groups.values()) {
            for (BandType bt : list)
                if (bt.getName().equals(name))
                    return bt;
        }
        return null;
    }

    public static BandType[] getBandTypes(String group) {
        ArrayList<BandType> list = groups.get(group);
        if (list == null)
            return new BandType[0];
        return list.toArray(new BandType[list.size()]);
    }

    public static String[] getGroups() {
        return groups.keySet().toArray(new String[0]);
    }

}
