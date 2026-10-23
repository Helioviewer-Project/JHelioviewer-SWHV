package org.helioviewer.jhv.gui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.helioviewer.jhv.app.DisplaySettings;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.source.jpip.JPIPCacheManager;
import org.helioviewer.jhv.thread.Task;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public final class SettingsDialog extends StandardDialog {

    private final JLabel cacheSizeLabel = new JLabel(cacheSizeText("--.-"), JLabel.RIGHT);
    private final JButton clearCache = new JButton("Clear Cache");

    private void updateCacheSize() {
        if (!JPIPCacheManager.isAvailable()) {
            clearCache.setEnabled(false);
            cacheSizeLabel.setText("JPIP disk cache is unavailable.");
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
        return "The image cache currently uses " + size + "GB on disk.";
    }

    private ProxyPanel proxyPanel;

    public SettingsDialog() {
        super(MainFrame.get(), "Settings", true);
        setResizable(false);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                proxyPanel.saveSettings();
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
        proxyPanel = new ProxyPanel();
        proxyPanel.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        return proxyPanel;
    }

    @Override
    public JComponent createBannerPanel() {
        JPanel panel = createParametersPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        return panel;
    }

    public void showDialog() {
        updateCacheSize();
        pack();
        setLocationRelativeTo(MainFrame.get());
        setVisible(true);
    }

    private JPanel createParametersPanel() {
        JPanel settings = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 2, 0, 2);

        c.gridx = 0;
        c.gridy = 0;
        settings.add(new JLabel("Preferred server:", JLabel.RIGHT), c);

        c.gridx = 1;
        c.gridy = 0;
        JComboBox<String> comboServer = new JComboBox<>(DataSources.getServers().toArray(new String[0]));
        comboServer.setSelectedItem(Settings.getProperty("dataSources.defaultServer"));
        comboServer.addActionListener(e -> Settings.setProperty("dataSources.defaultServer", (String) Objects.requireNonNull(comboServer.getSelectedItem())));
        settings.add(comboServer, c);

        c.gridx = 0;
        c.gridy = 1;
        settings.add(new JLabel("At start-up:", JLabel.RIGHT), c);

        c.gridx = 1;
        c.gridy = 1;
        settings.add(getStatePanel(), c);

        c.gridx = 1;
        c.gridy = 2;
        JCheckBox sampHub = new JCheckBox("Load SAMP hub", Boolean.parseBoolean(Settings.getProperty("startup.sampHub")));
        sampHub.addActionListener(e -> Settings.setProperty("startup.sampHub", Boolean.toString(sampHub.isSelected())));
        settings.add(sampHub, c);

        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        themePanel.add(new JLabel("Use theme ", JLabel.RIGHT));
        ButtonGroup themeGroup = new ButtonGroup();
        DisplaySettings.UITheme currentTheme = DisplaySettings.getUITheme();
        for (DisplaySettings.UITheme theme : DisplaySettings.UITheme.values()) {
            JRadioButton radio = new JRadioButton(theme.toString(), theme == currentTheme);
            radio.addItemListener(e -> {
                if (radio.isSelected()) {
                    DisplaySettings.setUITheme(theme);
                }
            });
            themePanel.add(radio);
            themeGroup.add(radio);
        }

        c.gridx = 1;
        c.gridy = 3;
        settings.add(themePanel, c);

        c.gridx = 0;
        c.gridy = 4;
        settings.add(new JLabel("For new layers:", JLabel.RIGHT), c);

        c.gridx = 1;
        c.gridy = 4;
        JCheckBox normalizeAIA = new JCheckBox("Normalize SDO/AIA brightness", DisplaySettings.getNormalizeAIA());
        normalizeAIA.addActionListener(e -> DisplaySettings.setNormalizeAIA(normalizeAIA.isSelected()));
        settings.add(normalizeAIA, c);

        c.gridx = 1;
        c.gridy = 5;
        JCheckBox normalizeRadius = new JCheckBox("Normalize solar radius", DisplaySettings.getNormalizeRadius());
        normalizeRadius.addActionListener(e -> DisplaySettings.setNormalizeRadius(normalizeRadius.isSelected()));
        settings.add(normalizeRadius, c);

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        timePanel.add(new JLabel("Use time at ", JLabel.RIGHT));
        ButtonGroup timeModeGroup = new ButtonGroup();
        DisplaySettings.TimeMode timeMode = DisplaySettings.getTimeMode();
        for (DisplaySettings.TimeMode mode : DisplaySettings.TimeMode.values()) {
            JRadioButton radio = new JRadioButton(mode.toString(), mode == timeMode);
            radio.addItemListener(e -> {
                if (radio.isSelected())
                    DisplaySettings.setTimeMode(mode);
            });
            timePanel.add(radio);
            timeModeGroup.add(radio);
        }

        c.gridx = 1;
        c.gridy = 6;
        settings.add(timePanel, c);

        c.gridx = 0;
        c.gridy = 7;
        settings.add(new JLabel("Plugins:", JLabel.RIGHT), c);

        c.gridx = 1;
        for (Plugin plugin : PluginManager.getPlugins()) {
            JCheckBox plugCheck = new JCheckBox("<html>" + plugin.getName() + "<br/><small>" + plugin.getDescription(), plugin.isActive());
            plugCheck.addActionListener(e -> PluginManager.setActive(plugin, plugCheck.isSelected()));
            settings.add(plugCheck, c);
            c.gridy++;
        }

        JPanel paramsPanel = new JPanel(new BorderLayout());
        paramsPanel.add(settings, BorderLayout.PAGE_START);
        paramsPanel.add(createCachePanel(), BorderLayout.PAGE_END);

        return paramsPanel;
    }

    private JPanel createCachePanel() {
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

        JPanel cache = new JPanel(new FlowLayout(FlowLayout.LEADING));
        cache.add(cacheSizeLabel);
        cache.add(clearCache);
        return cache;
    }

    private static JPanel getStatePanel() {
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

        JPanel statePanel = new JPanel(new BorderLayout());
        statePanel.add(defaultState, BorderLayout.LINE_START);
        statePanel.add(selectState, BorderLayout.LINE_END);
        return statePanel;
    }

    private static class ProxyPanel extends JPanel {

        private final JTextField username = new JTextField(Settings.getProperty("proxy.username"), 20);
        private final JPasswordField password = new JPasswordField(20);

        ProxyPanel() {
            super(new GridBagLayout());
            password.setText(ProxySettings.getPassword());

            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.LINE_END;
            c.insets = new Insets(0, 2, 0, 2);
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("Proxy username:"), c);
            c.gridy = 1;
            add(new JLabel("Proxy password:"), c);

            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            add(username, c);
            c.gridy = 1;
            add(password, c);
        }

        void saveSettings() {
            Settings.setProperty("proxy.username", username.getText());
            ProxySettings.setPassword(new String(password.getPassword()));
        }

    }

}
