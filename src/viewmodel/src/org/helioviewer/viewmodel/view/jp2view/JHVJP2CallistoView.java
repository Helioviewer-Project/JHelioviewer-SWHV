package org.helioviewer.viewmodel.view.jp2view;

import java.awt.Dimension;
import java.util.Date;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.jp2view.J2KRender.RenderReasons;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.viewmodel.viewport.Viewport;

public class JHVJP2CallistoView extends JHVJP2View {

    public JHVJP2CallistoView(boolean isMainView, Interval<Date> range) {
        super(isMainView, range);
    }

    @Override
    public boolean setViewport(Viewport v, ChangeEvent event) {
        Log.debug("Set viewport: " + v);
        // Thread.dumpStack();
        boolean viewportChanged = (viewport == null ? v == null : !viewport.equals(v));
        viewport = v;
        if (setImageViewParams(calculateParameter())) {
            // sub image data will change because resolution level changed
            // -> memorize change event till sub image data has changed

            this.event.copyFrom(event);

            this.event.addReason(new ViewportChangedReason(this, v));

            return true;

        } else if (viewportChanged && imageViewParams.resolution.getZoomLevel() == jp2Image.getResolutionSet().getMaxResolutionLevels()) {

            this.event.copyFrom(event);

            this.event.addReason(new ViewportChangedReason(this, v));
            renderRequestedSignal.signal(RenderReasons.OTHER);

            return true;
        }

        return viewportChanged;

    }

    @Override
    public boolean setRegion(Region r, ChangeEvent event) {
        // Log.debug("Set region : " + r);
        // Thread.dumpStack();
        boolean changed = region == null ? r == null : !region.equals(r);
        region = r;
        changed |= setImageViewParams(calculateParameter());
        this.event.copyFrom(event);
        return changed;
    }

    @Override
    protected JP2ImageParameter calculateParameter() {
        int maxHeight = jp2Image.getResolutionSet().getResolutionLevel(0).getResolutionBounds().height;
        int maxWidth = jp2Image.getResolutionSet().getResolutionLevel(0).getResolutionBounds().width;
        Thread.dumpStack();
        Log.debug("Available dimension : " + new Dimension((int) Math.ceil(viewport.getWidth() / region.getWidth() * maxWidth), 2 * (int) Math.ceil(viewport.getHeight() / region.getHeight() * maxHeight)));
        ResolutionLevel res = jp2Image.getResolutionSet().getClosestResolutionLevel(new Dimension((int) Math.ceil(viewport.getWidth() / region.getWidth() * maxWidth), 2 * (int) Math.ceil(viewport.getHeight() / region.getHeight() * maxHeight)));

        SubImage subImage = new SubImage((int) (1.0 * region.getCornerX() / maxWidth * res.getResolutionBounds().width), (int) (1.0 * region.getCornerY() / maxHeight * res.getResolutionBounds().height), (int) (1.0 * region.getWidth() / maxWidth * res.getResolutionBounds().width), (int) (1.0 * region.getHeight() / maxHeight * res.getResolutionBounds().height));
        // subImageBuffer.putSubImage(subImage, region);

        Log.debug("SubImage : " + subImage + ", resolutionLevel: " + res + " quality layers " + getCurrentNumQualityLayers());

        return new JP2ImageParameter(subImage, res, getCurrentNumQualityLayers(), 0);
    }
}
