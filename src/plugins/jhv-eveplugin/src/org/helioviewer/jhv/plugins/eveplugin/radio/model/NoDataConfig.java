package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.Color;
import java.awt.Graphics2D;

import org.helioviewer.jhv.base.interval.Interval;

/**
 * Contains the configuration of an interval with no data used in the
 * RadioPlotModel.
 *
 * @author Bram.Bourgoignie@oma.be
 */
public class NoDataConfig {

    /** The interval for which there is no data. */
    private Interval dateInterval;

    /** The drawable area map for this no data configuration. */
    private DrawableAreaMap drawableAreaMap;

    /** Is the no data configuration visible */
    private boolean visible;

    /**
     * Creates a no data configuration with a date interval, drawable area map,
     * download id and an indication the no data configuration is visible.
     *
     * @param dateInterval
     *            The date interval
     * @param drawableAreaMap
     *            The drawable area map
     * @param downloadId
     *            The download id
     * @param visible
     *            Is it visible
     */
    public NoDataConfig(Interval dateInterval, DrawableAreaMap drawableAreaMap, boolean visible) {
        this.dateInterval = dateInterval;
        this.drawableAreaMap = drawableAreaMap;
        this.visible = visible;
    }

    /**
     * The date interval.
     *
     * @return the date interval
     */
    public Interval getDateInterval() {
        return dateInterval;
    }

    /**
     * Sets the date interval.
     *
     * @param dateInterval
     *            The date interval
     */
    public void setDateInterval(Interval dateInterval) {
        this.dateInterval = dateInterval;
    }

    /**
     * The drawable area map.
     *
     * @return The drawable area map
     */
    public DrawableAreaMap getDrawableAreaMap() {
        return drawableAreaMap;
    }

    /**
     * Set the drawable area map.
     *
     * @param drawableAreaMap
     *            The drawable area map
     */
    public void setDrawableAreaMap(DrawableAreaMap drawableAreaMap) {
        this.drawableAreaMap = drawableAreaMap;
    }

    /**
     * Is the no data config visible?
     *
     * @return Is the no data config visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets the no data config visible. True is the no data config is visible,
     * false if the no data config is not visible.
     *
     * @param visible
     *            True is the no data config is visible, false if the no data
     *            config is not visible.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Draws the no data configuration on the given Graphics.
     *
     * @param g
     *            The Graphics on which the no data configuration should be
     *            drawn
     */
    public void draw(Graphics2D g) {
        if (visible) {
            // Log.trace("Visible and drawable area map : " +
            // drawableAreaMap.toString());
            int spaceWidth = drawableAreaMap.getDestinationX1() - drawableAreaMap.getDestinationX0();
            int spaceHeight = drawableAreaMap.getDestinationY1() - drawableAreaMap.getDestinationY0();
            g.setColor(Color.GRAY);
            g.fillRect(drawableAreaMap.getDestinationX0(), drawableAreaMap.getDestinationY0(), spaceWidth, spaceHeight);
            g.setColor(Color.white);
            String text = "No data available";
            final int textWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
            final int textHeight = (int) g.getFontMetrics().getStringBounds(text, g).getHeight();
            final int x1 = drawableAreaMap.getDestinationX0() + spaceWidth / 2 - textWidth / 2;
            final int y1 = spaceHeight / 2 - textHeight / 2;
            g.drawString(text, x1, y1);
        }
    }

}
