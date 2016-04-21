package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;

/**
 * Radio specific implementation of y-axis element. This implementation
 * overrides the getSelectedRange() and getAvailableRange() in order to use the
 * YValueModel to get the correct values.
 *
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class RadioYAxis extends YAxis {

    /**
     * Creates a radio y-axis element based on the given selected range,
     * available range, label, minimum value, maximum value, a color and a plot
     * identifier.
     *
     * @param selectedRange
     *            The selected range of the radio y-axis element
     * @param availableRange
     *            The available range of the radio y-axis element
     * @param label
     *            The label corresponding with the radio y-axis element
     */
    public RadioYAxis(String label, boolean isLogScale) {
        super(new Range(), label, isLogScale);
    }

    @Override
    public void setSelectedRange(Range newScaledSelectedRange) {
        selectedRange = newScaledSelectedRange;
        fireSelectedRangeChanged();
    }

}
