package org.helioviewer.jhv.plugins;

import org.helioviewer.jhv.Settings;
import org.json.JSONObject;

public abstract class Plugin {

    private boolean active;

    public Plugin() {
        active = getActive();
    }

    public abstract String getName();
    public abstract String getDescription();
    public abstract void install();
    public abstract void uninstall();
    public abstract void saveState(JSONObject jo);
    public abstract void loadState(JSONObject jo);

    public boolean isActive() {
        return active;
    }

    private boolean getActive() {
        String p = Settings.getProperty("plugins." + this + ".active");
        return p == null || Boolean.parseBoolean(p);
    }

    public void toggleActive() {
        active = !active;
        Settings.setProperty("plugins." + this + ".active", Boolean.toString(active));
        if (active)
            install();
        else
            uninstall();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
