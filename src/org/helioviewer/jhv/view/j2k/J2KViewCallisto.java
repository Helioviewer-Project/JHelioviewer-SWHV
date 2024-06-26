package org.helioviewer.jhv.view.j2k;

import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.view.DecodeExecutor;

public class J2KViewCallisto extends J2KView {

    public J2KViewCallisto(DecodeExecutor _executor, APIRequest _request, DataUri _dataUri) throws Exception {
        super(_executor, _request, _dataUri);
    }

    public void setRegion(Rectangle r) {
        region = r;
    }

    private Rectangle region;

    @Override
    protected J2KParams.Decode getDecodeParams(Position viewpoint, int frame, double pixFactor, float factor) {
        ResolutionSet.Level res = getResolutionLevel(frame, 0);
        SubImage subImage = new SubImage(region.x, region.y, region.width, region.height, res.width, res.height);
        AtomicBoolean status = completionLevel.getFrameStatus(frame, res.level);
        return new J2KParams.Decode(serial, frame, subImage, res.level, factor, status != null && status.get(), viewpoint);
    }

    @Override
    protected void signalReader(J2KParams.Decode decodeParams) { // not used
        Log.warn("J2KViewCallisto.signalReader called: should not happen");
        //reader.signalReader(new ReadParams(this, decodeParams, true));
    }

}
