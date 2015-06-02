package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.log4j.Level;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;

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
    private final JCheckBox limitMaxSize = new JCheckBox("Limit size");
    private final JLabel occupiedSizeLabel = new JLabel();
    private final JTextField maxCacheBox = new JTextField("0.0");
    private final JLabel maxCacheBoxLabel = new JLabel("Mbytes");
    private JComboBox debugFileCombo = null;
    private JComboBox debugConsoleCombo = null;
    private JTextField debugFileTextField = null;
    private DefaultsSelectionPanel defaultsPanel;

    private final Settings settings = Settings.getSingletonInstance();

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
        jpipSupPanel.add(createJPIPCachePanel(), BorderLayout.CENTER);

        panel.add(paramsSubPanel, BorderLayout.NORTH);
        panel.add(defaultsSubPanel, BorderLayout.CENTER);
        panel.add(jpipSupPanel, BorderLayout.SOUTH);

        mainPanel.add(panel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton acceptBtn = new JButton("Accept");
        JButton cancelBtn = new JButton("Cancel");
        JButton resetBtn = new JButton("Reset");

        acceptBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (limitMaxSize.isSelected() && !(Double.parseDouble(maxCacheBox.getText()) > 0.0)) {
                        Message.err("Invalid value", "The value for the maximal cache size must be greater than 0.", false);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    Message.err("Invalid value", "The value for the maximal cache size is not a number.", false);
                    return;
                }

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
                    maxCacheBox.setText("0.0");

                    LogSettings logSettings = LogSettings.getSingletonInstance();

                    if (debugFileCombo != null) {
                        debugFileCombo.setSelectedItem(logSettings.getDefaultLoggingLevel("file"));
                        debugFileTextField.setText(Integer.toString(logSettings.getDefaultMaxiumLogFileAge("file")));
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
        pack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDialog() {
        loadSettings();

        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
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
        // The current cache size
        occupiedSizeLabel.setText(getCacheSizeText());
        // Debug options
        LogSettings logSettings = LogSettings.getSingletonInstance();
        if (debugFileCombo != null) {
            debugFileCombo.setSelectedItem(logSettings.getLoggingLevel("file"));
            debugFileTextField.setText(Integer.toString(logSettings.getMaxiumLogFileAge("file")));
        }
        if (debugConsoleCombo != null) {
            debugConsoleCombo.setSelectedItem(logSettings.getLoggingLevel("console"));
        }

        // Default values
        defaultsPanel.loadSettings();
        // Maximum JPIP cache size
        maxCacheBox.setText(settings.getProperty("jpip.cache.size"));

        try {
            limitMaxSize.setSelected(Double.parseDouble(maxCacheBox.getText()) > 0);
        } catch (NumberFormatException e) {
            maxCacheBox.setText("0.0");
            limitMaxSize.setSelected(false);
        }

        maxCacheBox.setVisible(limitMaxSize.isSelected());
        maxCacheBoxLabel.setVisible(limitMaxSize.isSelected());
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
            logSettings.setMaxiumLogFileAge(LogSettings.getSingletonInstance().FILE_LOGGER, Integer.parseInt(debugFileTextField.getText()));
        }
        if (debugConsoleCombo != null) {
            Level level = (Level) debugConsoleCombo.getSelectedItem();
            logSettings.setLoggingLevel("console", level);
        }

        // Default values
        defaultsPanel.saveSettings();
        // Maximum JPIP cache size
        if (limitMaxSize.isSelected())
            settings.setProperty("jpip.cache.size", maxCacheBox.getText());
        else
            settings.setProperty("jpip.cache.size", "0.0");

        // Update and save settings
        settings.update();
        settings.save();
        LogSettings.getSingletonInstance().update();
    }

    /**
     * Builds the string showing the size of the cache currently used.
     *
     * @return String showing the size of the cache currently used
     */
    private String getCacheSizeText() {
        long len = 0;
        File[] list = JHV_Kdu_cache.getCacheFiles(JHVDirectory.CACHE.getFile());

        for (File f : list)
            len += f.length();

        if (len < 1024)
            return ("Current size: " + len + " bytes");
        else if (len < 1048576)
            return ("Current size: " + (Math.round((len / 1024.0) * 100.0) / 100.0) + " Kbytes");
        else
            return ("Current size: " + (Math.round((len / 1048576.0) * 100.0) / 100.0) + " Mbytes");
    }

    /**
     * Creates the JPIP cache panel.
     *
     * @return JPIP cache panel
     */
    private JPanel createJPIPCachePanel() {
        JPanel cachePanel = new JPanel(new GridLayout(0, 1));
        cachePanel.setBorder(BorderFactory.createTitledBorder(" JPIP Cache "));

        // cache location
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        row1.add(new JLabel("Cache location: " + JHVDirectory.CACHE.getFile().getAbsolutePath()));

        // used cache size
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        row2.add(occupiedSizeLabel);

        // maximum cache size
        maxCacheBox.setPreferredSize(new Dimension(80, maxCacheBox.getPreferredSize().height));

        limitMaxSize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                maxCacheBox.setVisible(limitMaxSize.isSelected());
                maxCacheBoxLabel.setVisible(limitMaxSize.isSelected());
            }
        });

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        row3.add(limitMaxSize);
        row3.add(maxCacheBox);
        row3.add(maxCacheBoxLabel);

        cachePanel.add(row1);
        cachePanel.add(row2);
        cachePanel.add(row3);

        return cachePanel;
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
        Level fileLoggingLevel = logSettings.getLoggingLevel(LogSettings.getSingletonInstance().FILE_LOGGER);
        Level consoleLoggingLevel = logSettings.getLoggingLevel(LogSettings.getSingletonInstance().CONSOLE_LOGGER);

        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        if (fileLoggingLevel != null) {
            row4.add(new JLabel("File log level", JLabel.RIGHT));
            debugFileCombo = new JComboBox(LogSettings.getSingletonInstance().LEVELS);
            row4.add(debugFileCombo);
            debugFileCombo.setSelectedItem(fileLoggingLevel);
        }

        if (consoleLoggingLevel != null) {
            row4.add(new JLabel("Console log level", JLabel.RIGHT));
            debugConsoleCombo = new JComboBox(LogSettings.getSingletonInstance().LEVELS);
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
            debugFileTextField.setText(Integer.toString(logSettings.getMaxiumLogFileAge(LogSettings.getSingletonInstance().FILE_LOGGER)));
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
            setPreferredSize(new Dimension(150, 180));

            Settings settings = Settings.getSingletonInstance();

            tableData = new Object[][] { { "Default save directory", settings.getProperty("default.save.path") }, { "Default local path", settings.getProperty("default.local.path") }, { "Default remote path", settings.getProperty("default.remote.path") } };

            table = new JTable(new DefaultTableModel(tableData, new String[] { "Description", "Value" }) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return ((row == 2) && (column == 1));
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
            model.setValueAt(settings.getProperty("default.remote.path"), 2, 1);
        }

        public void saveSettings() {
            TableModel model = table.getModel();
            Settings settings = Settings.getSingletonInstance();
            settings.setProperty("default.save.path", model.getValueAt(0, 1).toString());
            settings.setProperty("default.local.path", model.getValueAt(1, 1).toString());
            settings.setProperty("default.remote.path", model.getValueAt(2, 1).toString());
        }

        public void resetSettings() {
            TableModel model = table.getModel();
            model.setValueAt(JHVDirectory.EXPORTS.getPath(), 0, 1);
            model.setValueAt(JHVDirectory.HOME.getPath(), 1, 1);
            model.setValueAt("jpip://delphi.nascom.nasa.gov:8090", 2, 1);
        }
    }

    @Override
    public void init() {
    }

}
