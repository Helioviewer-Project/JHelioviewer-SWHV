package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.display.Display;

public class MovieDisplay {

    private static boolean missingHandlerLogged;
    private static RenderRequestHandler renderRequestHandler = _ -> missingRenderRequester();

    public static void render(float decodeFactor) {
        if (ImageLayers.areEnabled())
            ImageLayers.decode(decodeFactor);
        else
            display();
    }

    public static void display() {
        renderRequestHandler.requestRender(Display.getCamera().getViewpoint());
    }

    public static void setRenderRequestHandler(RenderRequestHandler _renderRequestHandler) {
        renderRequestHandler = _renderRequestHandler;
    }

    private static void missingRenderRequester() {
        if (missingHandlerLogged)
            return;
        missingHandlerLogged = true;
        Log.warn("No render request handler installed");
    }

}
