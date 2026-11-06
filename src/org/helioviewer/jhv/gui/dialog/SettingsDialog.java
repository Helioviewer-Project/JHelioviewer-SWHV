package org.helioviewer.jhv.gui.dialog;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.helioviewer.jhv.app.DisplaySettings;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.gui.component.FormPanel;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.source.jpip.JPIPCacheManager;
import org.helioviewer.jhv.thread.Task;

import com.jidesoft.dialog.ButtonPanel;

@SuppressWarnings("serial")
public final class SettingsDialog extends BaseDialog {

    private final JLabel cacheSizeLabel = new JLabel(cacheSizeText("--.-"));
    private final JButton clearCache = new JButton("Clear");
    private final JTextField username = new JTextField(Settings.getProperty("proxy.username"));
    private final JPasswordField password = new JPasswordField();

    private void updateCacheSize() {
        if (!JPIPCacheManager.isAvailable()) {
            clearCache.setEnabled(false);
            cacheSizeLabel.setText("Unavailable");
            return;
        }
        Task.submitBackground("cache-size",
                JPIPCacheManager::getSize,
                size -> {
                    if (JPIPCacheManager.isAvailable())
                        cacheSizeLabel.setText(cacheSizeText(String.format("%.1f", size / (1024 * 1024 * 1024.))));
                },
                Log::error);
    }

    private static String cacheSizeText(String size) {
        return size + "GB on disk";
    }

    public SettingsDialog() {
        super("Settings", true);
        setResizable(false);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Settings.setProperty("proxy.username", username.getText());
                ProxySettings.setPassword(new String(password.getPassword()));
                setVisible(false);
            }
        };
        setDefaultAction(close);
        setDefaultCancelAction(close);

        JButton closeBtn = new JButton(close);
        closeBtn.setText("Close");
        setInitFocusedComponent(closeBtn);

        ButtonPanel panel = new ButtonPanel();
        panel.add(closeBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        FormPanel form = new FormPanel();

        JComboBox<String> comboServer = new JComboBox<>(DataSources.getServers().toArray(new String[0]));
        comboServer.setSelectedItem(Settings.getProperty("dataSources.defaultServer"));
        comboServer.addActionListener(e -> Settings.setProperty("dataSources.defaultServer", (String) Objects.requireNonNull(comboServer.getSelectedItem())));
        form.addRow("Preferred server", comboServer);

        String propState = Settings.getProperty("startup.loadState");
        boolean loadState = propState != null && !"false".equals(propState);
        JButton selectState = new JButton("Select State");
        selectState.setEnabled(loadState);
        selectState.addActionListener(e -> {
            File file = LoadStateDialog.get();
            if (file != null)
                Settings.setProperty("startup.loadState", file.toString());
        });
        JCheckBox defaultState = new JCheckBox("Load state", loadState);
        defaultState.addActionListener(e -> {
            selectState.setEnabled(defaultState.isSelected());
            Settings.setProperty("startup.loadState", Boolean.toString(defaultState.isSelected()));
        });
        form.addRow("At start-up", defaultState, selectState);

        JCheckBox sampHub = new JCheckBox("Load SAMP hub", Boolean.parseBoolean(Settings.getProperty("startup.sampHub")));
        sampHub.addActionListener(e -> Settings.setProperty("startup.sampHub", Boolean.toString(sampHub.isSelected())));
        form.addRow("", sampHub);

        ButtonGroup themeGroup = new ButtonGroup();
        DisplaySettings.UITheme currentTheme = DisplaySettings.getUITheme();
        DisplaySettings.UITheme[] themes = DisplaySettings.UITheme.values();
        JComponent[] themeRow = new JComponent[themes.length + 1];
        themeRow[0] = new JLabel("Use theme");
        for (int i = 0; i < themes.length; i++) {
            DisplaySettings.UITheme theme = themes[i];
            JRadioButton radio = new JRadioButton(theme.toString(), theme == currentTheme);
            radio.addItemListener(e -> {
                if (radio.isSelected())
                    DisplaySettings.setUITheme(theme);
            });
            themeGroup.add(radio);
            themeRow[i + 1] = radio;
        }
        form.addRow("", themeRow);

        JCheckBox normalizeAIA = new JCheckBox("Normalize SDO/AIA brightness", DisplaySettings.getNormalizeAIA());
        normalizeAIA.addActionListener(e -> DisplaySettings.setNormalizeAIA(normalizeAIA.isSelected()));
        form.addRow("For new layers", normalizeAIA);

        JCheckBox normalizeRadius = new JCheckBox("Normalize solar radius", DisplaySettings.getNormalizeRadius());
        normalizeRadius.addActionListener(e -> DisplaySettings.setNormalizeRadius(normalizeRadius.isSelected()));
        form.addRow("", normalizeRadius);

        ButtonGroup timeModeGroup = new ButtonGroup();
        DisplaySettings.TimeMode timeMode = DisplaySettings.getTimeMode();
        DisplaySettings.TimeMode[] modes = DisplaySettings.TimeMode.values();
        JComponent[] timeRow = new JComponent[modes.length + 1];
        timeRow[0] = new JLabel("Use time at");
        for (int i = 0; i < modes.length; i++) {
            DisplaySettings.TimeMode mode = modes[i];
            JRadioButton radio = new JRadioButton(mode.toString(), mode == timeMode);
            radio.addItemListener(e -> {
                if (radio.isSelected())
                    DisplaySettings.setTimeMode(mode);
            });
            timeModeGroup.add(radio);
            timeRow[i + 1] = radio;
        }
        form.addRow("", timeRow);

        String pluginsLabel = "Plugins";
        for (Plugin plugin : PluginManager.getPlugins()) {
            JCheckBox plugCheck = new JCheckBox("<html>" + plugin.getName() + "<br/><small>" + plugin.getDescription(), plugin.isActive());
            plugCheck.addActionListener(e -> {
                try {
                    PluginManager.setActive(plugin, plugCheck.isSelected());
                } catch (RuntimeException failure) {
                    Log.error("Failed to change " + plugin.getName(), failure);
                    Message.err("Failed to change " + plugin.getName(), failure.getMessage());
                } finally {
                    plugCheck.setSelected(plugin.isActive());
                }
            });
            form.addRow(pluginsLabel, plugCheck);
            pluginsLabel = "";
        }

        clearCache.addActionListener(e -> {
            clearCache.setEnabled(false);
            Task.submitBackground("clear-cache", () -> {
                JPIPCacheManager.clear();
                return null;
            }, _ -> {
                clearCache.setEnabled(JPIPCacheManager.isAvailable());
                updateCacheSize();
            }, (logContext, t) -> {
                Log.error(logContext, t);
                clearCache.setEnabled(JPIPCacheManager.isAvailable());
                updateCacheSize();
            });
        });
        form.addRow("Image cache", cacheSizeLabel, clearCache);

        password.setText(ProxySettings.getPassword());
        form.addRow("Proxy username", username);
        form.addRow("Proxy password", password);
        return form;
    }

    @Override
    public void showDialog() {
        updateCacheSize();
        super.showDialog();
    }

}
