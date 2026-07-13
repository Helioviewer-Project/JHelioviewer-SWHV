package org.helioviewer.jhv.image;

import org.helioviewer.jhv.metadata.Region;

public record DecodedImage(ImageBuffer imageBuffer, Region region) {}
