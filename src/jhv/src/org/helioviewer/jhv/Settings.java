package org.helioviewer.jhv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.logging.Log;

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
    private final Properties defaultProperties = new Properties();
    private final Properties userProperties = new Properties();

    /** The properties file */
    private final File propFile = new File(JHVDirectory.SETTINGS.getPath() + "user.properties");

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
                Log.debug(">> Settings.load() > Load default system settings: " + defaultProperties);
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
        save(userProperties);
    }

    private void save(Properties props) {
        try {
            propFile.createNewFile();
            FileOutputStream fileOutput = new FileOutputStream(propFile);
            props.store(fileOutput, null);
            fileOutput.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void save(String key) {
        Properties props = loadCopyUser();
        props.setProperty(key, getProperty(key));
        save(props);
    }

    private Properties loadCopyUser() {
        Properties props = new Properties();
        try {
            if (propFile.exists()) {
                FileInputStream fileInput = new FileInputStream(propFile);
                props.load(fileInput);
                fileInput.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return props;
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
    public void setLookAndFeelEverywhere(JFrame frame, String lookAndFeel) {
        if (lookAndFeel == null) {
            lookAndFeel = getProperty("display.laf");
            if (lookAndFeel == null) {
                lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            }
        }

        if (!UIManager.getLookAndFeel().getClass().getName().equals(lookAndFeel)) {
            try {
                UIManager.setLookAndFeel(lookAndFeel);
                if (frame != null) {
                    SwingUtilities.updateComponentTreeUI(frame);
                    frame.pack();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        // ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        // JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    }

}
