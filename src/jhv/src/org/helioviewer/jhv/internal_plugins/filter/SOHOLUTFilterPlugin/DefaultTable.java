package org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
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
    /**
     * Singleton instance
     */
    private static DefaultTable singleton;

    /**
     * Returns the only instance of this class and will be initialize on the
     * first use.
     * 
     * @return the only instance of this class.
     * */
    public static DefaultTable getSingletonInstance() {
        if (singleton == null)
            singleton = new DefaultTable();
        return singleton;
    }

    /**
     * List of rules to apply
     */
    private JSONArray colorRules;

    /**
     * Used as singleton
     */
    private DefaultTable() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(FileUtils.getResourceInputStream("/settings/colors.js"), "UTF-8"));
            colorRules = new JSONArray(new JSONTokener(in));
        } catch (IOException e) {
            Log.warn("Error reading the configuration for the default color tables", e);
            Message.warn("Color Tables", "Error reading the configuration for the default color tables: " + e.getMessage() + "\n" + "There will be no default color tables applied.");
        } catch (JSONException e) {
            Log.warn("Error reading the configuration for the default color tables", e);
            Message.warn("Color Tables", "Error reading the configuration for the default color tables: " + e.getMessage() + "\n" + "There will be no default color tables applied.");
        }
    }

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
        for (int i = 0; i < colorRules.length(); ++i) {
            try {
                JSONObject rule = colorRules.getJSONObject(i);
                if (rule.has("observatory")) {
                    if (!rule.getString("observatory").equalsIgnoreCase(hvMetaData.getObservatory()))
                        continue;
                }
                if (rule.has("instrument")) {
                    if (!rule.getString("instrument").equalsIgnoreCase(hvMetaData.getInstrument()))
                        continue;
                }
                if (rule.has("detector")) {
                    if (!rule.getString("detector").equalsIgnoreCase(hvMetaData.getDetector()))
                        continue;
                }
                if (rule.has("measurement")) {
                    if (!rule.getString("measurement").equalsIgnoreCase(hvMetaData.getMeasurement()))
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
