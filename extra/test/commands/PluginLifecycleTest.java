package org.helioviewer.jhv.plugins;

import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.io.Directories;

public final class PluginLifecycleTest {

    public static void main(String[] args) {
        Directories.createPersistentDirs();
        String key = "plugins.LegacyProbe.active";
        Settings.setProperty(key, "true");
        Probe plugin = new Probe();
        PluginManager.addPlugin(plugin);
        check(!plugin.isActive() && !plugin.model, "Failed startup retained an active plugin");
        check(Settings.getProperty(key).equals("true"), "Failed startup changed the saved preference");

        Settings.setProperty(key, "false");
        expectFailure(plugin);
        check(!plugin.isActive() && !plugin.model, "Failed activation retained model resources");
        check(Settings.getProperty(key).equals("false"), "Failed activation changed the saved preference");
        plugin.failModel = false;
        PluginManager.setActive(plugin, true);
        check(plugin.isActive() && plugin.model && !plugin.gui, "Headless activation installed GUI resources");
        int installs = plugin.installs;
        PluginManager.setActive(plugin, true);
        check(plugin.installs == installs, "Repeated activation installed twice");
        check(Settings.getProperty(key).equals("true"), "Successful activation did not save the preference");
        PluginManager.setActive(plugin, false);
        check(!plugin.isActive() && !plugin.model, "Deactivation retained model resources");

        PluginManager.setGUIEnabled(true);
        plugin.failGui = true;
        expectFailure(plugin);
        check(!plugin.isActive() && !plugin.model && !plugin.gui, "Failed GUI activation retained resources");
        check(Settings.getProperty(key).equals("false"), "Failed GUI activation changed the saved preference");
        plugin.failGui = false;
        PluginManager.setActive(plugin, true);
        check(plugin.isActive() && plugin.model && plugin.gui, "Retry did not install model and GUI");
        PluginManager.setActive(plugin, false);
        check(!plugin.isActive() && !plugin.model && !plugin.gui, "Deactivation retained GUI resources");
        System.out.println("PluginLifecycleTest passed");
    }

    private static void expectFailure(Probe plugin) {
        try {
            PluginManager.setActive(plugin, true);
            throw new AssertionError("Activation unexpectedly succeeded");
        } catch (IllegalStateException expected) {
            check(expected == plugin.failure, "Activation replaced the original failure");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static final class Probe extends Plugin {
        private final IllegalStateException failure = new IllegalStateException("Expected installation failure");
        private boolean failModel = true;
        private boolean failGui;
        private boolean model;
        private boolean gui;
        private int installs;

        Probe() { super("probe", "LegacyProbe", "Probe", "Lifecycle probe"); }

        @Override
        protected void install() {
            installs++;
            model = true;
            if (failModel) throw failure;
        }

        @Override
        protected void uninstall() { model = false; }

        @Override
        protected void installGUI() {
            gui = true;
            if (failGui) throw failure;
        }

        @Override
        protected void uninstallGUI() { gui = false; }
    }
}
