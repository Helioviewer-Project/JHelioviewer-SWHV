package org.helioviewer.jhv.view.j2k;

import java.awt.Rectangle;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.APIResponse;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;
import org.helioviewer.jhv.view.j2k.image.ImageParams;
import org.helioviewer.jhv.view.j2k.image.ResolutionSet.ResolutionLevel;

public class J2KViewCallisto extends J2KView {

    public J2KViewCallisto(URI _uri, APIRequest _request, APIResponse _response) throws Exception {
        super(_uri, _request, _response);
    }

    public void setRegion(Rectangle r) {
        region = r;
    }

    private Rectangle region;

    @Override
    protected DecodeParams getDecodeParams(int serialNo, int frame, double pixFactor, double factor) {
        ResolutionLevel res = getResolutionLevel(frame, 0);
        SubImage subImage = new SubImage(region.x, region.y, region.width, region.height, res.width, res.height);
        AtomicBoolean status = cacheStatus.getFrameStatus(frame, res.level);
        return new DecodeParams(serialNo, status != null && status.get(), subImage, res, frame, factor);
    }

    @Override
    protected void signalReader(DecodeParams decodeParams) {
        reader.signalReader(new ImageParams(true, decodeParams));
    }

}
