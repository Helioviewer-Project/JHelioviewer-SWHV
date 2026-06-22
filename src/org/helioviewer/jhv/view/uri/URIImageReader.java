package org.helioviewer.jhv.view.uri;

import java.io.File;

import javax.annotation.Nullable;

import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;

interface URIImageReader {

    record Image(@Nullable String xml, ImageBuffer buffer, @Nullable LUT lut) {}

    Image readImage(File file) throws Exception;

    // clip: optional fixed [min, max] display range; when non-null the FITS reader normalizes to
    // it instead of per-frame auto, so a multi-frame layer's frames don't strobe. Non-FITS ignore.
    ImageBuffer readImageBuffer(File file, ImageFilter filter, @Nullable float[] clip) throws Exception;

}
