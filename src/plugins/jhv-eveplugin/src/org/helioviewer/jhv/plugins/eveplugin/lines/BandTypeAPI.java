package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.timelines.TimelineSettings;
import org.helioviewer.jhv.timelines.data.BandGroup;
import org.helioviewer.jhv.timelines.data.BandType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BandTypeAPI {

    private static final HashMap<String, BandGroup> groups = new HashMap<>();
    private static final ArrayList<BandGroup> orderedGroups = new ArrayList<>();

    public static void getDatasets() {
        try {
            JSONObject jsonmain = JSONUtils.getJSONStream(new DownloadStream(TimelineSettings.baseURL).getInput());
            updateBandGroups(jsonmain.getJSONArray("groups"));
            updateBandTypes(jsonmain.getJSONArray("objects"));
        } catch (UnknownHostException e) {
            Log.debug("Unknown host, network down?", e);
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

    public static BandGroup[] getGroups() {
        return orderedGroups.toArray(new BandGroup[orderedGroups.size()]);
    }

}
