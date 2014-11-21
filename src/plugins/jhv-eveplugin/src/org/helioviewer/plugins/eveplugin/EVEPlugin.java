package org.helioviewer.plugins.eveplugin;

import java.awt.EventQueue;
import java.net.URL;
import java.util.LinkedList;

import javax.swing.JComponent;

import org.helioviewer.jhv.JavaHelioViewerLauncher;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.events.data.EventRequester;
import org.helioviewer.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.plugins.eveplugin.lines.data.DatabaseController;
import org.helioviewer.plugins.eveplugin.radio.model.RadioPlotModel;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.ControlsPanel;
import org.helioviewer.plugins.eveplugin.view.MainPanel;
import org.helioviewer.plugins.eveplugin.view.ObservationDialogUIPanel;
import org.helioviewer.plugins.eveplugin.view.SimpleObservationDialogUIPanel;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

/**
 * 
 * 
 * @author Stephan Pagel
 * */
public class EVEPlugin implements Plugin, MainContentPanelPlugin {

    private final LinkedList<JComponent> pluginPanes = new LinkedList<JComponent>();
    private MainPanel mainPanel;

    @Override
    public void installPlugin() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                EventRequester eventRequester = EventRequester.getSingletonInstance();
                ZoomController.getSingletonInstance().addZoomControllerListener(eventRequester);
                eventRequester.addListener(EventModel.getSingletonInstance());
                ZoomController.getSingletonInstance().addZoomControllerListener(EventModel.getSingletonInstance());
                if (mainPanel == null) {
                    mainPanel = new MainPanel();
                }

                pluginPanes.add(mainPanel);

                ImageViewerGui.getSingletonInstance().getLeftContentPane()
                        .add("Timeline Layers", ControlsPanel.getSingletonInstance(), true);

                ImageViewerGui.getSingletonInstance().getMainContentPanel().addPlugin(EVEPlugin.this);
                ObservationDialog.getSingletonInstance().addUserInterface(EVESettings.OBSERVATION_UI_NAME,
                        new ObservationDialogUIPanel(mainPanel.getPlotContainerPanel()));
                ObservationDialog.getSingletonInstance().addUserInterface(EVESettings.RADIO_OBSERVATION_UI_NAME,
                        new SimpleObservationDialogUIPanel(mainPanel.getPlotContainerPanel()));
                // initialize database connection
                DatabaseController.getSingletonInstance();
                RadioPlotModel.getSingletonInstance();
            }
        });

    }

    @Override
    public void uninstallPlugin() {
        ObservationDialog.getSingletonInstance().removeUserInterface(EVESettings.OBSERVATION_UI_NAME,
                new ObservationDialogUIPanel(mainPanel.getPlotContainerPanel()));
        ObservationDialog.getSingletonInstance().removeUserInterface(EVESettings.RADIO_OBSERVATION_UI_NAME,
                new SimpleObservationDialogUIPanel(mainPanel.getPlotContainerPanel()));
        ImageViewerGui.getSingletonInstance().getMainContentPanel().removePlugin(this);
        ImageViewerGui.getSingletonInstance().getLeftContentPane().remove(ControlsPanel.getSingletonInstance());
    }

    public static URL getResourceUrl(String name) {
        return EVEPlugin.class.getResource(name);
    }

    @Override
    public String getName() {
        return "EVEPlugin " + "$Rev$";
    }

    @Override
    public String getDescription() {
        return "This plugin visualizes the level 2 data of the EVE project";
    }

    @Override
    public String getAboutLicenseText() {
        String description = "";

        description += "<p>The plugin uses the <a href=\"http://www.json.org/java/\">JSON in Java</a> Library, licensed under a <a href=\"http://www.json.org/license.html\">custom License</a>.";
        description += "<p>The plugin uses <a href=\"http://db.apache.org/derby/\">Apache Derby, an Apache DB subproject</a>,<br>"
                + '\u00A9'
                + " 2011, Apache Software Foundation, <a href=\"http://www.apache.org/licenses/\">Apache License, Version 2.0</a><br>";

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

    @Override
    public LinkedList<JComponent> getVisualInterfaces() {
        return pluginPanes;
    }

    @Override
    public String getTabName() {
        return "1-D and 2-D Time Series";
    }

    @Override
    public void setState(String state) {
        // TODO SP: Implement setState for EVEPlugin
    }

    @Override
    public String getState() {
        // TODO SP: Implement getState for EVEPlugin
        return "";
    }
}
