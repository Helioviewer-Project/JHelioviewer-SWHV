package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

/**
 * The Plug-in Dialog allows to manage all available plug-ins. Plug-ins can be
 * added, removed or enabled / disabled.
 *
 * @author Stephan Pagel
 * */
@SuppressWarnings({ "serial" })
public class PluginsDialog extends JDialog implements ShowableDialog, ActionListener, WindowListener, ListEntryChangeListener {

    private boolean changesMade = false;

    private static final Dimension DIALOG_SIZE_MINIMUM = new Dimension(400, 500);
    private static final Dimension DIALOG_SIZE_PREFERRED = new Dimension(400, 500);

    private final JPanel contentPane = new JPanel();

    private final JComboBox filterComboBox = new JComboBox(new String[] { "All", "Enabled", "Disabled" });
    private final JButton addButton = new JButton("Add plug-in", IconBank.getIcon(JHVIcon.ADD));
    private final JButton downloadButton = new JButton("Download");

    private final JLabel emptyLabel = new JLabel("No plug-ins available", JLabel.CENTER);
    private final List pluginList = new List();
    private final JScrollPane emptyScrollPane = new JScrollPane(emptyLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private final JPanel listContainerPane = new JPanel();
    private final CardLayout listLayout = new CardLayout();

    private final JButton okButton = new JButton("OK", IconBank.getIcon(JHVIcon.CHECK));

    public PluginsDialog() {
        super(ImageViewerGui.getMainFrame(), "Plug-in Manager", true);
        initVisualComponents();
    }

    /**
     * Initialize the visual parts of the component.
     */
    private void initVisualComponents() {
        // dialog
        setMinimumSize(DIALOG_SIZE_MINIMUM);
        setPreferredSize(DIALOG_SIZE_PREFERRED);
        setContentPane(contentPane);
        addWindowListener(this);

        // header
        final StringBuilder headerText = new StringBuilder();
        headerText.append("<html><font style=\"font-family: '" + getFont().getFamily() + "'; font-size: " + getFont().getSize() + ";\">");
        headerText.append("<p style=\"padding-left:10px\">");
        headerText.append("You can import, enable or delete JHelioviewer plug-ins."); // TODO
        headerText.append("</p></font></html>");

        final JEditorPane headerPane = new JEditorPane("text/html", headerText.toString());
        headerPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 6, 3));
        headerPane.setEditable(false);
        headerPane.setOpaque(false);

        // center - installed plug-ins
        final JPanel installedFilterPane = new JPanel();
        installedFilterPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        installedFilterPane.add(new JLabel("Filter"));
        installedFilterPane.add(filterComboBox);

        filterComboBox.addActionListener(this);

        pluginList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        emptyScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
        emptyLabel.setHorizontalTextPosition(JLabel.CENTER);
        emptyLabel.setOpaque(true);
        emptyLabel.setBackground(Color.WHITE);

        listContainerPane.setLayout(listLayout);
        listContainerPane.add(emptyScrollPane, "empty");
        listContainerPane.add(pluginList, "list");

        pluginList.addListEntryChangeListener(this);

        final JPanel installedButtonPane = new JPanel();
        installedButtonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        installedButtonPane.add(addButton);
        // installedButtonPane(downloadButton); //TODO SP: add

        addButton.setToolTipText("Add a new plug-in to JHelioviewer");
        addButton.addActionListener(this);
        downloadButton.addActionListener(this);

        final JPanel installedPane = new JPanel();
        installedPane.setLayout(new BorderLayout());
        installedPane.setBorder(BorderFactory.createTitledBorder(" Installed Plug-ins "));
        installedPane.add(installedFilterPane, BorderLayout.PAGE_START);
        installedPane.add(listContainerPane, BorderLayout.CENTER);
        installedPane.add(installedButtonPane, BorderLayout.PAGE_END);

        // center
        final JPanel centerPane = new JPanel();
        centerPane.setLayout(new BorderLayout());
        centerPane.add(installedPane, BorderLayout.CENTER);

        // footer
        final JPanel footer = new JPanel();
        footer.setLayout(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        footer.add(okButton);

        okButton.setToolTipText("Close the dialog");
        okButton.addActionListener(this);

        // content pane
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        contentPane.add(headerPane, BorderLayout.PAGE_START);
        contentPane.add(centerPane, BorderLayout.CENTER);
        contentPane.add(footer, BorderLayout.PAGE_END);
    }

    /**
     * Updates visual components according to the current state.
     * */
    private void updateVisualComponents() {
        if (pluginList.getNumberOfItems() > 0) {
            listLayout.show(listContainerPane, "list");
        } else {
            switch (filterComboBox.getSelectedIndex()) {
            case 0:
                emptyLabel.setText("No plug-ins available");
                break;
            case 1:
                emptyLabel.setText("No plug-ins enabled");
                break;
            case 2:
                emptyLabel.setText("No plug-ins disabled");
                break;
            }
            listLayout.show(listContainerPane, "empty");
        }
    }

    /**
     * This method will close the dialog and handles things which have to be
     * done before. This includes saving the settings and rebuild the viewchains
     * with the current activated plug ins.
     */
    private void closeDialog() {
        if (changesMade) {
            // save plug-in settings to XML file
            PluginManager.getSingletonInstance().saveSettings();
        }
        // close dialog
        dispose();
    }

    /**
     * Removes all entries from the plug-in list and adds all available plug-ins
     * to the list again.
     * */
    private void updatePluginList() {
        final PluginContainer[] plugins = PluginManager.getSingletonInstance().getAllPlugins();
        final int filterIndex = filterComboBox.getSelectedIndex();

        final PluginListEntry entry = (PluginListEntry) pluginList.getSelectedEntry();
        final String selectedPlugin = entry == null ? null : entry.getPluginContainer().getName();

        pluginList.removeAllEntries();

        for (final PluginContainer plugin : plugins) {
            if (filterIndex == 0 || (plugin.isActive() && filterIndex == 1) || (!plugin.isActive() && filterIndex == 2)) {
                pluginList.addEntry(plugin.getName(), new PluginListEntry(plugin, pluginList));
            }
        }
        pluginList.selectItem(selectedPlugin);

        updateVisualComponents();
    }

    /**
     * This method allows the user to select a plug-in file which has to be
     * loaded. The file will be copied to the plug-in directory of JHV and the
     * plug-in will occur in the list of available plug-ins. If a file with the
     * same name already exists in the plug-in directory the selected file will
     * not be copied.
     */
    private void importPlugin() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.addChoosableFileFilter(new JARFilter());
        fileChooser.setMultiSelectionEnabled(false);

        fileChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent _e) {
                if (_e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION) && fileChooser.getSelectedFile().exists() && fileChooser.getSelectedFile().isFile()) {
                    fileChooser.setVisible(false);

                    final File dstFile = new File(JHVDirectory.PLUGINS.getPath() + fileChooser.getSelectedFile().getName());
                    if (dstFile.exists()) {
                        Message.err("An error occured while importing the plugin.", "A plugin with the same name already exists!", false);
                        return;
                    }

                    try {
                        FileUtils.copy(fileChooser.getSelectedFile(), dstFile);
                    } catch (final IOException e) {
                        Message.err("An error occured while importing the plugin.", "Copying the plugin file to the plugin directory failed!", false);
                        return;
                    }

                    try {
                        PluginManager.getSingletonInstance().loadPlugin(dstFile.toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Message.err("An error occured while loading the plugin.", "The plugin file is corrupt!", false);
                        return;
                    }

                    updatePluginList();
                    pluginList.fireItemChanged();
                }
            }
        });

        fileChooser.showOpenDialog(this);
    }

    private void downloadPlugins() {
        updatePluginList();
    }

    // Showable Dialog

    /**
     * {@inheritDoc}
     * */
    @Override
    public void showDialog() {
        changesMade = false;

        updatePluginList();
        pluginList.selectFirstItem();

        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    // Action Listener

    /**
     * {@inheritDoc}
     * */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(okButton)) {
            closeDialog();
        } else if (e.getSource().equals(addButton)) {
            importPlugin();
        } else if (e.getSource().equals(downloadButton)) {
            downloadPlugins();
        } else if (e.getSource().equals(filterComboBox)) {
            updatePluginList();
        }
    }

    // Window Listener

    /**
     * {@inheritDoc}
     * */
    @Override
    public void windowActivated(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void windowClosed(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void windowClosing(final WindowEvent e) {
        closeDialog();
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void windowDeactivated(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void windowDeiconified(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void windowIconified(final WindowEvent e) {
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void windowOpened(final WindowEvent e) {
    }

    // List Entry Change Listener

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemChanged() {
        changesMade = true;

        updatePluginList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void listChanged() {
        updateVisualComponents();
    }

    // JAR Filter

    /**
     * File Chooser Filter which allows JAR files only.
     * */
    private class JARFilter extends FileFilter {

        private final String[] extensions = { "jar" };

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(File f) {
            if (f.isDirectory())
                return true;

            String testName = f.getName().toLowerCase();
            for (String ext : extensions) {
                if (testName.endsWith(ext))
                    return true;
            }

            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return "JAR files (\".jar\")";
        }

    }

    @Override
    public void init() {
    }

}
