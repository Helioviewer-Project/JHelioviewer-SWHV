package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.util.Date;

public interface DrawControllerListener {

    public abstract void drawRequest();

    public abstract void drawMovieLineRequest(Date time);

}
