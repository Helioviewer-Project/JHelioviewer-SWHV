package org.helioviewer.jhv.timelines.data;

import java.util.ArrayList;
import java.util.HashMap;

import org.helioviewer.jhv.log.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class BandTypeAPI {

    private static final HashMap<String, BandGroup> groups = new HashMap<>();
    private static final ArrayList<BandGroup> orderedGroups = new ArrayList<>();

    static void updateBandTypes(JSONArray jo) {
        BandType[] bandtypes = new BandType[jo.length()];
        for (int i = 0; i < jo.length(); i++) {
            bandtypes[i] = new BandType();

            JSONObject job = jo.getJSONObject(i);
            if (job.has("label")) {
                bandtypes[i].setLabel(job.getString("label"));
            }
            if (job.has("name")) {
                bandtypes[i].setName(job.getString("name"));
            }
            if (job.has("range")) {
                JSONArray rangeArray = job.getJSONArray("range");
                bandtypes[i].setMin(rangeArray.getDouble(0));
                bandtypes[i].setMax(rangeArray.getDouble(1));
            }
            if (job.has("unitLabel")) {
                bandtypes[i].setUnitLabel(job.getString("unitLabel"));
            }
            if (job.has("baseUrl")) {
                bandtypes[i].setBaseURL(job.getString("baseUrl"));
            }
            if (job.has("scale")) {
                bandtypes[i].setScale(job.getString("scale"));
            }
            if (job.has("warnLevels")) {
                JSONArray warnLevels = job.getJSONArray("warnLevels");
                HashMap<String, Double> store = bandtypes[i].getWarnLevels();
                for (int j = 0; j < warnLevels.length(); j++) {
                    JSONObject helpobj = warnLevels.getJSONObject(j);
                    store.put(helpobj.getString("warnLabel"), helpobj.getDouble("warnValue"));
                }
            }
            if (job.has("group")) {
                BandGroup group = groups.get(job.getString("group"));
                group.add(bandtypes[i]);
            }
        }
    }

    static void updateBandGroups(JSONArray jo) {
        for (int i = 0; i < jo.length(); i++) {
            BandGroup group = new BandGroup();
            JSONObject job = jo.getJSONObject(i);
            if (job.has("groupLabel")) {
                group.setGroupLabel(job.getString("groupLabel"));
            }
            if (job.has("key")) {
                groups.put(job.getString("key"), group);
                orderedGroups.add(group);
            }
        }
    }

    static BandType getBandType(String name) {
        for (BandGroup bg : orderedGroups) {
            for (BandType bt : bg.bandtypes) {
                if (bt.getName().equals(name))
                    return bt;
            }
        }
        return null;
    }

    static BandGroup getGroup(BandType t) {
        for (BandGroup bg : orderedGroups) {
            for (BandType bt : bg.bandtypes) {
                if (bt.equals(t))
                    return bg;
            }
        }
        return null;
    }

    public static BandType[] getBandTypes(BandGroup group) {
        return group.bandtypes.toArray(new BandType[group.bandtypes.size()]);
    }

    public static BandGroup[] getGroups() {
        return orderedGroups.toArray(new BandGroup[orderedGroups.size()]);
    }

}
