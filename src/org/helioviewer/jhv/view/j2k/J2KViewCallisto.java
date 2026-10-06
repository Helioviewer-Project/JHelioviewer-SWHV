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

    // Radio data is a single-frame JP2: decode a full-resolution pixel region at a resolution level.
    public void decodeRegion(int x, int y, int width, int height, int level) {
        ResolutionSet.Level res = getResolutionLevel(0, level);
        decode(new J2KParams.Decode(0, levelRegion(x, y, width, height, getResolutionLevel(0, 0), res), res.level()), null);
    }

    // The rectangle of a level which covers a full-resolution one, rounded to 32 pixels at full resolution.
    static J2KParams.SubImage levelRegion(int x, int y, int width, int height, ResolutionSet.Level full, ResolutionSet.Level res) {
        J2KParams.SubImage roi = new J2KParams.SubImage(x, y, width, height, full.width(), full.height());
        int shift = res.level(), round = (1 << shift) - 1;
        int x0 = roi.x() >> shift, y0 = roi.y() >> shift;
        int x1 = Math.min((roi.x() + roi.w() + round) >> shift, res.width());
        int y1 = Math.min((roi.y() + roi.h() + round) >> shift, res.height());
        return new J2KParams.SubImage(x0, y0, x1 - x0, y1 - y0);
    }

    // A pixel of a level covers 2^level source pixels, also where 380 rows do not halve evenly.
    @Override
    public ResolutionSet.Level getResolutionLevel(int frame, int level) {
        ResolutionSet.Level res = super.getResolutionLevel(frame, level);
        int factor = 1 << res.level();
        return new ResolutionSet.Level(res.level(), res.width(), res.height(), factor, factor);
    }

}
