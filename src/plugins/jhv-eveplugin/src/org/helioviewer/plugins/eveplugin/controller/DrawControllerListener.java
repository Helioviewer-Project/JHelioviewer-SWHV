package org.helioviewer.plugins.eveplugin.controller;

import java.util.Date;

public interface DrawControllerListener {
    public abstract void drawRequest();

    public abstract void drawMovieLineRequest(Date time);
}
