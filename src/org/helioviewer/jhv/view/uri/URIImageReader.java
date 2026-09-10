package org.helioviewer.jhv.view.uri;

import java.io.File;

import javax.annotation.Nullable;

import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.view.ClipSet;

interface URIImageReader {

    record Info(@Nullable String xml, int width, int height, @Nullable LUT lut, @Nullable ClipSet clipSet) {}

    Info readInfo(File file) throws Exception;

    ImageBuffer decode(File file, ImageFilter filter, @Nullable ClipSet.Range clipRange) throws Exception;

}
