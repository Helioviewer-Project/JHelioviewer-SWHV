package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.EVESettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BandTypeAPI {

    private static final HashMap<String, BandGroup> groups = new HashMap<>();
    private static final ArrayList<BandGroup> orderedGroups = new ArrayList<>();

    public static void getDatasets() {
        try {
            JSONObject jsonmain = JSONUtils.getJSONStream(new DownloadStream(new URL(EVESettings.baseURL)).getInput());
            updateBandGroups(jsonmain.getJSONArray("groups"));
            updateBandTypes(jsonmain.getJSONArray("objects"));
        } catch (IOException e) {
            Log.error("Error downloading the bandtypes", e);
        } catch (JSONException e) {
            Log.error("JSON parsing error", e);
        }
    }

    private static void updateBandTypes(JSONArray jsonObjectArray) {
        BandType[] bandtypes = new BandType[jsonObjectArray.length()];
        try {
            for (int i = 0; i < jsonObjectArray.length(); i++) {
                bandtypes[i] = new BandType();

                JSONObject job = jsonObjectArray.getJSONObject(i);
                if (job.has("label")) {
                    bandtypes[i].setLabel(job.getString("label"));
                }
                if (job.has("name")) {
                    bandtypes[i].setName(job.getString("name"));
                }
                if (job.has("range")) {
                    JSONArray rangeArray = job.getJSONArray("range");
                    Double v0 = rangeArray.getDouble(0);
                    Double v1 = rangeArray.getDouble(1);
                    bandtypes[i].setMin(v0);
                    bandtypes[i].setMax(v1);
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
                    for (int j = 0; j < warnLevels.length(); j++) {
                        JSONObject helpobj = warnLevels.getJSONObject(j);
                        bandtypes[i].warnLevels.put(helpobj.getString("warnLabel"), helpobj.getDouble("warnValue"));
                    }
                }
                if (job.has("group")) {
                    BandGroup group = groups.get(job.getString("group"));
                    group.add(bandtypes[i]);
                }
            }
        } catch (JSONException e) {
            Log.error("JSON parsing error", e);
        }
    }

    private static void updateBandGroups(JSONArray jsonGroupArray) {
        try {
            for (int i = 0; i < jsonGroupArray.length(); i++) {
                BandGroup group = new BandGroup();
                JSONObject job = (JSONObject) jsonGroupArray.get(i);
                if (job.has("groupLabel")) {
                    group.setGroupLabel(job.getString("groupLabel"));
                }
                if (job.has("key")) {
                    groups.put(job.getString("key"), group);
                    orderedGroups.add(group);
                }
            }
        } catch (JSONException e) {
            Log.error("JSON parsing error", e);
        }
    }

    public static BandType[] getBandTypes(BandGroup group) {
        return group.bandtypes.toArray(new BandType[group.bandtypes.size()]);
    }

    public static List<BandGroup> getOrderedGroups() {
        return orderedGroups;
    }

}
