package org.helioviewer.jhv.plugins.eveplugin.draw;

public interface DrawControllerListener {

    void drawRequest();

    void drawMovieLineRequest();

    void movieIntervalChanged(long start, long end);

}
