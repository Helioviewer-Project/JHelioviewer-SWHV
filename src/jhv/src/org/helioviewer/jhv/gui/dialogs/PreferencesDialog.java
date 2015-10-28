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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.log4j.Level;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.logging.LogSettings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.observation.ServerListCombo;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

/**
 * Dialog that allows the user to change default preferences and settings.
 *
 * @author Desmond Amadigwe
 * @author Benjamin Wamsler
 * @author Juan Pablo
 * @author Markus Langenberg
 * @author Andre Dau
 */
@SuppressWarnings("serial")
public class PreferencesDialog extends JDialog implements ShowableDialog {

    private JRadioButton loadDefaultMovieOnStartUp;
    private JRadioButton doNothingOnStartUp;
    private JPanel paramsPanel;
    private JComboBox debugFileCombo = null;
    private JComboBox debugConsoleCombo = null;
    private JTextField debugFileTextField = null;
    private DefaultsSelectionPanel defaultsPanel;

    private final Settings settings = Settings.getSingletonInstance();

    private final JButton acceptBtn;

    /**
     * The private constructor that sets the fields and the dialog.
     */
    public PreferencesDialog() {
        super(ImageViewerGui.getMainFrame(), "Preferences", true);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel paramsSubPanel = new JPanel(new BorderLayout());
        paramsSubPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        paramsSubPanel.add(createParametersPanel(), BorderLayout.CENTER);

        JPanel defaultsSubPanel = new JPanel(new BorderLayout());
        defaultsSubPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        defaultsSubPanel.add(createDefaultSaveDirPanel(), BorderLayout.CENTER);

        JPanel jpipSupPanel = new JPanel(new BorderLayout());
        jpipSupPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        panel.add(paramsSubPanel, BorderLayout.NORTH);
        panel.add(defaultsSubPanel, BorderLayout.CENTER);
        panel.add(jpipSupPanel, BorderLayout.SOUTH);

        mainPanel.add(panel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        acceptBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        JButton resetBtn = new JButton("Reset");

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

        resetBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(null, "Do you really want to reset the setting values?", "Attention", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    defaultsPanel.resetSettings();
                    loadDefaultMovieOnStartUp.setSelected(true);

                    LogSettings logSettings = LogSettings.getSingletonInstance();

                    if (debugFileCombo != null) {
                        debugFileCombo.setSelectedItem(logSettings.getDefaultLoggingLevel("file"));
                        debugFileTextField.setText(Integer.toString(logSettings.getDefaultMaximumLogFileAge("file")));
                    }

                    if (debugConsoleCombo != null) {
                        debugConsoleCombo.setSelectedItem(logSettings.getDefaultLoggingLevel("console"));
                    }
                }
            }
        });

        if (System.getProperty("os.name").toUpperCase().contains("WIN")) {
            btnPanel.add(acceptBtn);
            btnPanel.add(resetBtn);
            btnPanel.add(cancelBtn);
        } else {
            btnPanel.add(resetBtn);
            btnPanel.add(cancelBtn);
            btnPanel.add(acceptBtn);
        }

        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);
        getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelPressed();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

    }

    private void cancelPressed() {
        dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDialog() {
        loadSettings();

        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        getRootPane().setDefaultButton(acceptBtn);

        pack();
        setVisible(true);
    }

    /**
     * Loads the settings.
     *
     * Reads the informations from {@link org.helioviewer.jhv.Settings} and sets
     * all gui elements according to them.
     */
    private void loadSettings() {
        // In principle the settings have been previously loaded
        // settings.load();
        // Start up
        loadDefaultMovieOnStartUp.setSelected(Boolean.parseBoolean(settings.getProperty("startup.loadmovie")));
        doNothingOnStartUp.setSelected(!Boolean.parseBoolean(settings.getProperty("startup.loadmovie")));

        // Debug options
        LogSettings logSettings = LogSettings.getSingletonInstance();
        if (debugFileCombo != null) {
            debugFileCombo.setSelectedItem(logSettings.getLoggingLevel("file"));
            debugFileTextField.setText(Integer.toString(logSettings.getMaximumLogFileAge("file")));
        }
        if (debugConsoleCombo != null) {
            debugConsoleCombo.setSelectedItem(logSettings.getLoggingLevel("console"));
        }

        // Default values
        defaultsPanel.loadSettings();
    }

    /**
     * Saves the settings.
     *
     * Writes the informations to {@link org.helioviewer.jhv.Settings}.
     */
    private void saveSettings() {
        // Start up
        settings.setProperty("startup.loadmovie", Boolean.toString(loadDefaultMovieOnStartUp.isSelected()));
        // Debug options
        LogSettings logSettings = LogSettings.getSingletonInstance();
        if (debugFileCombo != null) {
            Level level = (Level) debugFileCombo.getSelectedItem();
            logSettings.setLoggingLevel("file", level);
            logSettings.setMaximumLogFileAge(LogSettings.FILE_LOGGER, Integer.parseInt(debugFileTextField.getText()));
        }
        if (debugConsoleCombo != null) {
            Level level = (Level) debugConsoleCombo.getSelectedItem();
            logSettings.setLoggingLevel("console", level);
        }

        // Default values
        defaultsPanel.saveSettings();
        settings.save();
        LogSettings.getSingletonInstance().update();
    }

    /**
     * Creates the general parameters panel.
     *
     * @return General parameters panel
     */
    private JPanel createParametersPanel() {
        paramsPanel = new JPanel();

        paramsPanel.setBorder(BorderFactory.createTitledBorder(" Configuration "));
        paramsPanel.setLayout(new GridLayout(0, 1));

        JPanel row_1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        row_1.add(new JLabel("Default server", JLabel.RIGHT));
        row_1.add(new ServerListCombo());
        paramsPanel.add(row_1);

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        row0.add(new JLabel("At start-up"));

        loadDefaultMovieOnStartUp = new JRadioButton("Load default movie", true);
        doNothingOnStartUp = new JRadioButton("Do nothing", false);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(loadDefaultMovieOnStartUp);
        buttonGroup.add(doNothingOnStartUp);

        row0.add(loadDefaultMovieOnStartUp);
        row0.add(doNothingOnStartUp);
        paramsPanel.add(row0);

        LogSettings logSettings = LogSettings.getSingletonInstance();
        Level fileLoggingLevel = logSettings.getLoggingLevel(LogSettings.FILE_LOGGER);
        Level consoleLoggingLevel = logSettings.getLoggingLevel(LogSettings.CONSOLE_LOGGER);

        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        if (fileLoggingLevel != null) {
            row4.add(new JLabel("File log level", JLabel.RIGHT));
            debugFileCombo = new JComboBox(LogSettings.LEVELS);
            row4.add(debugFileCombo);
            debugFileCombo.setSelectedItem(fileLoggingLevel);
        }

        if (consoleLoggingLevel != null) {
            row4.add(new JLabel("Console log level", JLabel.RIGHT));
            debugConsoleCombo = new JComboBox(LogSettings.LEVELS);
            row4.add(debugConsoleCombo);
            debugConsoleCombo.setSelectedItem(consoleLoggingLevel);
        }

        if (fileLoggingLevel != null || consoleLoggingLevel != null) {
            paramsPanel.add(row4);
        }

        JPanel row5 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        if (fileLoggingLevel != null) {
            row5.add(new JLabel("Delete log files after"));
            debugFileTextField = new JTextField(3);
            debugFileTextField.setText(Integer.toString(logSettings.getMaximumLogFileAge(LogSettings.FILE_LOGGER)));
            row5.add(debugFileTextField);
            row5.add(new JLabel("days (enter 0 to keep all files)"));
            paramsPanel.add(row5);
        }

        return paramsPanel;
    }

    /**
     * Creates the default save directories panel.
     *
     * @return Default save directories panel
     */
    private JPanel createDefaultSaveDirPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(" Defaults "));

        defaultsPanel = new DefaultsSelectionPanel();
        defaultsPanel.setPreferredSize(new Dimension(450, 100));
        defaultsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panel.add(defaultsPanel, BorderLayout.CENTER);

        return panel;
    }

    private class DefaultsSelectionPanel extends JPanel {

        private JTable table = null;
        private Object[][] tableData = null;

        public DefaultsSelectionPanel() {
            super(new BorderLayout());
            // setPreferredSize(new Dimension(150, 180));

            Settings settings = Settings.getSingletonInstance();

            tableData = new Object[][] { { "Default save directory", settings.getProperty("default.save.path") },
                                         { "Default local path", settings.getProperty("default.local.path") } };

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

        public void resetSettings() {
            TableModel model = table.getModel();
            model.setValueAt(JHVDirectory.EXPORTS.getPath(), 0, 1);
            model.setValueAt(JHVDirectory.HOME.getPath(), 1, 1);
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
