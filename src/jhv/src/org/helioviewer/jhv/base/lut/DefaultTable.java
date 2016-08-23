package org.helioviewer.jhv.base.lut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Singleton class which gives for a given layer the default layer back
 * 
 * @author Helge Dietert
 * 
 */
public class DefaultTable {

    private static final DefaultTable instance = new DefaultTable();

    public static DefaultTable getSingletonInstance() {
        return instance;
    }

    private DefaultTable() {
        try {
            InputStream is = FileUtils.getResourceInputStream("/settings/colors.js");
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                colorRules = new JSONArray(new JSONTokener(in));
            } finally {
                is.close();
            }
        } catch (IOException e) {
            Log.warn("Error reading the configuration for the default color tables", e);
            Message.warn("Color Tables", "Error reading the configuration for the default color tables: " + e.getMessage() + "\n" + "There will be no default color tables applied.");
        } catch (JSONException e) {
            Log.warn("Error reading the configuration for the default color tables", e);
            Message.warn("Color Tables", "Error reading the configuration for the default color tables: " + e.getMessage() + "\n" + "There will be no default color tables applied.");
        }
    }

    /**
     * List of rules to apply
     */
    private JSONArray colorRules;

    /**
     * Gives back the default color table matching to the meta data
     * 
     * @param hvMetaData
     *            Metadata to look for
     * @return name (key) of default color table, null if no rule applies
     */
    public String getColorTable(HelioviewerMetaData hvMetaData) {
        // Only if the config is fine
        if (colorRules == null)
            return null;

        int length = colorRules.length();
        for (int i = 0; i < length; ++i) {
            try {
                JSONObject rule = colorRules.getJSONObject(i);
                if (rule.has("observatory") && !rule.getString("observatory").equalsIgnoreCase(hvMetaData.getObservatory())) {
                        continue;
                }
                if (rule.has("instrument") && !rule.getString("instrument").equalsIgnoreCase(hvMetaData.getInstrument())) {
                        continue;
                }
                if (rule.has("detector") && !rule.getString("detector").equalsIgnoreCase(hvMetaData.getDetector())) {
                        continue;
                }
                if (rule.has("measurement") && !rule.getString("measurement").equalsIgnoreCase(hvMetaData.getMeasurement())) {
                        continue;
                }
                return rule.getString("color");
            } catch (JSONException e) {
                Log.warn("Rule " + i + " for the default color table is invalid!", e);
            }
        }
        return null;
    }

}
