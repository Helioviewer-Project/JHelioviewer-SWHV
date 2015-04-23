package org.helioviewer.jhv.gui.dialogs.pluginsOLD;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.AbstractList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.viewmodelplugin.controller.PluginContainer;
import org.helioviewer.viewmodelplugin.controller.PluginManager;

/**
 * This class provides a dialog which allows to manage the plug-ins. Available
 * plug-ins can be activated so there functionality is available in JHV and
 * plug-ins can be disabled.
 *
 * @author Stephan Pagel
 */
public class PluginDialog extends AbstractPluginDialog implements ListSelectionListener {

    private final JButton availablePluginsImportButton = new JButton("Import");
    private final JButton availablePluginsDeleteButton = new JButton("Delete");

    /**
     * Default constructor.
     */
    public PluginDialog() {
        super("Plugin Manager");
        initVisualComponents();
        displayData();
    }

    /**
     * Initialize the visual parts of the component.
     */
    private void initVisualComponents() {
        // set tool tip to activate and deactivate filter buttons
        super.setActivateButtonToolTipText("Enable plugin");
        super.setDeactivateButtonToolTipText("Disable plugin");

        // add control buttons the the available plug-in list to import and
        // delete plug-ins.
        availablePluginsImportButton.addActionListener(this);
        availablePluginsDeleteButton.addActionListener(this);

        super.addAvailablePluginsControlButton(availablePluginsImportButton);
        super.addAvailablePluginsControlButton(availablePluginsDeleteButton);

        availablePluginsList.addListSelectionListener(this);

        updateDeleteButton();
    }

    /**
     * Fills the lists with all available and activated plugins.
     */
    private void displayData() {
        // show activated plug-ins in corresponding list
        activatedPluginsListModel.clear();
        AbstractList<PluginContainer> activatedPlugins = PluginManager.getSingletonInstance().getPlugins(true);

        for (PluginContainer container : activatedPlugins) {
            activatedPluginsListModel.addElement(container);

        }

        // show available plug-ins in corresponding list
        availablePluginsListModel.clear();
        AbstractList<PluginContainer> availablePlugins = PluginManager.getSingletonInstance().getPlugins(false);

        for (PluginContainer container : availablePlugins) {
            availablePluginsListModel.addElement(container);
        }
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
            public void actionPerformed(ActionEvent _e) {
                if (_e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION) && fileChooser.getSelectedFile().exists() && fileChooser.getSelectedFile().isFile()) {
                    fileChooser.setVisible(false);
                    final File dstFile = new File(JHVDirectory.PLUGINS.getPath() + fileChooser.getSelectedFile().getName());

                    if (dstFile.exists()) {
                        Message.err("An error occured while importing the plugin.", "A plugin with the same name already exists!", false);
                        return;
                    }

                    try {
                        FileUtils.copy(fileChooser.getSelectedFile(), dstFile);
                    } catch (IOException e) {
                        Message.err("An error occured while importing the plugin.", "Copying the plugin file to the plugin directory failed!", false);
                        return;
                    }
                    setChanged(true);

                    try {
                        PluginManager.getSingletonInstance().loadPlugin(dstFile.toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Message.err("An error occured while loading the plugin.", "The plugin file is corrupt!", false);
                        return;
                    }

                    displayData();
                }
            }
        });

        fileChooser.showOpenDialog(this);
    }

    /**
     * Deletes the plug-in file of selected entry.
     */
    private void deletePlugin() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure to delete the corresponding plug in file permanently from your file system?", "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            final Object selected = availablePluginsList.getSelectedValue();

            if (selected instanceof PluginContainer) {
                final URI pluginLocation = ((PluginContainer) selected).getPluginLocation();
                PluginManager.getSingletonInstance().removePluginContainer((PluginContainer) selected);
                availablePluginsListModel.removeElement(selected);

                final File file = new File(pluginLocation);

                if (!file.delete()) {
                    Message.err("An error occured while deleting the plugin file!", "Please check manually!", false);
                }
            }
        }
    }

    private void updateDeleteButton() {
        availablePluginsDeleteButton.setEnabled(availablePluginsList.getSelectedIndex() >= 0);
    }

    // Action Listener

    /**
     * {@inheritDoc}
     */

    @Override
    public void actionPerformed(ActionEvent arg0) {
        super.actionPerformed(arg0);

        if (arg0.getSource() == availablePluginsImportButton) {
            importPlugin();
        } else if (arg0.getSource() == availablePluginsDeleteButton) {
            deletePlugin();
        }
    }

    // List Selection Listener

    @Override
    public void valueChanged(final ListSelectionEvent event) {
        updateDeleteButton();
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
