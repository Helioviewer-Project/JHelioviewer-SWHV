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
import java.util.LinkedList;

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

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.pluginsOLD.OverlayPluginDialog;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.viewmodel.factory.ViewFactory;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.opengl.GLOverlayView;
import org.helioviewer.viewmodelplugin.controller.PluginContainer;
import org.helioviewer.viewmodelplugin.controller.PluginManager;

/**
 * The Plug-in Dialog allows to manage all available plug-ins. Plug-ins can be
 * added, removed or enabled / disabled.
 *
 * @author Stephan Pagel
 * */
public class PluginsDialog extends JDialog implements ShowableDialog, ActionListener, WindowListener, ListEntryChangeListener {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private boolean changesMade = false;

    private static final Dimension DIALOG_SIZE_MINIMUM = new Dimension(400, 500);
    private static final Dimension DIALOG_SIZE_PREFERRED = new Dimension(400, 500);

    private final JPanel contentPane = new JPanel();

    private final JComboBox filterComboBox = new JComboBox(new String[] { "All", "Enabled", "Disabled" });
    private final JButton addButton = new JButton("Add Plug-in", IconBank.getIcon(JHVIcon.ADD));
    private final JButton downloadButton = new JButton("Download");

    private final JLabel emptyLabel = new JLabel("No Plug-ins available", JLabel.CENTER);
    private final List pluginList = new List();
    private final JScrollPane emptyScrollPane = new JScrollPane(emptyLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private final JPanel listContainerPane = new JPanel();
    private final CardLayout listLayout = new CardLayout();

    private final JButton overlayButton = new JButton("Overlays");

    private final JButton okButton = new JButton("Ok", IconBank.getIcon(JHVIcon.CHECK));

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
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
        headerText.append("<b>JHelioviewer Plug-ins</b>");
        headerText.append("<p style=\"padding-left:10px\">");
        headerText.append("Manage available plug-ins of JHelioviewer.<br>");
        headerText.append("You can import enable or delete plug-ins.<br>"); // TODO
        // SP:
        // add
        // "download"
        headerText.append("Press the information buttons to get additional details.");
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

        // ////////
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

        // ////////
        final JPanel installedButtonPane = new JPanel();
        installedButtonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        installedButtonPane.add(addButton);
        // installedButtonPane(downloadButton); //TODO SP: add

        addButton.setToolTipText("Add a new plug-in to JHelioviewer.");
        addButton.addActionListener(this);
        downloadButton.addActionListener(this);

        final JPanel installedPane = new JPanel();
        installedPane.setLayout(new BorderLayout());
        installedPane.setBorder(BorderFactory.createTitledBorder(" Installed Plug-ins "));
        installedPane.add(installedFilterPane, BorderLayout.PAGE_START);
        installedPane.add(listContainerPane, BorderLayout.CENTER);
        installedPane.add(installedButtonPane, BorderLayout.PAGE_END);

        // center - sequence arrangement
        final JPanel managePane = new JPanel();
        managePane.setLayout(new FlowLayout(FlowLayout.LEFT));
        managePane.setBorder(BorderFactory.createTitledBorder(" Filter and Overlays "));
        managePane.add(overlayButton);

        overlayButton.setToolTipText("Opens a dialog where all available overlays can be managed.");

        overlayButton.addActionListener(this);

        // center
        final JPanel centerPane = new JPanel();
        centerPane.setLayout(new BorderLayout());
        centerPane.add(installedPane, BorderLayout.CENTER);
        centerPane.add(managePane, BorderLayout.PAGE_END);

        // footer
        final JPanel footer = new JPanel();
        footer.setLayout(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        footer.add(okButton);

        okButton.setToolTipText("Closes the dialog.");
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
                emptyLabel.setText("No Plug-ins available");
                break;
            case 1:
                emptyLabel.setText("No Plug-ins enabled");
                break;
            case 2:
                emptyLabel.setText("No Plug-ins disabled");
                break;
            }

            listLayout.show(listContainerPane, "empty");
        }

        overlayButton.setEnabled(PluginManager.getSingletonInstance().getNumberOfOverlays() > 0);
    }

    /**
     * This method will close the dialog and handles things which have to be
     * done before. This includes saving the settings and rebuild the viewchains
     * with the current activated plug ins.
     */
    private void closeDialog() {
        if (changesMade) {
            // rebuild the view chains
            recreateViewChains();

            // save plug-in settings to XML file
            PluginManager.getSingletonInstance().saveSettings();
        }

        // close dialog
        dispose();
    }

    /**
     * Rebuilds the existing view chains and removes and adds corresponding
     * parts from plug ins.
     */
    private void recreateViewChains() {
        Thread.dumpStack();
        GL3DViewchainFactory chainFactory = new GL3DViewchainFactory();
        ViewFactory viewFactory = chainFactory.getUsedViewFactory();

        // Memorize all ImageInfoViews, remove all existing layers and add the
        // memorized ImageInfoViews as new layers again. Activated and needed
        // filters will be added to the corresponding sub chains.
        LayeredView mainLayeredView = ImageViewerGui.getSingletonInstance().getMainView().getAdapter(LayeredView.class);
        LinkedList<ImageInfoView> newImageInfoViews = new LinkedList<ImageInfoView>();

        while (mainLayeredView.getNumLayers() > 0) {
            newImageInfoViews.add(viewFactory.createViewFromSource(mainLayeredView.getLayer(0).getAdapter(ImageInfoView.class), true));
            mainLayeredView.removeLayer(0);
        }

        for (ImageInfoView imageView : newImageInfoViews) {
            chainFactory.addLayerToViewchainMain(imageView, mainLayeredView);
        }

        // Update all OverlayViews which are included in the view chain above
        // the layered view
        GLOverlayView overlayView = ImageViewerGui.getSingletonInstance().getMainView().getAdapter(GLOverlayView.class);
        chainFactory.updateOverlayViewsInViewchainMain(overlayView);
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

    /**
     *
     * */
    private void downloadPlugins() {
        // TODO SP: show download dialog

        updatePluginList();
    }

    // ////////////////////////////////////////////////////////////////
    // Showable Dialog
    // ////////////////////////////////////////////////////////////////

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

    // ////////////////////////////////////////////////////////////////
    // Action Listener
    // ////////////////////////////////////////////////////////////////

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
        } else if (e.getSource().equals(overlayButton)) {
            new OverlayPluginDialog().showDialog();
        } else if (e.getSource().equals(filterComboBox)) {
            updatePluginList();
        }
    }

    // ////////////////////////////////////////////////////////////////
    // Window Listener
    // ////////////////////////////////////////////////////////////////

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

    // ////////////////////////////////////////////////////////////////
    // List Entry Change Listener
    // ////////////////////////////////////////////////////////////////

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

    // ////////////////////////////////////////////////////////////////
    // JAR Filter
    // ////////////////////////////////////////////////////////////////

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