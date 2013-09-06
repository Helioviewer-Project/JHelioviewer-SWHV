package org.helioviewer.jhv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;

/**
 * A class that stores and reads default values in a settings file.
 * 
 * To check, whether the default settings have changed, a version string is
 * used. This string always should contain the date of the last change.
 * 
 * @author Benjamin Wamsler
 * @author Juan Pablo
 * @author Markus Langenberg
 * @author Andre Dau
 */
public class Settings {
    /** The sole instance of this class. */
    private static final Settings singletonInstance = new Settings();

    /** The properties object */
    private Properties defaultProperties = new Properties();
    private Properties userProperties = new Properties();

    /** The properties file */
    private File propFile = new File(JHVDirectory.SETTINGS.getPath() + "user.properties");

    /**
     * The private constructor of this class.
     * */
    private Settings() {
    }

    public void load() {
        load(true);
    }

    /**
     * Method loads the settings from a user file or the default settings file
     * */
    public void load(boolean verbose) {
        try {
            defaultProperties.clear();
            userProperties.clear();

            InputStream defaultPropStream = FileUtils.getResourceInputStream("/settings/defaults.properties");
            defaultProperties.load(defaultPropStream);
            defaultPropStream.close();
            if (verbose) {
                Log.debug(">> Settings.load() > Load default system settings: " + defaultProperties.toString());
            }
            if (propFile.exists()) {
                FileInputStream fileInput = new FileInputStream(propFile);
                userProperties.load(fileInput);
                fileInput.close();
            }

            if (getProperty("default.save.path") == null) {
                setProperty("default.save.path", JHVDirectory.EXPORTS.getPath());
            }
            if (getProperty("default.local.path") == null) {
                setProperty("default.local.path", JHVDirectory.HOME.getPath());
            }
        } catch (Exception ex) {
            if (verbose) {
                Log.error(">> Settings.load(boolean) > Could not load settings", ex);
            } else {
                ex.printStackTrace();
            }
        }
    }

    /**
     * This method saves all the values in the user properties file.
     */
    public void save() {
        try {
            propFile.createNewFile();
            FileOutputStream fileOutput = new FileOutputStream(propFile);
            userProperties.store(fileOutput, null);
            fileOutput.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * The new property values are updated.
     */
    public void update() {
        try {
            String val;

            val = getProperty("opengl.enabled");
            GLInfo.setGlEnabled(Boolean.parseBoolean(val));

            val = getProperty("display.laf");

            setLookAndFeelEverywhere(val);

            double size = 0;
            val = getProperty("jpip.cache.size");
            if (val != null) {
                try {
                    size = Double.valueOf(val);
                } catch (NumberFormatException ex) {
                    Log.error(">> Settings.update(boolean) > Invalid jpip cache size: " + val);
                }
                setProperty("jpip.cache.size", Double.toString(size));
            }

            JHV_Kdu_cache.updateCacheDirectory(JHVDirectory.CACHE.getFile(), size);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Method sets the value of a specified property and saves it as a user
     * setting
     * 
     * @param key
     *            Default field to be written to
     * @param val
     *            Value to be set to
     */
    public void setProperty(String key, String val) {
        if (!val.equals(getProperty(key))) {
            userProperties.setProperty(key, val);
        }
    }

    /**
     * Method that returns the value of the specified property. User defined
     * properties are always preferred over the default settings.
     * 
     * @param key
     *            Default field to read
     */
    public String getProperty(String key) {
        String val = userProperties.getProperty(key);
        if (val == null) {
            val = defaultProperties.getProperty(key);
        }
        return val;
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static Settings getSingletonInstance() {
        return singletonInstance;
    }

    /**
     * Sets the look and feel to all windows of the application.
     * 
     * @param lookAndFeel
     *            name of the lookandfeel.
     */
    public void setLookAndFeelEverywhere(String lookAndFeel) {

        if (!UIManager.getLookAndFeel().getClass().getName().equals(lookAndFeel)) {
            try {
                UIManager.setLookAndFeel(lookAndFeel);

                ImageViewerGui.getSingletonInstance();
                SwingUtilities.updateComponentTreeUI(ImageViewerGui.getMainFrame());

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * The values are saved to disk only if there have been a modification.
     */
    protected void finalize() {
        save();
    }
}
