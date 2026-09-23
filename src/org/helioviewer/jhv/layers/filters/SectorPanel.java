package org.helioviewer.jhv.layers.filters;

import org.helioviewer.jhv.image.ImageDisplaySettings;
import org.helioviewer.jhv.layers.ImageLayer;

public final class SectorPanel {

    private final FilterDetails directionDetails;
    private final FilterDetails widthDetails;

    public SectorPanel(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        directionDetails = SliderFilterPanel.create("Sector", -180, 180, settings.getSectorCenter(), 1, SectorPanel::formatDegree,
                value -> settings.setSector(value, settings.getSectorWidth()));
        widthDetails = SliderFilterPanel.create("Opening", 0, 360, settings.getSectorWidth(), 1, SectorPanel::formatDegree,
                value -> settings.setSector(settings.getSectorCenter(), value));
    }

    public FilterDetails getDirectionDetails() {
        return directionDetails;
    }

    public FilterDetails getWidthDetails() {
        return widthDetails;
    }

    public void setVisible(boolean visible) {
        directionDetails.setVisible(visible);
        widthDetails.setVisible(visible);
    }

    private static String formatDegree(double angle) {
        return String.format("%.0f°", angle);
    }

}
