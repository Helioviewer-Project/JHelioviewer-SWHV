package org.helioviewer.jhv.plugins.eveplugin.draw;

public interface DrawControllerListener {

    public abstract void drawRequest();

    public abstract void drawMovieLineRequest(long time);

    public abstract void movieIntervalChanged(long start, long end);

}
