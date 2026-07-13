package org.helioviewer.jhv.view.uri;

import java.io.File;

import javax.annotation.Nullable;

import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;

interface URIImageReader {

    record Image(@Nullable String xml, ImageBuffer buffer, @Nullable LUT lut) {}

    Image readImage(File file) throws Exception;

    ImageBuffer readImageBuffer(File file, ImageFilter filter) throws Exception;

}
