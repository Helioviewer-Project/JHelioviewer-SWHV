package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BandTypeAPI {

    private static BandTypeAPI singletonInstance;

    private static final HashMap<String, BandGroup> groups = new HashMap<String, BandGroup>();
    private static final ArrayList<BandGroup> orderedGroups = new ArrayList<BandGroup>();

    private final Properties defaultProperties = new Properties();
    private final String baseURL;

    public static BandTypeAPI getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new BandTypeAPI();
        }
        return singletonInstance;
    }

    private BandTypeAPI() {
        loadSettings();
        baseURL = defaultProperties.getProperty("plugin.eve.dataseturl");
        updateDatasets();
    }

    private void loadSettings() {
        InputStream defaultPropStream = EVEPlugin.class.getResourceAsStream("/settings/eveplugin.properties");
        try {
            defaultProperties.load(defaultPropStream);
            defaultPropStream.close();
        } catch (IOException e) {
            Log.error("BandTypeAPI.loadSettings() > Could not load settings", e);
        }
    }

    private String readJSON() {
        String string = null;
        URL url = null;
        try {
            url = new URL(baseURL + "/datasets/index.php");
        } catch (MalformedURLException e) {
            Log.error("Malformed URL", e);
        }

        File dstFile = new File(JHVDirectory.PLUGINS.getPath() + "/EVEPlugin/datasets.json");
        try {
            FileUtils.save(new DownloadStream(url).getInput(), dstFile);
        } catch (UnknownHostException e) {
            Log.debug("Unknown host, network down?", e);
        } catch (IOException e) {
            Log.debug("Error downloading the bandtypes", e);
        }

        try {
            string = FileUtils.read(dstFile);
        } catch (IOException e) {
            Log.debug("Error reading the bandtypes", e);
        }
        return string;
    }

    private void updateBandTypes(JSONArray jsonObjectArray) {
        BandType[] bandtypes = new BandType[jsonObjectArray.length()];
        try {
            for (int i = 0; i < jsonObjectArray.length(); i++) {
                bandtypes[i] = new BandType();
                JSONObject job = (JSONObject) jsonObjectArray.get(i);

                if (job.has("label")) {
                    bandtypes[i].setLabel((String) job.get("label"));
                }
                if (job.has("name")) {
                    bandtypes[i].setName((String) job.get("name"));
                }
                if (job.has("range")) {
                    JSONArray rangeArray = job.getJSONArray("range");
                    Double v0 = rangeArray.getDouble(0);
                    Double v1 = rangeArray.getDouble(1);
                    bandtypes[i].setMin(v0);
                    bandtypes[i].setMax(v1);
                }
                if (job.has("unitLabel")) {
                    bandtypes[i].setUnitLabel((String) job.get("unitLabel"));
                }
                if (job.has("baseUrl")) {
                    bandtypes[i].setBaseURL((String) job.get("baseUrl"));
                }
                if (job.has("scale")) {
                    bandtypes[i].setScale(job.getString("scale"));
                }
                if (job.has("warnLevels")) {
                    JSONArray warnLevels = job.getJSONArray("warnLevels");
                    for (int j = 0; j < warnLevels.length(); j++) {
                        JSONObject helpobj = (JSONObject) warnLevels.get(j);
                        bandtypes[i].warnLevels.put((String) helpobj.get("warnLabel"), helpobj.getDouble("warnValue"));
                    }
                }
                if (job.has("group")) {
                    BandGroup group = groups.get(job.getString("group"));
                    group.add(bandtypes[i]);
                    bandtypes[i].setGroup(group);
                }
            }
        } catch (JSONException e) {
            Log.error("JSON parsing error", e);
        }
    }

    private void updateBandGroups(JSONArray jsonGroupArray) {
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

    private void updateDatasets() {
        try {
            String jsonString = readJSON();
            JSONObject jsonmain = new JSONObject(jsonString);
            JSONArray jsonGroupArray = (JSONArray) jsonmain.get("groups");
            updateBandGroups(jsonGroupArray);
            JSONArray jsonObjectArray = (JSONArray) jsonmain.get("objects");
            updateBandTypes(jsonObjectArray);
        } catch (JSONException e) {
            Log.error("JSON parsing error", e);
        }
    }

    public BandType[] getBandTypes(BandGroup group) {
        return group.bandtypes.toArray(new BandType[group.bandtypes.size()]);
    }

    public List<BandGroup> getOrderedGroups() {
        return orderedGroups;
    }

}
