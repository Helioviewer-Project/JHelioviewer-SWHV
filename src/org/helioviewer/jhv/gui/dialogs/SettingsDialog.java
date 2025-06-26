package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.DisplaySettings;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.export.VideoFormat;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public final class SettingsDialog extends StandardDialog implements Interfaces.ShowableDialog {

    private final JLabel labelCache = new JLabel("The image cache currently uses 0.0GB on disk.", JLabel.RIGHT);

    private void setLabelCache() {
        labelCache.setText(String.format("The image cache currently uses %.1fGB on disk.", JPIPCacheManager.getSize() / (1024 * 1024 * 1024.)));
    }

    private DefaultsSelectionPanel defaultsPanel;

    public SettingsDialog() {
        super(JHVFrame.getFrame(), "Settings", true);
        setResizable(false);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                defaultsPanel.saveSettings();
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
        defaultsPanel = new DefaultsSelectionPanel();
        defaultsPanel.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        return defaultsPanel;
    }

    @Override
    public JComponent createBannerPanel() {
        JPanel panel = createParametersPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        return panel;
    }

    @Override
    public void showDialog() {
        setLabelCache();
        pack();
        setLocationRelativeTo(JHVFrame.getFrame());
        setVisible(true);
    }

    private JPanel createParametersPanel() {
        JPanel settings = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 2, 0, 2);

        c.gridx = 0;
        c.gridy = 0;
        settings.add(new JLabel("At start-up:", JLabel.RIGHT), c);

        c.gridx = 1;
        c.gridy = 0;
        settings.add(getStatePanel(), c);

        c.gridx = 1;
        c.gridy = 1;
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
        c.gridy = 2;
        settings.add(themePanel, c);

        c.gridx = 0;
        c.gridy = 3;
        settings.add(new JLabel("For new layers:", JLabel.RIGHT), c);

        c.gridx = 1;
        c.gridy = 3;
        JCheckBox normalizeAIA = new JCheckBox("Normalize SDO/AIA brightness", DisplaySettings.getNormalizeAIA());
        normalizeAIA.addActionListener(e -> DisplaySettings.setNormalizeAIA(normalizeAIA.isSelected()));
        settings.add(normalizeAIA, c);

        c.gridx = 1;
        c.gridy = 4;
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
        c.gridy = 5;
        settings.add(timePanel, c);

        c.gridx = 0;
        c.gridy = 6;
        settings.add(new JLabel("Record video as:", JLabel.RIGHT), c);

        c.gridx = 1;
        c.gridy = 6;
        JComboBox<VideoFormat> comboVideo = new JComboBox<>(VideoFormat.values());
        VideoFormat videoFormat = VideoFormat.H264;
        try {
            videoFormat = VideoFormat.valueOf(Settings.getProperty("video.format"));
        } catch (Exception ignore) {
        }
        comboVideo.setSelectedItem(videoFormat);
        comboVideo.addActionListener(e -> Settings.setProperty("video.format", ((VideoFormat) Objects.requireNonNull(comboVideo.getSelectedItem())).name()));
        settings.add(comboVideo, c);

        c.gridx = 0;
        c.gridy = 7;
        settings.add(new JLabel("Plugins:", JLabel.RIGHT), c);

        c.gridx = 1;
        for (Plugin plugin : PluginManager.getPlugins()) {
            JCheckBox plugCheck = new JCheckBox("<html>" + plugin.getName() + "<br/><small>" + plugin.getDescription(), plugin.isActive());
            plugCheck.addActionListener(e -> plugin.toggleActive());
            settings.add(plugCheck, c);
            c.gridy++;
        }

        JPanel cache = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JButton clearCache = new JButton("Clear Cache");
        clearCache.addActionListener(e -> {
            try {
                JPIPCacheManager.clear();
                setLabelCache();
            } catch (Exception ex) {
                Log.error("JPIP cache clear error", ex);
            }
        });
        cache.add(labelCache);
        cache.add(clearCache);

        JPanel paramsPanel = new JPanel(new BorderLayout());
        paramsPanel.add(settings, BorderLayout.PAGE_START);
        paramsPanel.add(cache, BorderLayout.PAGE_END);

        return paramsPanel;
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

    private static class DefaultsSelectionPanel extends JPanel {

        final JTable grid;
        final TableModel model;

        DefaultsSelectionPanel() {
            super(new BorderLayout());
            setPreferredSize(new Dimension(-1, 150));

            String pass = Settings.getProperty("proxy.password");
            try {
                pass = new String(Base64.getDecoder().decode(pass), StandardCharsets.UTF_8);
            } catch (Exception e) {
                pass = null;
            }

            String[][] tableData = {
                    {"Proxy username", Settings.getProperty("proxy.username")},
                    {"Proxy password", pass},
            };

            model = new DefaultTableModel(tableData, new String[]{"Description", "Value"}) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 1;
                }
            };

            JPasswordField passField = new JPasswordField();
            TableCellEditor passEditor = new DefaultCellEditor(passField);

            grid = new JTable(model) {
                @Override
                public TableCellEditor getCellEditor(int row, int column) {
                    if (row == 1 && column == 1) {
                        Object val = getValueAt(1, 1);
                        if (val instanceof String)
                            passField.setText((String) val);
                        return passEditor;
                    } else
                        return super.getCellEditor(row, column);
                }
            };
            grid.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    if (row == 1 && column == 1) {
                        if (value instanceof String)
                            passField.setText((String) value);
                        return passField;
                    }
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            });

            grid.setRowHeight(20);
            JScrollPane scrollPane = new JScrollPane(grid);
            grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // grid.setFillsViewportHeight(true);

            TableColumn col = grid.getColumnModel().getColumn(0);
            col.setMaxWidth(150);
            col.setMinWidth(150);

            add(scrollPane, BorderLayout.CENTER);
        }

        void saveSettings() {
            Object val0 = model.getValueAt(0, 1);
            if (val0 instanceof String)
                Settings.setProperty("proxy.username", (String) val0);
            Object val1 = model.getValueAt(1, 1);
            if (val1 instanceof String) {
                String s = Base64.getEncoder().withoutPadding().encodeToString(((String) val1).getBytes(StandardCharsets.UTF_8));
                Settings.setProperty("proxy.password", s);
            }
        }

    }

}
