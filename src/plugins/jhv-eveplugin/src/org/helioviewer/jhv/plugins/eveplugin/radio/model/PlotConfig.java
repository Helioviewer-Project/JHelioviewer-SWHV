package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class PlotConfig {

    private BufferedImage image;
    private final DrawableAreaMap map;
    private boolean visible;
    private long downloadID;
    private long imageId;

    public PlotConfig(BufferedImage image, DrawableAreaMap map, boolean visible, long downloadID, long imageID) {
        super();
        this.image = image;
        this.map = map;
        this.visible = visible;
        this.downloadID = downloadID;
        imageId = imageID;
    }

    public void draw(Graphics2D g) {
        if (visible) {
            // Thread.dumpStack();
            // Log.trace("Draw image on : " + map);
            g.drawImage(image, map.getDestinationX0(), map.getDestinationY0(), map.getDestinationX1(), map.getDestinationY1(), map.getSourceX0(), map.getSourceY0(), map.getSourceX1(), map.getSourceY1(), null);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getDrawWidth() {
        return map.getDestinationX1() - map.getDestinationX0();
    }

    public long getDownloadID() {
        return downloadID;
    }

    public void setDownloadID(long downloadID) {
        this.downloadID = downloadID;
    }

    public long getImageId() {
        return imageId;
    }

    public void setImageId(long imageId) {
        this.imageId = imageId;
    }

    public void setBufferedImage(BufferedImage bufferedImage) {
        image = bufferedImage;
    }

}
