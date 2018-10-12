package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.view.j2k.io.jpip.JPIPCacheManager;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class PreferencesDialog extends StandardDialog implements ShowableDialog {

    private final JLabel labelCache = new JLabel("The image cache currently uses 0.0GB on disk.", JLabel.RIGHT);

    private void setLabelCache() {
        labelCache.setText(String.format("The image cache currently uses %.1fGB on disk.", JPIPCacheManager.getSize() / (1024 * 1024 * 1024.)));
    }

    private JCheckBox defaultMovie;
    private JCheckBox sampHub;
    private JCheckBox normalizeRadius;
    private JCheckBox normalizeAIA;
    private DefaultsSelectionPanel defaultsPanel;

    public PreferencesDialog() {
        super(JHVFrame.getFrame(), "Preferences", true);
        setResizable(false);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultCancelAction(close);

        JButton cancelBtn = new JButton(close);
        cancelBtn.setText("Cancel");

        AbstractAction save = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
                setVisible(false);
            }
        };
        setDefaultAction(save);

        JButton okBtn = new JButton(save);
        okBtn.setText("Save");
        setInitFocusedComponent(okBtn);

        ButtonPanel panel = new ButtonPanel();
        panel.add(okBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelBtn, ButtonPanel.CANCEL_BUTTON);

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

    private void saveSettings() {
        Settings.setProperty("startup.loadmovie", Boolean.toString(defaultMovie.isSelected()));
        Settings.setProperty("startup.sampHub", Boolean.toString(sampHub.isSelected()));
        Settings.setProperty("display.normalize", Boolean.toString(normalizeRadius.isSelected()));
        Settings.setProperty("display.normalizeAIA", Boolean.toString(normalizeAIA.isSelected()));
        defaultsPanel.setSettings();
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
        JComboBox<String> combo = new JComboBox<>(DataSources.getServers().toArray(new String[0]));
        combo.setSelectedItem(Settings.getProperty("default.server"));
        combo.addActionListener(e -> Settings.setProperty("default.server", (String) Objects.requireNonNull(combo.getSelectedItem())));
        settings.add(combo, c);

        c.gridx = 0;
        c.gridy = 1;
        settings.add(new JLabel("At start-up:", JLabel.RIGHT), c);

        c.gridx = 1;
        c.gridy = 1;
        defaultMovie = new JCheckBox("Load default movie", Boolean.parseBoolean(Settings.getProperty("startup.loadmovie")));
        settings.add(defaultMovie, c);

        c.gridx = 1;
        c.gridy = 2;
        sampHub = new JCheckBox("Load SAMP hub", Boolean.parseBoolean(Settings.getProperty("startup.sampHub")));
        settings.add(sampHub, c);

        c.gridx = 0;
        c.gridy = 3;
        settings.add(new JLabel("Normalize (after restart):", JLabel.RIGHT), c);

        c.gridx = 1;
        c.gridy = 3;
        normalizeRadius = new JCheckBox("Solar radius", Boolean.parseBoolean(Settings.getProperty("display.normalize")));
        settings.add(normalizeRadius, c);

        c.gridx = 1;
        c.gridy = 4;
        normalizeAIA = new JCheckBox("SDO/AIA brightness", Boolean.parseBoolean(Settings.getProperty("display.normalizeAIA")));
        settings.add(normalizeAIA, c);

        JPanel cache = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JButton clearCache = new JButton("Clear Cache");
        clearCache.addActionListener(e -> {
            JPIPCacheManager.clear();
            setLabelCache();
        });
        cache.add(labelCache);
        cache.add(clearCache);

        JPanel paramsPanel = new JPanel(new BorderLayout());
        paramsPanel.add(settings, BorderLayout.PAGE_START);
        paramsPanel.add(cache, BorderLayout.PAGE_END);

        return paramsPanel;
    }

    private static class DefaultsSelectionPanel extends JPanel {

        final JTable grid;
        final TableModel model;

        DefaultsSelectionPanel() {
            super(new BorderLayout());
            setPreferredSize(new Dimension(0, 150));

            String pass = Settings.getProperty("proxy.password");
            try {
                pass = new String(Base64.getDecoder().decode(pass), StandardCharsets.UTF_8);
            } catch (Exception e) {
                pass = null;
            }

            Object[][] tableData = {
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
            DefaultCellEditor passEditor = new DefaultCellEditor(passField);

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

        void setSettings() {
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
