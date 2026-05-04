package org.helioviewer.jhv.layers;

public class MovieDisplay {

    private static Runnable requester = () -> {};

    public static void render(float decodeFactor) {
        if (ImageLayers.areEnabled())
            ImageLayers.decode(decodeFactor);
        else
            display();
    }

    public static void display() {
        requester.run();
    }

    public static void setRequester(Runnable _requester) {
        requester = _requester;
    }

}
