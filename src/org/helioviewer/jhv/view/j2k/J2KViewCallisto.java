package org.helioviewer.jhv.view.j2k;

import java.awt.Rectangle;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;
import org.helioviewer.jhv.view.j2k.image.ReadParams;
import org.helioviewer.jhv.view.j2k.image.ResolutionSet.ResolutionLevel;

public class J2KViewCallisto extends J2KView {

    public J2KViewCallisto(APIRequest _request, URI _uri, J2KExecutor _executor) throws Exception {
        super(_request, null, _uri, _executor);
    }

    public void setRegion(Rectangle r) {
        region = r;
    }

    private Rectangle region;

    @Override
    protected DecodeParams getDecodeParams(Position viewpoint, int frame, double pixFactor, double factor) {
        ResolutionLevel res = getResolutionLevel(frame, 0);
        SubImage subImage = new SubImage(region.x, region.y, region.width, region.height, res.width, res.height);
        AtomicBoolean status = cacheStatus.getFrameStatus(frame, res.level);
        return new DecodeParams(this, viewpoint, status != null && status.get(), subImage, res, frame, factor);
    }

    @Override
    protected void signalReader(DecodeParams decodeParams) { // not used
        reader.signalReader(new ReadParams(true, decodeParams));
    }

}
