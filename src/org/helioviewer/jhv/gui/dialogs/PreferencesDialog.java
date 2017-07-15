package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
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
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.io.DataSources;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class PreferencesDialog extends StandardDialog implements ShowableDialog {

    private JCheckBox loadDefaultMovie;
    private JCheckBox normalizeRadius;
    private JCheckBox normalizeAIA;
    private DefaultsSelectionPanel defaultsPanel;

    private final Settings settings = Settings.getSingletonInstance();

    public PreferencesDialog() {
        super(ImageViewerGui.getMainFrame(), "Preferences", true);
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
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    private void saveSettings() {
        settings.setProperty("startup.loadmovie", Boolean.toString(loadDefaultMovie.isSelected()));
        settings.setProperty("display.normalize", Boolean.toString(normalizeRadius.isSelected()));
        settings.setProperty("display.normalizeAIA", Boolean.toString(normalizeAIA.isSelected()));
        defaultsPanel.saveSettings();
        settings.save();
    }

    private JPanel createParametersPanel() {
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        row1.add(new JLabel("Preferred server", JLabel.RIGHT));

        Set<String> servers = DataSources.getServers();
        JComboBox<String> combo = new JComboBox<>(servers.toArray(new String[servers.size()]));
        combo.setSelectedItem(Settings.getSingletonInstance().getProperty("default.server"));
        combo.addActionListener(e -> Settings.getSingletonInstance().setProperty("default.server", (String) combo.getSelectedItem()));
        row1.add(combo);

        loadDefaultMovie = new JCheckBox("Load default movie at start-up", Boolean.parseBoolean(settings.getProperty("startup.loadmovie")));
        row1.add(loadDefaultMovie);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        normalizeAIA = new JCheckBox("Normalize SDO/AIA brightness (needs restart)", Boolean.parseBoolean(settings.getProperty("display.normalizeAIA")));
        row2.add(normalizeAIA);
        normalizeRadius = new JCheckBox("Normalize solar radius (needs restart)", Boolean.parseBoolean(settings.getProperty("display.normalize")));
        row2.add(normalizeRadius);

        JPanel paramsPanel = new JPanel(new GridLayout(0, 1));
        paramsPanel.add(row1);
        paramsPanel.add(row2);

        return paramsPanel;
    }

    private static class DefaultsSelectionPanel extends JPanel {

        final JTable grid;
        final TableModel model;

        DefaultsSelectionPanel() {
            super(new BorderLayout());
            setPreferredSize(new Dimension(0, 150));

            Settings settings = Settings.getSingletonInstance();
            String pass = settings.getProperty("default.proxyPassword");
            try {
                pass = new String(Base64.getDecoder().decode(pass), StandardCharsets.UTF_8);
            } catch (Exception e) {
                pass = null;
            }

            Object[][] tableData = {
                { "Default recording directory", settings.getProperty("default.save.path") },
                { "Default download directory", settings.getProperty("default.local.path") },
                { "Proxy username", settings.getProperty("default.proxyUsername") },
                { "Proxy password", pass },
            };

            model = new DefaultTableModel(tableData, new String[] { "Description", "Value" }) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 1 && (row == 2 || row == 3);
                }
            };

            JPasswordField passField = new JPasswordField();
            DefaultCellEditor passEditor = new DefaultCellEditor(passField);

            grid = new JTable(model) {
                @Override
                public TableCellEditor getCellEditor(int row, int column) {
                    if (row == 3 && column == 1) {
                        if (getValueAt(3, 1) instanceof String)
                            passField.setText((String) getValueAt(3, 1));
                        return passEditor;
                    } else
                        return super.getCellEditor(row, column);
                }
            };
            grid.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer () {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    if (row == 3 && column == 1) {
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

            grid.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 2)
                        return;

                    int row = grid.getSelectedRow();
                    if (row == -1 || row >= 2)
                        return;

                    JFileChooser chooser = new JFileChooser((String) model.getValueAt(row, 1));
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    if (chooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION)
                        model.setValueAt(chooser.getSelectedFile().toString(), row, 1);
                }
            });

            TableColumn col = grid.getColumnModel().getColumn(0);
            col.setMaxWidth(150);
            col.setMinWidth(150);

            add(scrollPane, BorderLayout.CENTER);
        }

        public void saveSettings() {
            Settings settings = Settings.getSingletonInstance();
            if (model.getValueAt(0, 1) instanceof String)
                settings.setProperty("default.save.path", (String) model.getValueAt(0, 1));
            if (model.getValueAt(1, 1) instanceof String)
                settings.setProperty("default.local.path", (String) model.getValueAt(1, 1));
            if (model.getValueAt(2, 1) instanceof String)
                settings.setProperty("default.proxyUsername", (String) model.getValueAt(2, 1));
            if (model.getValueAt(3, 1) instanceof String) {
                String s = Base64.getEncoder().withoutPadding().encodeToString(((String) model.getValueAt(3, 1)).getBytes(StandardCharsets.UTF_8));
                settings.setProperty("default.proxyPassword", s);
            }
        }

    }

}
