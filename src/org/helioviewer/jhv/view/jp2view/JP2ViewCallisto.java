package org.helioviewer.jhv.view.jp2view;

import java.awt.Rectangle;
import java.net.URI;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.APIResponse;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.view.jp2view.image.ResolutionSet.ResolutionLevel;

public class JP2ViewCallisto extends JP2View {

    public JP2ViewCallisto(URI _uri, APIRequest _req, APIResponse _res) throws Exception {
        super(_uri, _req, _res);
    }

    public void setRegion(Rectangle r) {
        region = r;
    }

    private Rectangle region;

    @Override
    ImageParams calculateParams(Camera camera, Viewport vp, int frame, double factor) {
        ResolutionLevel res = getResolutionLevel(frame, 0);
        SubImage subImage = new SubImage(region.x, region.y, region.width, region.height, res.width, res.height);

        ImageParams params = new ImageParams(null, subImage, res, frame, factor, true);
        signalReader(params);

        return params;
    }

}
