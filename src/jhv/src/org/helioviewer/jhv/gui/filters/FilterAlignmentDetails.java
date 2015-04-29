package org.helioviewer.jhv.gui.filters;

import java.awt.Component;

/**
 * Interface to provide positioning information in the compact-panel
 *
 * @author mnuhn
 */
public interface FilterAlignmentDetails {

    public Component getTitle();

    public Component getSlider();

    public Component getValue();

}
