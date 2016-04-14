package org.helioviewer.jhv.plugins.eveplugin.draw;


public interface DrawControllerListener {

    public abstract void drawRequest();

    public abstract void drawMovieLineRequest(long time);

}
