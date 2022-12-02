package org.helioviewer.jhv.view.uri;

import java.net.URI;

import javax.annotation.Nullable;

import org.helioviewer.jhv.imagedata.ImageBuffer;

interface URIImageReader {

    record Image(@Nullable String xml, ImageBuffer buffer) {
    }

    Image readImage(URI uri) throws Exception;

    ImageBuffer readImageBuffer(URI uri) throws Exception;

}
