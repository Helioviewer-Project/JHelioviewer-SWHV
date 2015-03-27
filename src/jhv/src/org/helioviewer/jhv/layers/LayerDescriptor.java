package org.helioviewer.jhv.layers;

/**
 * Class representing the current state of a layer
 *
 * @author Malte Nuhn
 * @author Freek Verstringe
 */
public class LayerDescriptor {

    public boolean isMaster = false;
    public boolean isMovie = false;
    public boolean isVisible = false;
    public boolean isTimed = false;

    public String title = "";
    public String timestamp = "";

}
