package org.helioviewer.jhv.plugins.eveplugin.events.gui;

import java.awt.Color;
import java.awt.Component;

import org.helioviewer.jhv.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

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
        return "SWEK Events";
    }

    @Override
    public Color getDataColor() {
        return null;
    }

    @Override
    public void setDataColor(Color c) {
    }

    @Override
    public boolean isDownloading() {
        return false;
    }

    @Override
    public String getUnitLabel() {
        return "";
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public boolean hasData() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

}
