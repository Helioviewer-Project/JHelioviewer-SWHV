package org.helioviewer.jhv.imagedata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;

public class ImageData {

    private int serialNo;
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

    public int getSerial() {
        return serialNo;
    }

    public void setSerial(int _serialNo) {
        serialNo = _serialNo;
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
