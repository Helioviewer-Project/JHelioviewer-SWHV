package org.helioviewer.jhv.ui;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.DisplaySettings;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.source.jpip.JPIPCacheManager;
import org.helioviewer.jhv.thread.Task;

// The settings: each change applies at once; proxy credentials are saved by close, and stay as drafts until then.
public final class SettingsScreen extends DialogScreen {

    public static final Panel.FileFilter STATE_FILTER = new Panel.FileFilter("State", "jhv");

    private static final int SERVER = 1;
    private static final int LOAD_STATE = 2;
    private static final int SELECT_STATE = 3;
    private static final int SAMP_HUB = 4;
    private static final int THEME = 5;
    private static final int NORMALIZE_AIA = 6;
    private static final int NORMALIZE_RADIUS = 7;
    private static final int TIME_MODE = 8;
    private static final int CLEAR_CACHE = 10;
    private static final int PROXY_USER = 11;
    private static final int PROXY_PASSWORD = 12;
    private static final int CLOSE = 13;
    private static final int PLUGIN = 100;
    private static final int PROXY_COLUMNS = 20;

    private static final String[] THEMES = Panel.labels(DisplaySettings.UITheme.values());
    private static final String[] TIME_MODES = Panel.labels(DisplaySettings.TimeMode.values());

    private final Panel buffer = new Panel();
    private final Runnable pluginChanged;
    private final Runnable themeChanged;

    private String cacheSize = "--.-";
    private boolean clearing;
    @Nullable
    private String proxyUser;
    @Nullable
    private String proxyPass;

    public SettingsScreen(Runnable _pluginChanged, Runnable _themeChanged) {
        pluginChanged = _pluginChanged;
        themeChanged = _themeChanged;
    }

    @Override
    public String title() {
        return "Settings";
    }

    @Override
    public void show() {
        if (proxyUser == null) {
            String user = Settings.getProperty("proxy.username");
            String password = ProxySettings.getPassword();
            proxyUser = user == null ? "" : user;
            proxyPass = password == null ? "" : password;
        }
        updateCacheSize();
    }

    @Override
    public void close() {
        if (proxyUser != null) {
            Settings.setProperty("proxy.username", proxyUser);
            ProxySettings.setPassword(proxyPass);
        }
        super.close();
    }

    private void updateCacheSize() {
        if (!JPIPCacheManager.isAvailable())
            return;
        Task.submitBackground("cache-size", JPIPCacheManager::getSize, size -> {
            cacheSize = String.format("%.1f", size / (1024 * 1024 * 1024.));
            changed();
        }, Log::error);
    }

    private static boolean loadState() {
        String state = Settings.getProperty("startup.loadState");
        return state != null && !"false".equals(state);
    }

    @Override
    public Panel build() {
        String[] servers = DataSources.getServers().toArray(new String[0]);
        int server = Math.min(Math.max(0, Arrays.asList(servers).indexOf(Settings.getProperty("dataSources.defaultServer"))), servers.length - 1);
        boolean loadState = loadState();
        Panel p = buffer.reset().grid(2).identity(1).stretch(1).gap(4).swingMargin(3, 32, 0, 33)
                .caption("Preferred server:").combo(SERVER, servers, server).endRow()
                .caption("At start-up:").row().fill()
                .check(LOAD_STATE, "Load state", loadState)
                .row().right().button(SELECT_STATE, "Select State").file(STATE_FILTER, Settings.getProperty("path.state"), false).enabled(loadState).rowEnd().rowEnd().endRow()
                .space(0, 0).check(SAMP_HUB, "Load SAMP hub", Boolean.parseBoolean(Settings.getProperty("startup.sampHub"))).endRow()
                .space(0, 0).row().label("Use theme").radio(THEME, THEMES, DisplaySettings.getUITheme().ordinal()).rowEnd().endRow()
                .caption("For new layers:").check(NORMALIZE_AIA, "Normalize SDO/AIA brightness", DisplaySettings.getNormalizeAIA()).endRow()
                .space(0, 0).check(NORMALIZE_RADIUS, "Normalize solar radius", DisplaySettings.getNormalizeRadius()).endRow()
                .space(0, 0).row().label("Use time at").radio(TIME_MODE, TIME_MODES, DisplaySettings.getTimeMode().ordinal()).rowEnd().endRow();
        List<Plugin> plugins = PluginManager.getPlugins();
        p.caption("Plugins:");
        if (plugins.isEmpty())
            p.space(0, 0).endRow();
        for (int i = 0; i < plugins.size(); i++) {
            Plugin plugin = plugins.get(i);
            if (i > 0)
                p.space(0, 0);
            p.owner(i + 1).check(PLUGIN, plugin.getName(), plugin.isActive()).description(plugin.getDescription()).endRow();
        }
        p.owner(0).gridEnd().separator();
        boolean available = JPIPCacheManager.isAvailable();
        p.grid(2).identity(2).stretch(0).gap(5).swingMargin(5, 14, 5, 14)
                .label(available ? "The image cache currently uses " + cacheSize + "GB on disk." : "JPIP disk cache is unavailable.")
                .button(CLEAR_CACHE, "Clear Cache").enabled(available && !clearing).gridEnd();
        return p.separator().grid(2).identity(3).stretch(1).gap(4).swingMargin(6, 11, 3, 11)
                .caption("Proxy username:").text(PROXY_USER, proxyUser == null ? "" : proxyUser, PROXY_COLUMNS).live().endRow()
                .caption("Proxy password:").text(PROXY_PASSWORD, proxyPass == null ? "" : proxyPass, PROXY_COLUMNS).password().live().endRow()
                .gridEnd().separator()
                .row().right().button(CLOSE, "Close").defaultButton().initialFocus().rowEnd();
    }

    @Override
    public void checked(int owner, int item, boolean value) {
        if (owner != 0) {
            List<Plugin> plugins = PluginManager.getPlugins();
            if (owner <= plugins.size()) {
                Plugin plugin = plugins.get(owner - 1);
                try {
                    PluginManager.setActive(plugin, value);
                } catch (RuntimeException failure) {
                    Log.error("Failed to change " + plugin.getName(), failure);
                    Message.err("Failed to change " + plugin.getName(), failure.getMessage());
                }
                pluginChanged.run();
            }
            return;
        }
        switch (item) {
            case LOAD_STATE -> Settings.setProperty("startup.loadState", Boolean.toString(value));
            case SAMP_HUB -> Settings.setProperty("startup.sampHub", Boolean.toString(value));
            case NORMALIZE_AIA -> DisplaySettings.setNormalizeAIA(value);
            case NORMALIZE_RADIUS -> DisplaySettings.setNormalizeRadius(value);
            default -> throw new IllegalArgumentException("Settings checked " + item);
        }
    }

    @Override
    public void choice(int owner, int item, int value) {
        switch (item) {
            case SERVER -> Settings.setProperty("dataSources.defaultServer", Panel.choice(DataSources.getServers().toArray(new String[0]), value));
            case THEME -> {
                DisplaySettings.setUITheme(Panel.choice(DisplaySettings.UITheme.values(), value));
                themeChanged.run();
            }
            case TIME_MODE -> DisplaySettings.setTimeMode(Panel.choice(DisplaySettings.TimeMode.values(), value));
            default -> throw new IllegalArgumentException("Settings choice " + item);
        }
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        switch (item) {
            case CLEAR_CACHE -> clearCache();
            case CLOSE -> close();
            default -> throw new IllegalArgumentException("Settings item " + item);
        }
    }

    @Override
    public void setText(int owner, int item, String text) {
        switch (item) {
            case PROXY_USER -> proxyUser = text;
            case PROXY_PASSWORD -> proxyPass = text;
            default -> throw new IllegalArgumentException("Settings text " + item);
        }
    }

    @Override
    public void files(int owner, int item, String[] paths) {
        if (item == SELECT_STATE && paths.length != 0 && new File(paths[0]).isFile())
            Settings.setProperty("startup.loadState", paths[0]);
    }

    private void clearCache() {
        clearing = true;
        Task.submitBackground("clear-cache", () -> {
            JPIPCacheManager.clear();
            return null;
        }, ignored -> cleared(), (logContext, t) -> {
            Log.error(logContext, t);
            cleared();
        });
    }

    private void cleared() {
        clearing = false;
        updateCacheSize();
        changed();
    }

}
