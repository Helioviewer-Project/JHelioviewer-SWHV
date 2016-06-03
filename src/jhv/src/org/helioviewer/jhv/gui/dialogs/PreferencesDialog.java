package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.io.DataSources;

/**
 * Dialog that allows the user to change default preferences and settings.
 */
@SuppressWarnings("serial")
public class PreferencesDialog extends JDialog implements ShowableDialog {

    private JCheckBox loadDefaultMovie;
    private JCheckBox normalizeRadius;
    private DefaultsSelectionPanel defaultsPanel;

    private final JButton acceptBtn;

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
        acceptBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        acceptBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
                dispose();
            }
        });

        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        if (System.getProperty("jhv.os").equals("windows")) {
            btnPanel.add(acceptBtn);
            btnPanel.add(cancelBtn);
        } else {
            btnPanel.add(cancelBtn);
            btnPanel.add(acceptBtn);
        }

        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);
        getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());

        getRootPane().setDefaultButton(acceptBtn);
        setVisible(true);
    }

    /**
     * Saves the settings.
     *
     * Writes the informations to {@link org.helioviewer.jhv.Settings}.
     */
    private void saveSettings() {
        settings.setProperty("startup.loadmovie", Boolean.toString(loadDefaultMovie.isSelected()));
        settings.setProperty("display.normalize", Boolean.toString(normalizeRadius.isSelected()));

        defaultsPanel.saveSettings();
        settings.save();
    }

    /**
     * Creates the general parameters panel.
     *
     * @return General parameters panel
     */
    private JPanel createParametersPanel() {
        JPanel paramsPanel = new JPanel();
        paramsPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        paramsPanel.setLayout(new GridLayout(0, 1));

        JPanel row_1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        row_1.add(new JLabel("Default server", JLabel.RIGHT));
        row_1.add(DataSources.getServerComboBox());
        paramsPanel.add(row_1);

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEADING));

        loadDefaultMovie = new JCheckBox("Load default movie at start-up", Boolean.parseBoolean(settings.getProperty("startup.loadmovie")));
        row0.add(loadDefaultMovie);

        normalizeRadius = new JCheckBox("Normalize solar radius (needs restart)", Boolean.parseBoolean(settings.getProperty("display.normalize")));
        row0.add(normalizeRadius);

        paramsPanel.add(row0);

        return paramsPanel;
    }

    /**
     * Creates the default save directories panel.
     *
     * @return Default save directories panel
     */
    private JPanel createDefaultSaveDirPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(" Locations "));

        defaultsPanel = new DefaultsSelectionPanel();
        defaultsPanel.loadSettings();
        defaultsPanel.setPreferredSize(new Dimension(450, 100));
        defaultsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panel.add(defaultsPanel, BorderLayout.CENTER);

        return panel;
    }

    private static class DefaultsSelectionPanel extends JPanel {

        private final JTable table;

        public DefaultsSelectionPanel() {
            super(new BorderLayout());
            // setPreferredSize(new Dimension(150, 180));

            Settings settings = Settings.getSingletonInstance();

            Object[][] tableData = new Object[][] { { "Default recording directory", settings.getProperty("default.save.path") },
                                                    { "Default download path", settings.getProperty("default.local.path") } };

            table = new JTable(new DefaultTableModel(tableData, new String[] { "Description", "Value" }) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
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

                    JFileChooser chooser = new JFileChooser((String) table.getModel().getValueAt(row, 1));
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    if (chooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION)
                        table.getModel().setValueAt(chooser.getSelectedFile().toString(), row, 1);
                }
            });

            TableColumn col = table.getColumnModel().getColumn(0);
            col.setMaxWidth(150);
            col.setMinWidth(150);

            add(scrollPane, BorderLayout.CENTER);
        }

        public void loadSettings() {
            TableModel model = table.getModel();
            Settings settings = Settings.getSingletonInstance();
            model.setValueAt(settings.getProperty("default.save.path"), 0, 1);
            model.setValueAt(settings.getProperty("default.local.path"), 1, 1);
        }

        public void saveSettings() {
            TableModel model = table.getModel();
            Settings settings = Settings.getSingletonInstance();
            settings.setProperty("default.save.path", model.getValueAt(0, 1).toString());
            settings.setProperty("default.local.path", model.getValueAt(1, 1).toString());
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
