package org.helioviewer.plugins.eveplugin;

import java.net.URL;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.JComponent;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.events.data.EventRequester;
import org.helioviewer.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.plugins.eveplugin.lines.model.EVEDrawController;
import org.helioviewer.plugins.eveplugin.radio.model.RadioPlotModel;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.ObservationDialogUIPanel;
import org.helioviewer.plugins.eveplugin.view.RadioObservationDialogUIPanel;
import org.helioviewer.plugins.eveplugin.view.TimelinePluginPanel;
import org.helioviewer.plugins.eveplugin.view.plot.PlotPanel;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

/**
 *
 *
 * @author Stephan Pagel
 * */
public class EVEPlugin implements Plugin, MainContentPanelPlugin {

    private final LinkedList<JComponent> pluginPanes = new LinkedList<JComponent>();
    private final PlotPanel plotOne = new PlotPanel("Plot 1: ");
    private final TimelinePluginPanel timelinePluginPanel = new TimelinePluginPanel();

    @Override
    public void installPlugin() {
        EventRequester eventRequester = EventRequester.getSingletonInstance();
        DrawController.getSingletonInstance().addTimingListener(eventRequester);
        eventRequester.addListener(EventModel.getSingletonInstance());
        DrawController.getSingletonInstance().addTimingListener(EventModel.getSingletonInstance());
        DrawController.getSingletonInstance().setAvailableInterval(new Interval<Date>(new Date(), new Date()));
        // Create an instance of eveDrawController and leave it here.
        EVEDrawController.getSingletonInstance();
        pluginPanes.add(plotOne);

        ImageViewerGui.getSingletonInstance().getLeftContentPane().add("Timeline Layers", timelinePluginPanel, true);

        ImageViewerGui.getSingletonInstance().getMainContentPanel().addPlugin(EVEPlugin.this);
        ImageViewerGui.getSingletonInstance().getObservationDialog().addUserInterface(EVESettings.OBSERVATION_UI_NAME, new ObservationDialogUIPanel());
        ImageViewerGui.getSingletonInstance().getObservationDialog().addUserInterface(EVESettings.RADIO_OBSERVATION_UI_NAME, new RadioObservationDialogUIPanel());
        // initialize database connection
        RadioPlotModel.getSingletonInstance();
        EventModel.getSingletonInstance().activateEvents();
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getSingletonInstance().getObservationDialog().removeUserInterface(EVESettings.OBSERVATION_UI_NAME, new ObservationDialogUIPanel());
        ImageViewerGui.getSingletonInstance().getObservationDialog().removeUserInterface(EVESettings.RADIO_OBSERVATION_UI_NAME, new RadioObservationDialogUIPanel());
        ImageViewerGui.getSingletonInstance().getMainContentPanel().removePlugin(this);
        ImageViewerGui.getSingletonInstance().getLeftContentPane().remove(timelinePluginPanel);
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
        description += "<p>The plugin uses <a href=\"http://db.apache.org/derby/\">Apache Derby, an Apache DB subproject</a>,<br>" + '\u00A9' + " 2011, Apache Software Foundation, <a href=\"http://www.apache.org/licenses/\">Apache License, Version 2.0</a><br>";

        return description;
    }

    @Override
    public LinkedList<JComponent> getVisualInterfaces() {
        return pluginPanes;
    }

    @Override
    public String getTabName() {
        return "1D and 2D Time Series";
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
