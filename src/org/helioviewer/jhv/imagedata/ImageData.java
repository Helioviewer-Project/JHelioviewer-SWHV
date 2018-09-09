package org.helioviewer.jhv.imagedata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;

public class ImageData {

    private Region region;
    private MetaData metaData;
    private Position viewpoint;
    private boolean uploaded = false;

    private final ImageBuffer imageBuffer;

    public ImageData(ImageBuffer _imageBuffer) {
        imageBuffer = _imageBuffer;
    }

    public ImageBuffer getImageBuffer() {
        return imageBuffer;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region r) {
        region = r;
    }

    public void setMetaData(MetaData m) {
        metaData = m;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setViewpoint(Position p) {
        viewpoint = p;
    }

    public Position getViewpoint() {
        return viewpoint;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean _uploaded) {
        uploaded = _uploaded;
    }

}
