package org.helioviewer.jhv.view.j2k;

import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;
import org.helioviewer.jhv.view.j2k.image.SubImage;

public class J2KViewCallisto extends J2KView {

    public J2KViewCallisto(DecodeExecutor _executor, APIRequest _request, DataUri _dataUri) throws Exception {
        super(_executor, _request, _dataUri);
    }

    public void setRegion(Rectangle r) {
        region = r;
    }

    private Rectangle region;

    @Override
    protected DecodeParams getDecodeParams(Position viewpoint, int frame, double pixFactor, float factor) {
        ResolutionSet.Level res = getResolutionLevel(frame, 0);
        SubImage subImage = new SubImage(region.x, region.y, region.width, region.height, res.width, res.height);
        AtomicBoolean status = completionLevel.getFrameStatus(frame, res.level);
        return new DecodeParams(serial, frame, subImage, res.level, factor, status != null && status.get(), viewpoint);
    }

    @Override
    protected void signalReader(DecodeParams decodeParams) { // not used
        Log.warn("J2KViewCallisto.signalReader called: should not happen");
        //reader.signalReader(new ReadParams(this, decodeParams, true));
    }

}
