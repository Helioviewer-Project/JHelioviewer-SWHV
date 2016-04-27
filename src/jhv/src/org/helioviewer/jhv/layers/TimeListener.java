package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.base.time.JHVDate;

public interface TimeListener {

    public abstract void timeChanged(JHVDate time);

}
