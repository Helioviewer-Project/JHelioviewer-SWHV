package org.helioviewer.jhv.view.uri;

import java.io.File;

import javax.annotation.Nullable;

import org.helioviewer.jhv.imagedata.ImageBuffer;

interface URIImageReader {

    record Image(@Nullable String xml, ImageBuffer buffer, int[] lut) {
    }

    Image readImage(File file) throws Exception;

    ImageBuffer readImageBuffer(File file) throws Exception;

}
