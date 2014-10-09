package org.helioviewer.plugins.eveplugin.events.gui;

import java.awt.Color;

import org.helioviewer.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class EventsSelectorElement implements LineDataSelectorElement {

    private final EventModel eventModel;

    public EventsSelectorElement(EventModel eventModel) {
        this.eventModel = eventModel;
    }

    @Override
    public void removeLineData() {
        eventModel.deactivateEvents();

    }

    @Override
    public void setVisibility(boolean visible) {
        eventModel.setEventsVisible(visible);

    }

    @Override
    public boolean isVisible() {
        return eventModel.isEventsVisible();
    }

    @Override
    public String getName() {
        return "Events";
    }

    @Override
    public Color getDataColor() {
        return Color.black;
    }

    @Override
    public void setDataColor(Color c) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDownloading() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getPlotIdentifier() {
        return eventModel.getPlotIdentifier();
    }

    @Override
    public void setPlotIndentifier(String plotIdentifier) {
        eventModel.setPlotIdentifier(plotIdentifier);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getUnitLabel() {
        return "";
    }

}
