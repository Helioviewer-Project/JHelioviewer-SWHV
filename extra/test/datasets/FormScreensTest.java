package org.helioviewer.jhv.ui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.DisplaySettings;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.PluginManager;

// The Settings and FITS forms through their shared definitions and real Swing widgets, without a window.
public final class FormScreensTest {

    private static final int LOAD_STATE_FILE = 3;
    private static final int CLEAR_CACHE = 10;
    private static final int PROXY_USER = 11;
    private static final int PROXY_PASSWORD = 12;
    private static final int PLUGIN = 100;

    private static final class FailingPlugin extends Plugin {
        FailingPlugin() {
            super("failing", "FailingPlugin", "Failing", "");
        }

        @Override
        protected void install() {
            throw new IllegalStateException("install failed");
        }

        @Override
        protected void uninstall() {}
    }

    // Lets a focus event reach a component without a window, which the focus manager would drop.
    private static final class Direct extends DefaultKeyboardFocusManager {
        @Override
        public boolean dispatchEvent(AWTEvent e) {
            return false;
        }
    }

    private static void focus(Component target, int id, boolean temporary) {
        KeyboardFocusManager previous = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        KeyboardFocusManager.setCurrentKeyboardFocusManager(new Direct());
        try {
            target.dispatchEvent(new FocusEvent(target, id, temporary));
        } finally {
            KeyboardFocusManager.setCurrentKeyboardFocusManager(previous);
        }
    }

    private static void enter(JTextField field) {
        focus(field, FocusEvent.FOCUS_GAINED, false);
        DialogScreenTest.key(field, "ENTER");
    }

    private static void leave(JTextField field) {
        focus(field, FocusEvent.FOCUS_LOST, false);
    }

    private static void mouse(Component target, int id, int clicks) {
        target.dispatchEvent(new MouseEvent(target, id, 0, InputEvent.BUTTON1_DOWN_MASK, 1, 1, clicks, false, MouseEvent.BUTTON1));
    }

    private static SettingsScreen settingsScreen() {
        return new SettingsScreen(() -> {}, () -> {});
    }

    private static int control(Panel panel, Panel.Kind kind, int id) {
        for (int i = 0; i < panel.size(); i++) {
            if (panel.kind(i) == kind && panel.value(i, Panel.IntField.ID) == id)
                return i;
        }
        throw new AssertionError("Missing " + kind + "/" + id);
    }

    private static String proxyText(Panel panel, int id) {
        return panel.string(control(panel, Panel.Kind.TEXT, id), Panel.IntField.TEXT);
    }

    // The stored format and what a window that never saw an edit leaves behind.
    private static void proxyFormat() {
        DialogScreenTest.require(ProxySettings.getPassword() == null, "Absent proxy password was not null");
        SettingsScreen untouched = settingsScreen();
        untouched.show();
        untouched.close();
        DialogScreenTest.require("".equals(Settings.getProperty("proxy.username")) && "".equals(ProxySettings.getPassword()), "Close saves empty credentials, as the fields show them");
        Settings.setProperty("proxy.password", "YQ==");
        DialogScreenTest.require("a".equals(ProxySettings.getPassword()), "Padded legacy password was not decoded");
        Settings.setProperty("proxy.password", "%%%invalid");
        DialogScreenTest.require(ProxySettings.getPassword() == null, "Malformed proxy password was accepted");
        ProxySettings.setPassword("a");
        DialogScreenTest.require("YQ".equals(Settings.getProperty("proxy.password")), "Proxy password gained Base64 padding");
    }

    // Edits wait for close, survive a window closed without it, and are saved by the next close.
    private static void proxyDrafts() {
        Settings.setProperty("proxy.username", "original");
        int[] dismissed = new int[1];
        SettingsScreen window = settingsScreen();
        window.onDismiss(() -> dismissed[0]++);
        window.show();
        DialogScreenTest.require("original".equals(proxyText(window.build(), PROXY_USER)), "The window did not read the stored username");
        window.setText(0, PROXY_USER, "edited");
        window.setText(0, PROXY_PASSWORD, "pāss🔑");
        DialogScreenTest.require("original".equals(Settings.getProperty("proxy.username")), "A proxy edit was saved before Close");
        DialogScreenTest.require("YQ".equals(Settings.getProperty("proxy.password")), "A proxy password edit was saved before Close");
        Panel panel = window.build();
        DialogScreenTest.require("edited".equals(proxyText(panel, PROXY_USER)), "A panel refresh discarded the proxy draft");
        DialogScreenTest.require("pāss🔑".equals(proxyText(panel, PROXY_PASSWORD)), "A panel refresh discarded the password draft");
        window.show();
        panel = window.build();
        DialogScreenTest.require("edited".equals(proxyText(panel, PROXY_USER)) && "pāss🔑".equals(proxyText(panel, PROXY_PASSWORD)), "Reopening discarded the drafts");
        DialogScreenTest.require("original".equals(Settings.getProperty("proxy.username")) && "YQ".equals(Settings.getProperty("proxy.password")), "Reopening saved the drafts");
        window.close();
        DialogScreenTest.require("edited".equals(Settings.getProperty("proxy.username")), "Close did not save the proxy draft");
        DialogScreenTest.require("cMSBc3Pwn5SR".equals(Settings.getProperty("proxy.password")) && "pāss🔑".equals(ProxySettings.getPassword()), "Password UTF-8/Base64 format changed");
        DialogScreenTest.require(dismissed[0] == 1, "Close did not dismiss the window");
    }

    private static void swingProxy() {
        SettingsScreen screen = settingsScreen();
        int[] closed = new int[1];
        screen.onDismiss(() -> closed[0]++);
        screen.show();
        SwingPanel swing = DialogScreenTest.swing(screen);
        JPasswordField password = DialogScreenTest.all(swing, JPasswordField.class).getFirst();
        DialogScreenTest.require("pāss🔑".equals(new String(password.getPassword())), "Swing did not read the stored password");
        password.setText("");
        DialogScreenTest.require("cMSBc3Pwn5SR".equals(Settings.getProperty("proxy.password")), "Swing saved a password before Close");
        DialogScreenTest.require("Close".equals(swing.getRootPane().getDefaultButton().getText()), "Enter in a proxy field must close, as the original default action");
        DialogScreenTest.button(swing, "Close").doClick();
        DialogScreenTest.require("".equals(Settings.getProperty("proxy.password")) && "".equals(ProxySettings.getPassword()), "Swing did not save an empty password");
        DialogScreenTest.require(closed[0] == 1, "Swing Close did not dismiss the window");
    }

    private static void settingsPlugins() {
        Settings.setProperty("plugins.FailingPlugin.active", "false");
        PluginManager.addPlugin(new FailingPlugin());
        int[] refreshed = new int[1];
        SettingsScreen window = new SettingsScreen(() -> refreshed[0]++, () -> {});
        DialogScreenTest.require(!DialogScreenTest.shows(window, "Record video as:"), "The video format moved to the transport options");
        int owner = PluginManager.getPlugins().size();
        window.checked(owner, PLUGIN, true); // The failure is reported, not thrown.
        DialogScreenTest.require(!PluginManager.getPlugins().get(owner - 1).isActive(), "A failed plugin stayed active");
        DialogScreenTest.require(refreshed[0] == 1, "A plugin change did not reach its frontend");
    }

    private static void clearCacheUnavailable() {
        SettingsScreen settings = settingsScreen();
        Panel panel = settings.build();
        DialogScreenTest.require(!panel.has(control(panel, Panel.Kind.BUTTON, CLEAR_CACHE), Panel.ENABLED) && DialogScreenTest.shows(settings, "JPIP disk cache is unavailable."),
                "Clear Cache must be disabled without a JPIP cache");
    }

    // The Swing widgets reach the same preferences as the callbacks they replaced.
    private static void settingsWidgets() throws Exception {
        File state = File.createTempFile("settings", ".jhv");
        state.deleteOnExit();
        Settings.setProperty("startup.loadState", "false");
        Settings.setProperty("startup.sampHub", "false");
        DisplaySettings.setNormalizeAIA(true);
        DisplaySettings.setNormalizeRadius(false);
        DisplaySettings.setUITheme(DisplaySettings.UITheme.Dark);
        DisplaySettings.setTimeMode(DisplaySettings.TimeMode.Observer);
        String[] servers = DataSources.getServers().toArray(new String[0]);
        Settings.setProperty("dataSources.defaultServer", servers[servers.length - 1]);
        int[] themes = new int[1];
        SettingsScreen screen = new SettingsScreen(() -> {}, () -> themes[0]++);
        SwingPanel swing = DialogScreenTest.swing(screen);
        AbstractButton load = DialogScreenTest.button(swing, "Load state");
        AbstractButton select = DialogScreenTest.button(swing, "Select State");
        Rectangle loadBounds = SwingUtilities.convertRectangle(load.getParent(), load.getBounds(), swing);
        Rectangle selectBounds = SwingUtilities.convertRectangle(select.getParent(), select.getBounds(), swing);
        DialogScreenTest.require(loadBounds.y == selectBounds.y && loadBounds.height == selectBounds.height,
                "Load state must take clicks across the row of its button: " + loadBounds + " beside " + selectBounds);
        DialogScreenTest.require(!select.isEnabled(), "Select State must follow Load state");
        load.doClick();
        DialogScreenTest.require("true".equals(Settings.getProperty("startup.loadState")) && select.isEnabled(), "Load state did not reach the setting and its button");
        screen.files(0, LOAD_STATE_FILE, new String[]{state + ".missing"});
        DialogScreenTest.require("true".equals(Settings.getProperty("startup.loadState")), "A missing state file replaced the setting");
        screen.files(0, LOAD_STATE_FILE, new String[]{state.toString()});
        DialogScreenTest.require(state.toString().equals(Settings.getProperty("startup.loadState")), "The chosen state file was not stored");
        swing.refresh();
        DialogScreenTest.require(load.isSelected() && select.isEnabled(), "A stored state file must read as Load state");
        load.doClick();
        DialogScreenTest.require("false".equals(Settings.getProperty("startup.loadState")) && !select.isEnabled(), "Clearing Load state kept the file");
        DialogScreenTest.button(swing, "Load SAMP hub").doClick();
        DialogScreenTest.require("true".equals(Settings.getProperty("startup.sampHub")), "SAMP hub did not reach its setting");
        DialogScreenTest.button(swing, "Normalize SDO/AIA brightness").doClick();
        DialogScreenTest.require(!DisplaySettings.getNormalizeAIA(), "AIA normalization did not reach its setting");
        DialogScreenTest.button(swing, "Normalize solar radius").doClick();
        DialogScreenTest.require(DisplaySettings.getNormalizeRadius(), "Radius normalization did not reach its setting");
        DialogScreenTest.button(swing, "Light").doClick();
        DialogScreenTest.require(DisplaySettings.getUITheme() == DisplaySettings.UITheme.Light && themes[0] == 1, "Theme did not reach its setting and its frontend");
        DialogScreenTest.button(swing, "Earth").doClick();
        DialogScreenTest.require(DisplaySettings.getTimeMode() == DisplaySettings.TimeMode.Earth, "Time mode did not reach its setting");
        JComboBox<?> server = DialogScreenTest.all(swing, JComboBox.class).getFirst();
        DialogScreenTest.require(server.getSelectedIndex() == servers.length - 1, "The preferred server was not selected");
        server.setSelectedIndex(0);
        DialogScreenTest.require(servers[0].equals(Settings.getProperty("dataSources.defaultServer")), "The preferred server did not reach its setting");
        DisplaySettings.setUITheme(DisplaySettings.UITheme.Dark);
        DisplaySettings.setTimeMode(DisplaySettings.TimeMode.Observer);
    }

    // The Swing widgets of the shared form reach the same model as the native callbacks.
    private static void fitsWidgets() {
        ImageProcessingSettings state = new ImageProcessingSettings(() -> {});
        SwingPanel swing = DialogScreenTest.swing(new FitsOptions(state));
        DialogScreenTest.require("<html><body>pixel<sup>1/γ</sup>".equals(DialogScreenTest.button(swing, "γ").getToolTipText())
                && "<html><body>asinh(pixel / 2<sup>β</sup>)".equals(DialogScreenTest.button(swing, "β").getToolTipText())
                && "<html><body>log1p(10<sup>α</sup>⋅pixel) / log1p(10<sup>α</sup>)".equals(DialogScreenTest.button(swing, "α").getToolTipText()), "FITS tips lost their superscripts");
        List<JTextField> clip = DialogScreenTest.all(swing, JTextField.class);
        JTextField min = clip.getFirst();
        JTextField max = clip.getLast();
        DialogScreenTest.require(!min.isEditable(), "The clip range must wait for Range");
        mouse(min, MouseEvent.MOUSE_PRESSED, 1);
        DialogScreenTest.require(state.fitsParameters().clippingMode() == ImageProcessingSettings.ClippingMode.Range && min.isEditable(), "Pressing a clip field did not select Range");

        min.setText(" 12.5 ");
        enter(min);
        DialogScreenTest.require(state.fitsParameters().clippingMin() == 12.5 && " 12.5 ".equals(min.getText()), "Enter must commit the value and keep the text typed: " + min.getText());
        leave(min);
        DialogScreenTest.require("12.5000".equals(min.getText()), "Focus loss must format the committed value: " + min.getText());
        min.setText("12.5");
        enter(min);
        DialogScreenTest.require(state.fitsParameters().clippingMin() == 12.5 && "12.5".equals(min.getText()), "Enter on the current value must keep the text typed: " + min.getText());
        leave(min);
        DialogScreenTest.require("12.5000".equals(min.getText()), "Focus loss must format the current value: " + min.getText());
        min.setText("1e30");
        enter(min);
        DialogScreenTest.require(state.fitsParameters().clippingMin() == ImageProcessingSettings.CLIP_LIMIT && "1e30".equals(min.getText()), "Enter must clamp the value and keep the text typed: " + min.getText());
        min.setText("1e40");
        enter(min);
        leave(min);
        DialogScreenTest.require("1.00000e+20".equals(min.getText()), "Focus loss must show a value clamped to the current one: " + min.getText());
        min.setText("12.5");
        enter(min);
        leave(min);

        min.setText("invalid");
        enter(min);
        DialogScreenTest.require(state.fitsParameters().clippingMin() == 12.5 && "invalid".equals(min.getText()), "Enter must keep a rejected edit");
        focus(min, FocusEvent.FOCUS_LOST, true);
        DialogScreenTest.require("invalid".equals(min.getText()), "A temporary focus loss must keep the edit");
        swing.refresh();
        DialogScreenTest.require("invalid".equals(min.getText()), "A refresh must keep an edit in progress");
        leave(min);
        DialogScreenTest.require("12.5000".equals(min.getText()), "Focus loss must revert a rejected edit: " + min.getText());
        max.setText("40");
        leave(max);
        DialogScreenTest.require(state.fitsParameters().clippingMax() == 40 && "40.0000".equals(max.getText()), "Focus loss must commit a valid edit: " + max.getText());
        state.setClippingMin(1.23456789);
        swing.refresh();
        DialogScreenTest.require("1.23457".equals(min.getText()), "A model change did not reach the field: " + min.getText());
        leave(min);
        DialogScreenTest.require(state.fitsParameters().clippingMin() == 1.23456789, "Leaving an unedited field changed its value");
        min.setText("1.2345678912");
        enter(min);
        DialogScreenTest.require(state.fitsParameters().clippingMin() == 1.2345678912, "The field committed its formatted text, not the value typed");

        JHVSlider beta = DialogScreenTest.all(swing, JHVSlider.class).get(1);
        beta.setValue(beta.getMaximum());
        DialogScreenTest.require(state.fitsParameters().beta() == ImageProcessingSettings.BETA_MIN, "The slider index did not reach its parameter");
        DialogScreenTest.button(swing, "γ").doClick();
        DialogScreenTest.require(state.fitsParameters().scalingMode() == ImageProcessingSettings.ScalingMode.Gamma, "The scaling radio did not reach its mode");
        mouse(beta, MouseEvent.MOUSE_PRESSED, 1);
        DialogScreenTest.require(state.fitsParameters().scalingMode() == ImageProcessingSettings.ScalingMode.Beta, "Pressing a slider did not select its mode");
        mouse(beta, MouseEvent.MOUSE_CLICKED, 2);
        DialogScreenTest.require(state.fitsParameters().beta() == 1. / (1 << 6), "Double-click did not restore the first value");
        swing.setEnabled(false);
        swing.refresh();
        DialogScreenTest.require(!min.isEnabled() && !beta.isEnabled(), "A refresh undid the outer disable");
    }

    private static void run() throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        DataSources.initSources();
        proxyFormat();
        proxyDrafts();
        settingsPlugins();
        clearCacheUnavailable();
        SwingUtilities.invokeAndWait(() -> {
            try {
                DialogScreenTest.theme();
                swingProxy();
                settingsWidgets();
                fitsWidgets();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public static void main(String[] args) {
        int status = 0;
        try {
            run();
            System.out.println("FormScreensTest passed");
        } catch (Throwable failure) {
            failure.printStackTrace();
            status = 1;
        }
        System.exit(status);
    }

}
