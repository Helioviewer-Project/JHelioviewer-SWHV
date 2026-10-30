package org.helioviewer.jhv.plugins;

import org.helioviewer.jhv.app.Settings;

import org.json.JSONObject;

public abstract class Plugin {

    private final String stateId;
    private final String preferenceId;
    private final String name;
    private final String description;
    private boolean active;

    protected Plugin(String _stateId, String _preferenceId, String _name, String _description) {
        stateId = _stateId;
        preferenceId = _preferenceId;
        name = _name;
        description = _description;
    }

    protected abstract void install();

    protected abstract void uninstall();

    protected void installGUI() {}

    protected void uninstallGUI() {}

    protected void writeState(JSONObject jo) {}

    protected void readState(JSONObject jo) {}

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final boolean isActive() {
        return active;
    }

    final void start(boolean guiEnabled) {
        String p = Settings.getProperty("plugins." + preferenceId + ".active");
        if (p == null || Boolean.parseBoolean(p))
            activate(guiEnabled);
    }

    final void setActive(boolean _active, boolean guiEnabled) {
        if (active == _active)
            return;

        if (_active)
            activate(guiEnabled);
        else
            deactivate(guiEnabled);
        Settings.setProperty("plugins." + preferenceId + ".active", Boolean.toString(active));
    }

    private void activate(boolean guiEnabled) {
        try {
            install();
            if (guiEnabled)
                installGUI();
            active = true;
        } catch (RuntimeException | Error failure) {
            try {
                deactivate(guiEnabled);
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private void deactivate(boolean guiEnabled) {
        active = false;
        try {
            if (guiEnabled)
                uninstallGUI();
        } finally {
            uninstall();
        }
    }

    final void saveState(JSONObject plugins) {
        JSONObject data = new JSONObject();
        writeState(data);
        plugins.put(stateId, data);
    }

    final void loadState(JSONObject plugins) {
        JSONObject data = plugins.optJSONObject(stateId, plugins.optJSONObject(preferenceId));
        if (data != null)
            readState(data);
    }

}
