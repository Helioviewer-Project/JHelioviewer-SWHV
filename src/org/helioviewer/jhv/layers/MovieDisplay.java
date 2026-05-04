package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.events.JHVEventListener;
import org.helioviewer.jhv.events.JHVRelatedEvents;

public class MovieDisplay implements JHVEventListener.Highlight {

    private static final MovieDisplay instance = new MovieDisplay();
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

    @Override
    public void highlightChanged() {
        display();
    }

    private MovieDisplay() {
        JHVRelatedEvents.addHighlightListener(this);
    }

}
