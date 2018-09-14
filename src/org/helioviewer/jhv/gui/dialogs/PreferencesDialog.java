package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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

    private final JLabel labelCache = new JLabel("The image cache currently uses 0.0GB on disk.");
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
        labelCache.setText(String.format("The image cache currently uses %.1fGB on disk.", JPIPCacheManager.getSize() / (1024 * 1024 * 1024.)));
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
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        row1.add(new JLabel("Preferred server", JLabel.RIGHT));

        JComboBox<String> combo = new JComboBox<>(DataSources.getServers().toArray(new String[0]));
        combo.setSelectedItem(Settings.getProperty("default.server"));
        combo.addActionListener(e -> Settings.setProperty("default.server", (String) Objects.requireNonNull(combo.getSelectedItem())));
        row1.add(combo);

        defaultMovie = new JCheckBox("Load default movie at start-up", Boolean.parseBoolean(Settings.getProperty("startup.loadmovie")));
        row1.add(defaultMovie);

        sampHub = new JCheckBox("Start SAMP hub", Boolean.parseBoolean(Settings.getProperty("startup.sampHub")));
        row1.add(sampHub);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        normalizeAIA = new JCheckBox("Normalize SDO/AIA brightness (needs restart)", Boolean.parseBoolean(Settings.getProperty("display.normalizeAIA")));
        row2.add(normalizeAIA);
        normalizeRadius = new JCheckBox("Normalize solar radius (needs restart)", Boolean.parseBoolean(Settings.getProperty("display.normalize")));
        row2.add(normalizeRadius);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JButton clearCache = new JButton("Clear Cache");
        clearCache.addActionListener(e -> {
            JPIPCacheManager.clear();
            setVisible(false);
        });
        row3.add(labelCache);
        row3.add(clearCache);

        JPanel paramsPanel = new JPanel(new GridLayout(0, 1));
        paramsPanel.add(row1);
        paramsPanel.add(row2);
        paramsPanel.add(row3);

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
