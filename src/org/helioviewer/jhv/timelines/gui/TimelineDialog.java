package org.helioviewer.jhv.timelines.gui;

import java.awt.Window;
import java.util.Map;

import org.helioviewer.jhv.gui.dialog.ScreenDialog;
import org.helioviewer.jhv.timelines.band.BandDataset;
import org.helioviewer.jhv.ui.TimelineScreen;

@SuppressWarnings("serial")
public final class TimelineDialog extends ScreenDialog {

    private final TimelineScreen screen;

    public TimelineDialog() {
        this(new TimelineScreen(null));
    }

    private TimelineDialog(TimelineScreen _screen) {
        super(_screen, false);
        screen = _screen;
        setType(Window.Type.UTILITY);
    }

    public void setCatalogs(Map<String, BandDataset[]> catalogs) {
        screen.setCatalogs(catalogs);
    }

}
