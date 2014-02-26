package org.helioviewer.plugins.eveplugin;

import java.net.URL;
import java.util.LinkedList;

import javax.swing.JComponent;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.JavaHelioViewerLauncher;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;
import org.helioviewer.plugins.eveplugin.controller.DatabaseController;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDataManager;
import org.helioviewer.plugins.eveplugin.radio.model.RadioPlotModel;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.MainPanel;
import org.helioviewer.plugins.eveplugin.view.ObservationDialogUIPanel;
import org.helioviewer.plugins.eveplugin.view.SimpleObservationDialogUIPanel;
import org.helioviewer.plugins.eveplugin.view.plot.PlotManagerDialog;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

/**
 * 
 * 
 * @author Stephan Pagel
 * */
public class EVEPlugin implements Plugin, MainContentPanelPlugin {

    private final LinkedList<JComponent> pluginPanes = new LinkedList<JComponent>();
    private MainPanel mainPanel;
    
    public void installPlugin() {
    	Log.debug("EvePlugin : " + this);
    	if (mainPanel == null){
    		mainPanel = new MainPanel();
    	}
        
        
        pluginPanes.add(mainPanel);
        
        ImageViewerGui.getSingletonInstance().getMainContentPanel().addPlugin(this);        
        ObservationDialog.getSingletonInstance().addUserInterface(EVESettings.OBSERVATION_UI_NAME, new ObservationDialogUIPanel(mainPanel.getPlotContainerPanel()));
        ObservationDialog.getSingletonInstance().addUserInterface(EVESettings.RADIO_OBSERVATION_UI_NAME, new SimpleObservationDialogUIPanel(mainPanel.getPlotContainerPanel()));
        // initialize database connection
        DatabaseController.getSingletonInstance();
        RadioPlotModel.getSingletonInstance();
    }

    public void uninstallPlugin() {
        ImageViewerGui.getSingletonInstance().getMainContentPanel().removePlugin(this);
    }
    
    public static URL getResourceUrl(String name) {
        return EVEPlugin.class.getResource(name);
    }
    
    public String getName() {
        return "EVEPlugin";
    }

    public String getDescription() {
        return "This plugin visualizes the level 2 data of the EVE project";
    }

    public String getAboutLicenseText() {
        String description = "";
        
        description += "<p>The plugin uses the <a href=\"http://www.json.org/java/\">JSON in Java</a> Library, licensed under a <a href=\"http://www.json.org/license.html\">custom License</a>.";
        description += "<p>The plugin uses <a href=\"http://db.apache.org/derby/\">Apache Derby, an Apache DB subproject</a>,<br>" + '\u00A9' + " 2011, Apache Software Foundation, <a href=\"http://www.apache.org/licenses/\">Apache License, Version 2.0</a><br>"; 
        
        return description;        
    }

    /**
     * Used for testing the plugin
     * 
     * @see org.helioviewer.plugins.eveplugin.EVEPluginLauncher#main(String[])
     * @param args
     */
    public static void main(String[] args) {
        JavaHelioViewerLauncher.start(EVEPluginLauncher.class, args);
    }

    public LinkedList<JComponent> getVisualInterfaces() {
        return pluginPanes;
    }

    public String getTabName() {
        return "EVE";
    }

    public void setState(String state) {
        //TODO SP: Implement setState for EVEPlugin
    }

    public String getState() {
        //TODO SP: Implement getState for EVEPlugin
        return "";
    }
}
