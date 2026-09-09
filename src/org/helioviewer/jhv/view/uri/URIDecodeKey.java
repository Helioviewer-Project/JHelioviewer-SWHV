package org.helioviewer.jhv.view.uri;

import javax.annotation.Nullable;

import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.io.DataUri;

record URIDecodeKey(DataUri uri, ImageFilter.Type filter, @Nullable FITSViewState.Data fitsData) {}
