/**
 * 
 */
package ch.fhnw.jhv.main;

import ch.fhnw.jhv.gui.MainGui;
import ch.fhnw.jhv.gui.Settings;

/**
 * Main Starter Class
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class Main {

    /**
     * MainGui which contains the whole gui
     */
    private MainGui mainGui;

    /**
     * Settings provide the Plugin configuration
     */
    private Settings settings;

    /**
     * Constructor
     */
    public Main() {

        // Create the Settings, could also be singleton..
        settings = new Settings();

        // Load the Default settings of the gui composition
        settings.loadDefaultSettings();

        // Initialize the Main Gui
        mainGui = new MainGui();

    }

    /**
     * Main
     * 
     * @param args
     *            String[]
     */
    public static void main(String[] args) {
        // Start main Class and the whole process
        new Main();
    }

    public MainGui getMainGui() {
        return mainGui;
    }
}
