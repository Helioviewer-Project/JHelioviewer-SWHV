package org.helioviewer.jhv.view.jp2view;

import java.awt.Rectangle;
import java.net.URI;

import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.APIResponse;
import org.helioviewer.jhv.view.jp2view.image.DecodeParams;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.view.jp2view.image.ResolutionSet.ResolutionLevel;

public class J2KViewCallisto extends J2KView {

    public J2KViewCallisto(URI _uri, APIRequest _request, APIResponse _response) throws Exception {
        super(_uri, _request, _response);
    }

    public void setRegion(Rectangle r) {
        region = r;
    }

    private Rectangle region;

    @Override
    ImageParams calculateParams(int serialNo, int frame, double pixFactor, double factor) {
        ResolutionLevel res = getResolutionLevel(frame, 0);
        SubImage subImage = new SubImage(region.x, region.y, region.width, region.height, res.width, res.height);

        ImageParams params = new ImageParams(true, serialNo, new DecodeParams(subImage, res, frame, factor));
        signalReader(params);

        return params;
    }

}
