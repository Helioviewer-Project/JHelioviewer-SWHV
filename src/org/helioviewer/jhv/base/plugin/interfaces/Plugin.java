package org.helioviewer.jhv.base.plugin.interfaces;

import org.json.JSONObject;

public interface Plugin {

    String getName();

    String getDescription();

    void installPlugin();

    void uninstallPlugin();

    void saveState(JSONObject jo);

    void loadState(JSONObject jo);

    String getAboutLicenseText();

}
