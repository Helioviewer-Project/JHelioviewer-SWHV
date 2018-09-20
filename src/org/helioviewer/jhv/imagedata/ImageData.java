package org.helioviewer.jhv.imagedata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;

public class ImageData {

    private Position viewpoint;
    private Region region;
    private MetaData metaData;
    private boolean uploaded = false;

    private final ImageBuffer imageBuffer;

    public ImageData(ImageBuffer _imageBuffer) {
        imageBuffer = _imageBuffer;
    }

    public ImageBuffer getImageBuffer() {
        return imageBuffer;
    }

    public Position getViewpoint() {
        return viewpoint;
    }

    public void setViewpoint(Position _viewpoint) {
        viewpoint = _viewpoint;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region _region) {
        region = _region;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData _metaData) {
        metaData = _metaData;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean _uploaded) {
        uploaded = _uploaded;
    }

}
