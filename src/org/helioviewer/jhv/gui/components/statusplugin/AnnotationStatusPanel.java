package org.helioviewer.jhv.gui.components.statusplugin;

import org.helioviewer.jhv.gui.components.StatusPanel;

@SuppressWarnings("serial")
public class AnnotationStatusPanel extends StatusPanel.StatusPlugin {

    private static final String nullData = "--";

    public AnnotationStatusPanel() {
        update(nullData);
    }

    public void update(String data) {
        setText(String.format("Annotation: %s |", data));
    }

}
