package org.helioviewer.jhv.layers.filters;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.opengl.GLImage;

public final class SectorPanel {

    private final FilterDetails directionDetails;
    private final FilterDetails widthDetails;
    private int direction;
    private int width;

    public SectorPanel(ImageLayer layer) {
        GLImage image = layer.getGLImage();
        direction = (int) Math.round(image.getSectorCenter());
        width = (int) Math.round(image.getSectorWidth());
        directionDetails = SliderFilterPanel.create("Sector", -180, 180, direction, SectorPanel::formatDegree, value -> {
            direction = value;
            image.setSector(direction, width);
        });
        widthDetails = SliderFilterPanel.create("Opening", 0, 360, width, SectorPanel::formatDegree, value -> {
            width = value;
            image.setSector(direction, width);
        });
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

    private static String formatDegree(int angle) {
        return angle + "°";
    }

}
