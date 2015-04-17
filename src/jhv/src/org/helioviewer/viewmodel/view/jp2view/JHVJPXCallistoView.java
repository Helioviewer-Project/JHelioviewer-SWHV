package org.helioviewer.viewmodel.view.jp2view;

import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.viewport.Viewport;

public class JHVJPXCallistoView extends JHVJPXView {

    public JHVJPXCallistoView(boolean isMainView, Interval<Date> range) {
        super(isMainView, range);
    }

    public JHVJPXCallistoView(boolean isMainView, Interval<Date> range, boolean b) {
        super(isMainView, range, b);
    }

    @Override
    public boolean setViewport(Viewport v, ChangeEvent event) {
        // TODO Auto-generated method stub
        return super.setViewport(v, event);
    }

    @Override
    public boolean setRegion(Region r, ChangeEvent event) {
        // TODO Auto-generated method stub
        return super.setRegion(r, event);
    }
}
