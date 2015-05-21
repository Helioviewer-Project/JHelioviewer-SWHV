package org.helioviewer.jhv.plugins.eveplugin;

import java.net.URL;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.SwingWorker;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;
import org.helioviewer.jhv.plugin.interfaces.Plugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeIntervalLockModel;
import org.helioviewer.jhv.plugins.eveplugin.events.data.EventRequester;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.jhv.plugins.eveplugin.lines.model.EVEDrawController;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.RadioPlotModel;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandTypeAPI;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.plugins.eveplugin.view.ObservationDialogUIPanel;
import org.helioviewer.jhv.plugins.eveplugin.view.RadioObservationDialogUIPanel;
import org.helioviewer.jhv.plugins.eveplugin.view.chart.PlotPanel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDateSelectorTablePanel;

/**
 *
 *
 * @author Stephan Pagel
 * */
public class EVEPlugin implements Plugin, MainContentPanelPlugin {

    private final LinkedList<JComponent> pluginPanes = new LinkedList<JComponent>();
    private final PlotPanel plotOne = new PlotPanel();
    private final LineDateSelectorTablePanel timelinePluginPanel = new LineDateSelectorTablePanel();

    @Override
    public void installPlugin() {
        SwingWorker<Void, Void> installPlugin = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                // call BandType API in background => loads the datasets.
                BandTypeAPI.getSingletonInstance();
                return null;
            }

            @Override
            public void done() {
                EventRequester eventRequester = EventRequester.getSingletonInstance();
                DrawController.getSingletonInstance().addTimingListener(eventRequester);
                eventRequester.addListener(EventModel.getSingletonInstance());
                DrawController.getSingletonInstance().addTimingListener(EventModel.getSingletonInstance());
                DrawController.getSingletonInstance().setAvailableInterval(new Interval<Date>(new Date(), new Date()));
                // Create an instance of eveDrawController and leave it here.
                EVEDrawController.getSingletonInstance();
                // Avoid concurrent modification error.
                TimeIntervalLockModel.getInstance();
                pluginPanes.add(plotOne);

                ImageViewerGui.getLeftContentPane().add("Timeline Layers", timelinePluginPanel, true);
                ImageViewerGui.getLeftContentPane().revalidate();

                ImageViewerGui.getMainContentPanel().addPlugin(EVEPlugin.this);
                ImageViewerGui.getMainContentPanel().revalidate();

                ImageViewerGui.getObservationDialog().addUserInterface(EVESettings.OBSERVATION_UI_NAME, new ObservationDialogUIPanel());
                ImageViewerGui.getObservationDialog().addUserInterface(EVESettings.RADIO_OBSERVATION_UI_NAME, new RadioObservationDialogUIPanel());

                RadioPlotModel.getSingletonInstance();
                EventModel.getSingletonInstance().activateEvents();
            }
        };
        installPlugin.execute();
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getObservationDialog().removeUserInterface(EVESettings.OBSERVATION_UI_NAME, new ObservationDialogUIPanel());
        ImageViewerGui.getObservationDialog().removeUserInterface(EVESettings.RADIO_OBSERVATION_UI_NAME, new RadioObservationDialogUIPanel());
        ImageViewerGui.getMainContentPanel().removePlugin(this);
        ImageViewerGui.getLeftContentPane().remove(timelinePluginPanel);
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
    }

    @Override
    public String getState() {
        return "";
    }

}
