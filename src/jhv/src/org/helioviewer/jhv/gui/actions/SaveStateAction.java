package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JHVStateFilter;
import org.helioviewer.jhv.layers.LayersModel;

public class SaveStateAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SaveStateAction() {
        super("Save Current State ...");
        putValue(SHORT_DESCRIPTION, "Saves the current state of JHV");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {

        // prompt user to choose filename
        File selectedFile = chooseFile();

        // if the user selected a filename
        if (selectedFile != null) {

            // get layer XML representation
            String layerRepresentation = LayersModel.getSingletonInstance().getXMLRepresentation("\t");

            // TODO Malte Nuhn : get Plugin XML representation

            // merge all XML blocks together
            String xmlRepresentation = "<jhvstate>\n" + layerRepresentation + "\n</jhvstate>";

            // write to disk
            boolean success = writeXML(selectedFile, xmlRepresentation);

            // check if an error occured
            if (!success) {
                Message.err(null, "An error occured while writing the JHV state to disk.", false);
            }

        }

    }

    /**
     * Write the given xmlData to the given file handle
     * 
     * @param selectedFile
     *            - 'File' object to write to
     * @param xmlData
     *            - the data to write to disk
     * @return true if the operation was successful, false if an error occured
     */
    private boolean writeXML(File selectedFile, String xmlData) {
        boolean success = true;
        Writer xmlWriter = null;

        try {
            xmlWriter = new OutputStreamWriter(new FileOutputStream(selectedFile));
            xmlWriter.write(xmlData);
        } catch (FileNotFoundException fileNotFoundException) {
            success = false;
        } catch (IOException ioException) {
            success = false;
        } finally {
            // in any case, try to close the xmlWriter
            try {
                if (xmlWriter != null) {
                    xmlWriter.close();
                }
            } catch (IOException ioException) {
                // ignore this exception
            }
        }
        return success;
    }

    /**
     * Prompt the user to choose a filename and return a 'File' object pointing
     * to the selected location. In case the user selects an already existing
     * filename, the user is asked if he really wants to overwrite the file.
     * <p>
     * If any errors occur, null is returned.
     * 
     * @return a 'File' object pointing to the selected location, null if an
     *         error occured
     */
    private File chooseFile() {
        JFileChooser fileChooser = new JFileChooser(Settings.getSingletonInstance().getProperty("default.local.path"));
        fileChooser.setFileHidingEnabled(false);
        fileChooser.setCurrentDirectory(JHVDirectory.STATES.getFile());
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new JHVStateFilter());

        String txtTargetFile = getDefaultFileName();
        fileChooser.setSelectedFile(new File(txtTargetFile));

        int retVal = fileChooser.showSaveDialog(ImageViewerGui.getMainFrame());
        File selectedFile = null;

        if (retVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();

            // Has user entered the correct extension or not?
            ExtensionFileFilter fileFilter = (ExtensionFileFilter) fileChooser.getFileFilter();

            if (!fileFilter.accept(selectedFile)) {
                selectedFile = new File(selectedFile.getPath() + "." + fileFilter.getDefaultExtension());
            }

            // does the file already exist?
            if (selectedFile.exists()) {

                // ask if the user wants to overwrite
                int response = JOptionPane.showConfirmDialog(null, "Overwrite existing file?", "Confirm Overwrite", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                // if the user doesn't want to overwrite, simply return null
                if (response == JOptionPane.CANCEL_OPTION) {
                    return null;
                }
            }
        }

        return selectedFile;
    }

    /**
     * Returns the default name for a state. The name consists of
     * "JHV_state_saved" plus the current system date and time.
     * 
     * @return Default name for a screenshot.
     */
    static String getDefaultFileName() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

        String output = new String(JHVDirectory.STATES.getPath() + "JHV_state_saved_");
        output += dateFormat.format(new Date());

        return output;
    }

}