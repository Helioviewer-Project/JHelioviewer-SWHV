package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.Log;

public class MovieDisplay {

    private static boolean missingRenderRequesterLogged;
    private static Runnable requestRender = MovieDisplay::missingRenderRequester;

    public static void render(float decodeFactor) {
        if (ImageLayers.areEnabled())
            ImageLayers.decode(decodeFactor);
        else
            display();
    }

    public static void display() {
        requestRender.run();
    }

    public static void setRenderRequester(Runnable _requestRender) {
        requestRender = _requestRender;
    }

    private static void missingRenderRequester() {
        if (missingRenderRequesterLogged)
            return;
        missingRenderRequesterLogged = true;
        Log.warn("No render requester installed");
    }

}
