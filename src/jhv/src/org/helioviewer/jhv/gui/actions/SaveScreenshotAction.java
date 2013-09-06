package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * Action to save a screenshot in default format (png) to default location.
 * 
 * <p>
 * The default location is {@link org.helioviewer.jhv.JHVDirectory#EXPORTS}. The
 * name of the file will be determined based in the source file.
 * 
 * @author Markus Langenberg
 */
public class SaveScreenshotAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SaveScreenshotAction() {
        super("Save Screenshot");
        putValue(SHORT_DESCRIPTION, "Save Screenshot to Default Folder");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        try {
            String filename = new String(JHVDirectory.EXPORTS.getPath() + getDefaultFileName() + ".png");
            ImageViewerGui.getSingletonInstance().getMainView().saveScreenshot("png", new File(filename));

            JOptionPane.showMessageDialog(ImageViewerGui.getMainFrame(), "Saved Screenshot at: " + filename);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Returns the default name for a screenshot. The name consists of
     * "JHV_screenshot_created" plus the current system date and time.
     * 
     * @return Default name for a screenshot.
     */
    static String getDefaultFileName() {
        String output = new String("JHV_screenshot_created_");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        output += dateFormat.format(new Date());

        return output;
    }
}
