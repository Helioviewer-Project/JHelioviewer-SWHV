package org.helioviewer.jhv.view.uri;

import java.net.URI;

import javax.annotation.Nullable;

import org.helioviewer.jhv.imagedata.ImageBuffer;

interface URIImageReader {

    @Nullable
    String readXML(URI uri) throws Exception;

    @Nullable
    ImageBuffer readImageBuffer(URI uri) throws Exception;

}
