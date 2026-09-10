package org.helioviewer.jhv.view.uri;

import java.io.File;

import javax.annotation.Nullable;

import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.view.ClipSet;

interface URIImageReader {

    record Image(@Nullable String xml, ImageBuffer buffer, @Nullable LUT lut, @Nullable ClipSet clipSet) {}

    Image readImage(File file) throws Exception;

    ImageBuffer readImageBuffer(File file, ImageFilter filter, @Nullable ClipSet clipSet) throws Exception;

}
