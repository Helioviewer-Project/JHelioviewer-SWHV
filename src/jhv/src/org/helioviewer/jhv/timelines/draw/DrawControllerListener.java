package org.helioviewer.jhv.timelines.draw;

public interface DrawControllerListener {

    void drawRequest();

    void drawMovieLineRequest();

    void movieIntervalChanged(long start, long end);

}
