package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.JHVStateFilter;
import org.helioviewer.jhv.layers.LayersModel;

public class LoadStateAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final URL stateLocation;

    /**
     * Constructor specifying no location to load.
     * 
     * The user will be prompted to select the file to be loaded. The title will
     * be "Load State..." in every case.
     */
    public LoadStateAction() {
        super("Load State...");
        putValue(SHORT_DESCRIPTION, "Loads the state saved in user specified location");
        stateLocation = null;
    }

    /**
     * Constructor specifying the file to load.
     * 
     * The title of the menu item will be formed from the file name.
     * 
     * @param location
     *            URL specifying the state to load
     */
    public LoadStateAction(URL location) {
        this("Load state " + location.getPath().substring(location.getPath().lastIndexOf('/') + 1), location);
        putValue(SHORT_DESCRIPTION, "Loads the state saved in " + location.getFile());
    }

    /**
     * Constructor specifying the title of the menu item and the file to load.
     * 
     * @param title
     *            Title of the menu item
     * @param location
     *            URL specifying the state to load
     */
    public LoadStateAction(String title, URL location) {
        super(title);
        putValue(SHORT_DESCRIPTION, "Loads the saved state");

        stateLocation = location;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {

        final URL selectedLocation;

        if (stateLocation == null) {
            final JFileChooser fileChooser = new JFileChooser(JHVDirectory.STATES.getPath());
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.addChoosableFileFilter(new JHVStateFilter());
            fileChooser.setMultiSelectionEnabled(false);

            int retVal = fileChooser.showOpenDialog(ImageViewerGui.getMainFrame());

            if (retVal != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile.exists() && selectedFile.isFile()) {
                try {
                    selectedLocation = selectedFile.toURI().toURL();
                } catch (MalformedURLException e1) {
                    Log.error("Error while opening state " + selectedFile, e1);
                    return;
                }
            } else {
                return;
            }
        } else {
            selectedLocation = stateLocation;
        }

        // If the function reaches this point, it is guaranteed that
        // stateLocation is not null.
        new Thread(new Runnable() {
            public void run() {
                ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(true);
                LayersModel.getSingletonInstance().loadState(selectedLocation);
                ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
            }
        }, "LoadStateThread").start();
    }
}
