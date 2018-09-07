package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;

public class ImageData {

    public enum ImageFormat {
        Gray8(1), Gray16(2), ARGB32(4);

        public final int bytes;

        ImageFormat(int _bytes) {
            bytes = _bytes;
        }
    }

    private final int width;
    private final int height;
    private final ImageFormat format;
    private final Buffer buffer;
    private final int texID;

    private Region region;
    private MetaData metaData;
    private Position viewpoint;

    public ImageData(int _width, int _height, ImageFormat _format, Buffer _buffer) {
        width = _width;
        height = _height;
        format = _format;
        buffer = _buffer;
        texID = 0;
    }

    public ImageData(ImageBuffer buf) {
        width = buf.width;
        height = buf.height;
        format = buf.format;
        buffer = buf.buffer;
        texID = buf.texID;
    }

    public int getTexID() {
        return texID;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public ImageFormat getImageFormat() {
        return format;
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

}
