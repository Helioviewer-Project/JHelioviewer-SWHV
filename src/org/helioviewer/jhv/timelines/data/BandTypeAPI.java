package org.helioviewer.jhv.timelines.data;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;

public class BandTypeAPI {

    private static final HashMap<String, ArrayList<BandType>> groups = new HashMap<>();

    static void updateBandTypes(JSONArray jo) {
        BandType[] bandtypes = new BandType[jo.length()];
        for (int i = 0; i < jo.length(); i++) {
            bandtypes[i] = new BandType(jo.getJSONObject(i));
        }

        for (BandType bandtype : bandtypes) {
            String group = bandtype.getGroup();
            if (!groups.containsKey(group)) {
                groups.put(group, new ArrayList<>(Arrays.asList(bandtypes)));
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
