package org.helioviewer.jhv.view.uri;

import java.io.File;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageFilter;

interface URIImageReader {

    record Image(@Nullable String xml, ImageBuffer buffer, @Nullable LUT lut) {
    }

    Image readImage(File file) throws Exception;

    ImageBuffer readImageBuffer(File file, ImageFilter.Type filterType) throws Exception;

}
