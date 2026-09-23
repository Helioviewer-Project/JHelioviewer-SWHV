package org.helioviewer.jhv.view.j2k;

import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.thread.LatestWorker;

public class J2KViewCallisto extends J2KView {

    public J2KViewCallisto(LatestWorker<DecodedImage> _executor, APIRequest _request, DataUri _dataUri, ImageProcessingSettings _processingSettings) throws Exception {
        super(_executor, _request, _dataUri, _processingSettings);
    }

    // Radio data is a single-frame JP2: decode the given region of the full-resolution level.
    public void decodeRegion(int x, int y, int width, int height, float factor) {
        ResolutionSet.Level res = getResolutionLevel(0, 0);
        J2KParams.SubImage subImage = new J2KParams.SubImage(x, y, width, height, res.width(), res.height());
        decode(new J2KParams.Decode(0, subImage, res.level(), factor), null);
    }

}
