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
        active = loadActive();
    }

    public abstract void install();

    public abstract void uninstall();

    public void installGUI() {}

    public void uninstallGUI() {}

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

    private boolean loadActive() {
        String p = Settings.getProperty("plugins." + this + ".active");
        return p == null || Boolean.parseBoolean(p);
    }

    final void setActive(boolean _active, boolean guiEnabled) {
        if (active == _active)
            return;

        active = _active;
        Settings.setProperty("plugins." + this + ".active", Boolean.toString(active));
        if (active)
            activate(guiEnabled);
        else
            deactivate(guiEnabled);
    }

    final void activate(boolean guiEnabled) {
        install();
        if (guiEnabled)
            installGUI();
    }

    final void deactivate(boolean guiEnabled) {
        if (guiEnabled)
            uninstallGUI();
        uninstall();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
