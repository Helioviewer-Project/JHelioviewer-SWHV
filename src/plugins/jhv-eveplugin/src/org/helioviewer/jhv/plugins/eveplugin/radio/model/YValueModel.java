/**
 *
 */
package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import org.helioviewer.jhv.plugins.eveplugin.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.draw.ValueSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;

/**
 * Keeps the y value information for one plot identifier.
 *
 * @author Bram.Bourgoignie@oma.be
 */
public class YValueModel implements ValueSpaceListener {

    /** The available minimum y-value. */
    private double availableYMin;

    /** The available maximum y-value. */
    private double availableYMax;

    /** The selected minimum y-value. */
    private double selectedYMin;

    /** The selected maximum y-value. */
    private double selectedYMax;

    private static YValueModel singletonInstance;

    /**
     * Constructor
     */
    private YValueModel() {

    }

    public static YValueModel getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new YValueModel();
            RadioPlotModel.getSingletonInstance().getYAxisElement().addValueSpaceListener(singletonInstance);
        }
        return singletonInstance;
    }

    /**
     * Gives the available minimum y-value.
     *
     * @return The available minimum y-value
     */
    public double getAvailableYMin() {
        return availableYMin;
    }

    /**
     * Sets the available minimum y-value.
     *
     * @param availableYMin
     *            The new available minimum y-value
     */
    public void setAvailableYMin(double availableYMin) {
        this.availableYMin = availableYMin;
        recalculateSelectedInterval();
    }

    /**
     * Gives the available maximum y-value.
     *
     * @return The new available maximum y-value
     */
    public double getAvailableYMax() {
        return availableYMax;
    }

    /**
     * Sets the available maximum y-value
     *
     * @param availableYMax
     */
    public void setAvailableYMax(double availableYMax) {
        this.availableYMax = availableYMax;
        recalculateSelectedInterval();
    }

    /**
     * Gets the selected minimum y-value.
     *
     * @return The selected minimum y-value
     */
    public double getSelectedYMin() {
        return selectedYMin;
    }

    /**
     * Sets the selected minimum y-value.
     *
     * @param selectedYMin
     *            The new selected minimum y-value
     */
    public void setSelectedYMin(double selectedYMin) {
        this.selectedYMin = selectedYMin;
    }

    /**
     * Gets the selected maximum y-value.
     *
     * @return The selected maximum y-value
     */
    public double getSelectedYMax() {
        return selectedYMax;
    }

    /**
     * Sets the selected maximum y-value.
     *
     * @param selectedYMax
     *            The new selected maximum y-value
     */
    public void setSelectedYMax(double selectedYMax) {
        this.selectedYMax = selectedYMax;
    }

    /**
     * Recalculates the selected interval based on the plot area space
     * corresponding with this y-value model.
     */
    private void recalculateSelectedInterval() {
        YAxisElement yAxisElement = RadioPlotModel.getSingletonInstance().getYAxisElement();
        Range scaledAvailableRange = yAxisElement.getScaledAvailableRange();
        Range scaledSelectedRange = yAxisElement.getScaledSelectedRange();
        double scaledDiff = scaledAvailableRange.max - scaledAvailableRange.min;
        double absDiff = availableYMax - availableYMin;
        double freqPerScaled = absDiff / scaledDiff;
        selectedYMin = (1.0 * availableYMin + (scaledSelectedRange.min - scaledAvailableRange.min) * freqPerScaled);
        selectedYMax = (1.0 * availableYMin + (scaledSelectedRange.max - scaledAvailableRange.min) * freqPerScaled);
    }

    @Override
    public void valueSpaceChanged(Range availableRange, Range selectedRange) {
        double scaledDiff = availableRange.max - availableRange.min;
        double absDiff = availableYMax - availableYMin;
        double freqPerScaled = absDiff / scaledDiff;
        selectedYMin = (1.0 * availableYMin + (selectedRange.min - availableRange.min) * freqPerScaled);
        selectedYMax = (1.0 * availableYMin + (selectedRange.max - availableRange.min) * freqPerScaled);
    }

}
