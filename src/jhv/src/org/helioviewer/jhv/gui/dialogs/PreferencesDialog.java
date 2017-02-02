package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
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

// Dialog that allows the user to change default preferences and settings
@SuppressWarnings("serial")
public class PreferencesDialog extends JDialog implements ShowableDialog {

    private JCheckBox loadDefaultMovie;
    private JCheckBox normalizeRadius;
    private JCheckBox normalizeAIA;
    private DefaultsSelectionPanel defaultsPanel;

    private final Settings settings = Settings.getSingletonInstance();

    public PreferencesDialog() {
        super(ImageViewerGui.getMainFrame(), "Preferences", true);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new BorderLayout());

        JPanel paramsSubPanel = new JPanel(new BorderLayout());
        paramsSubPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 3));
        paramsSubPanel.add(createParametersPanel(), BorderLayout.CENTER);

        JPanel defaultsSubPanel = new JPanel(new BorderLayout());
        defaultsSubPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 3));
        defaultsSubPanel.add(createDefaultSaveDirPanel(), BorderLayout.CENTER);

        panel.add(paramsSubPanel, BorderLayout.NORTH);
        panel.add(defaultsSubPanel, BorderLayout.CENTER);

        mainPanel.add(panel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton acceptBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        acceptBtn.addActionListener(e -> {
            saveSettings();
            setVisible(false);
        });

        cancelBtn.addActionListener(e -> setVisible(false));

        if (System.getProperty("jhv.os").equals("windows")) {
            btnPanel.add(acceptBtn);
            btnPanel.add(cancelBtn);
        } else {
            btnPanel.add(cancelBtn);
            btnPanel.add(acceptBtn);
        }

        mainPanel.add(btnPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);

        getRootPane().registerKeyboardAction(e -> setVisible(false), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(acceptBtn);
        getRootPane().setFocusable(true);
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

    /**
     * Creates the general parameters panel.
     *
     * @return General parameters panel
     */
    private JPanel createParametersPanel() {
        JPanel paramsPanel = new JPanel(new GridLayout(0, 1));
        paramsPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        row1.add(new JLabel("Preferred server", JLabel.RIGHT));

        JComboBox<String> combo = new JComboBox<>(DataSources.getServers());
        combo.setSelectedItem(Settings.getSingletonInstance().getProperty("default.server"));
        combo.addActionListener(e -> Settings.getSingletonInstance().setProperty("default.server", (String) combo.getSelectedItem()));
        row1.add(combo);
        loadDefaultMovie = new JCheckBox("Load default movie at start-up", Boolean.parseBoolean(settings.getProperty("startup.loadmovie")));
        row1.add(loadDefaultMovie);
        paramsPanel.add(row1);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        normalizeAIA = new JCheckBox("Normalize SDO/AIA brightness (needs restart)", Boolean.parseBoolean(settings.getProperty("display.normalizeAIA")));
        row2.add(normalizeAIA);
        normalizeRadius = new JCheckBox("Normalize solar radius (needs restart)", Boolean.parseBoolean(settings.getProperty("display.normalize")));
        row2.add(normalizeRadius);
        paramsPanel.add(row2);

        return paramsPanel;
    }

    /**
     * Creates the default save directories panel.
     *
     * @return Default save directories panel
     */
    private JPanel createDefaultSaveDirPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(" Settings "));

        defaultsPanel = new DefaultsSelectionPanel();
        defaultsPanel.setPreferredSize(new Dimension(450, 150));
        defaultsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panel.add(defaultsPanel, BorderLayout.CENTER);

        return panel;
    }

    private static class DefaultsSelectionPanel extends JPanel {

        private final JTable table;
        private final TableModel model;

        public DefaultsSelectionPanel() {
            super(new BorderLayout());
            // setPreferredSize(new Dimension(150, 180));

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

            table = new JTable(model) {
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
            table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer () {
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

            table.setRowHeight(20);
            JScrollPane scrollPane = new JScrollPane(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // table.setFillsViewportHeight(true);

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 2)
                        return;

                    int row = table.getSelectedRow();
                    if (row >= 2)
                        return;

                    JFileChooser chooser = new JFileChooser((String) model.getValueAt(row, 1));
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    if (chooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION)
                        model.setValueAt(chooser.getSelectedFile().toString(), row, 1);
                }
            });

            TableColumn col = table.getColumnModel().getColumn(0);
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

    @Override
    public void init() {
    }

    public static void dialogShow() {
        PreferencesDialog dialog = new PreferencesDialog();
        dialog.init();
        dialog.showDialog();
    }

}
