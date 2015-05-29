package org.helioviewer.jhv.plugins.eveplugin.settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BandTypeAPI extends APIAbstract {

    private static BandTypeAPI singletonInstance;
    private BandType[] bandtypes;
    private Boolean isUpdated = Boolean.FALSE;
    private final HashMap<String, BandGroup> groups = new HashMap<String, BandGroup>();
    private final List<BandGroup> orderedGroups = new ArrayList<BandGroup>();

    private final Properties defaultProperties = new Properties();

    public static BandTypeAPI getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new BandTypeAPI();
        }
        return singletonInstance;
    }

    private BandTypeAPI() {
        super();
        LogSettings.init("/settings/log4j.initial.properties", JHVDirectory.SETTINGS.getPath() + "log4j.properties", JHVDirectory.LOGS.getPath(), false);
        this.loadSettings();
        this.setBaseUrl(defaultProperties.getProperty("plugin.eve.dataseturl"));
        this.updateDatasets();
    }

    private void loadSettings() {
        InputStream defaultPropStream = EVEPlugin.class.getResourceAsStream("/settings/eveplugin.properties");
        try {
            defaultProperties.load(defaultPropStream);
        } catch (IOException ex) {
            Log.error(">> Settings.load(boolean) > Could not load settings", ex);
        }
    }

    private String getDatasetUrl() {
        return this.getBaseUrl() + "/datasets/index.php";
    }

    @Override
    public String getUrl() {
        return this.getBaseUrl();
    }

    private String readJSON() {
        String string = null;
        URI url = null;
        try {
            url = new URI(this.getDatasetUrl());
        } catch (URISyntaxException e) {
            Log.error("Malformed url", e);
        }
        final File dstFile = new File(JHVDirectory.PLUGINS.getPath() + "/EVEPlugin/datasets.json");
        try {
            DownloadStream ds = new DownloadStream(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());
            FileUtils.save(ds.getInput(), dstFile);
        } catch (UnknownHostException e) {
            Log.debug("Unknown host, network down?", e);
        } catch (final IOException e1) {
            Log.debug("Error downloading the bandtypes.", e1);
        } catch (URISyntaxException e2) {
            Log.debug("Malformed url", e2);
        }
        try {
            string = FileUtils.read(dstFile);
        } catch (final IOException e1) {
            Log.debug("Error reading the bandtypes.", e1);
        }
        return string;
    }

    private void updateBandTypes(JSONArray jsonObjectArray) {
        bandtypes = new BandType[jsonObjectArray.length()];
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
                if (job.has("min")) {
                    bandtypes[i].setMin(job.getDouble("min"));
                }
                if (job.has("max")) {
                    bandtypes[i].setMax(job.getDouble("max"));
                }
                if (job.has("unitLabel")) {
                    bandtypes[i].setUnitLabel((String) job.get("unitLabel"));
                }
                if (job.has("baseUrl")) {
                    bandtypes[i].setBaseUrl((String) job.get("baseUrl"));
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
        } catch (JSONException e1) {
            Log.error("JSON parsing error", e1);
        }
    }

    private void updateBandGroups(JSONArray jsonGroupArray) {
        bandtypes = new BandType[jsonGroupArray.length()];
        try {
            for (int i = 0; i < jsonGroupArray.length(); i++) {
                BandGroup group = new BandGroup();
                JSONObject job = (JSONObject) jsonGroupArray.get(i);
                if (job.has("groupLabel")) {
                    group.setGroupLabel(job.getString("groupLabel"));
                }
                if (job.has("key")) {
                    group.setKey(job.getString("key"));
                    groups.put(job.getString("key"), group);
                    orderedGroups.add(group);
                }
            }
        } catch (JSONException e1) {
            Log.error("JSON parsing error", e1);
        }
    }

    public void updateDatasets() {
        try {
            String jsonString = readJSON();
            JSONObject jsonmain = new JSONObject(jsonString);
            JSONArray jsonGroupArray = (JSONArray) jsonmain.get("groups");
            updateBandGroups(jsonGroupArray);
            JSONArray jsonObjectArray = (JSONArray) jsonmain.get("objects");
            updateBandTypes(jsonObjectArray);
        } catch (JSONException e1) {
            Log.error("JSON parsing error", e1);
        }
        isUpdated = true;
    }

    public BandType[] getDatasets() {
        if (bandtypes == null) {
            this.updateDatasets();
        }
        return bandtypes;
    }

    public BandType[] getBandTypes() {
        return bandtypes;
    }

    public BandType[] getBandTypes(BandGroup group) {
        return group.bandtypes.toArray(new BandType[group.bandtypes.size()]);
    }

    public BandGroup[] getGroups() {
        if (!isUpdated) {
            updateDatasets();
        }
        return groups.values().toArray(new BandGroup[groups.size()]);
    }

    public List<BandGroup> getOrderedGroups() {
        if (!isUpdated) {
            updateDatasets();
        }
        return orderedGroups;
    }

}
