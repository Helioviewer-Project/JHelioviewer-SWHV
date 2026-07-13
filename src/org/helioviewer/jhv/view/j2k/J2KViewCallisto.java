package org.helioviewer.jhv.view.j2k;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.thread.LatestWorker;

public class J2KViewCallisto extends J2KView {

    public J2KViewCallisto(LatestWorker<DecodedImage> _executor, APIRequest _request, DataUri _dataUri) throws Exception {
        super(_executor, _request, _dataUri);
    }

    public void setDecodeRegion(int x, int y, int width, int height) {
        region = new DecodeRegion(x, y, width, height);
    }

    private record DecodeRegion(int x, int y, int width, int height) {}

    private DecodeRegion region;

    @Override
    protected J2KParams.Decode getDecodeParams(int frame, double pixFactor, float factor) {
        ResolutionSet.Level res = getResolutionLevel(frame, 0);
        J2KParams.SubImage subImage = new J2KParams.SubImage(region.x(), region.y(), region.width(), region.height(), res.width(), res.height());
        return new J2KParams.Decode(frame, subImage, res.level(), factor);
    }

    @Override
    protected void signalReader(J2KParams.Decode decodeParams, Position viewpoint) { // not used
        Log.warn("J2KViewCallisto.signalReader called: should not happen");
        //reader.signalReader(new ReadParams(this, decodeParams, true));
    }

}
