package org.helioviewer.jhv.plugins;

import org.helioviewer.jhv.Settings;
import org.json.JSONObject;

public abstract class Plugin {

    private final String name;
    private final String description;
    private boolean active;

    protected Plugin(String _name, String _description) {
        name = _name;
        description = _description;
        active = getActive();
    }

    public abstract void install();

    public abstract void uninstall();

    public abstract void saveState(JSONObject jo);

    public abstract void loadState(JSONObject jo);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

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
