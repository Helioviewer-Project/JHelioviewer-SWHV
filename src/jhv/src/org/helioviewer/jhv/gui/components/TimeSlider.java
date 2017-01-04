package org.helioviewer.jhv.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * Extension of JSlider displaying the caching status on the track.
 *
 * This element provides its own look and feel. Therefore, it is independent
 * from the global look and feel.
 */
@SuppressWarnings("serial")
class TimeSlider extends JSlider implements LazyComponent {

    public TimeSlider(int orientation, int min, int max, int value) {
        super(orientation, min, max, value);
        setUI(new TimeSliderUI(this));
    }

    // Overrides updateUI, to keep own SliderUI
    @Override
    public void updateUI() {
    }

    @Override
    public void repaint() {
        dirty = true;
    }

    @Override
    public void repaint(int x, int y, int width, int height) {
        dirty = true;
    }

    private boolean dirty = false;

    @Override
    public void lazyRepaint() {
        if (dirty) {
            super.repaint();
            dirty = false;
        }
    }

    /**
     * Extension of BasicSliderUI overriding some drawing functions.
     * All functions for size calculations stay the same.
     */
    private static class TimeSliderUI extends BasicSliderUI {

        private static final Color notCachedColor = Color.LIGHT_GRAY;
        private static final Color partialCachedColor = new Color(0x8080FF);
        private static final Color completeCachedColor = new Color(0x4040FF);

        private static final BasicStroke thickStroke = new BasicStroke(4);

        public TimeSliderUI(JSlider component) {
            super(component);
        }

        @Override
        protected TrackListener createTrackListener(JSlider slider) {
            return new TimeTrackListener();
        }

        @Override
        protected void scrollDueToClickInTrack(int dir) {
            if (trackListener instanceof TimeTrackListener)
                slider.setValue(valueForXPosition(((TimeTrackListener) trackListener).getCurrentX()));
        }

        @Override
        public void paintThumb(Graphics g) {
            g.setColor(Color.BLACK);
            g.drawRect(thumbRect.x, thumbRect.y, thumbRect.width - 1, thumbRect.height - 1);

            int x = thumbRect.x + (thumbRect.width - 1) / 2;
            g.drawLine(x, thumbRect.y, x, thumbRect.y + thumbRect.height - 1);
        }

        // Draws the different region (no/partial/complete information
        @Override
        public void paintTrack(Graphics g1) {
            Graphics2D g = (Graphics2D) g1.create();
            g.setStroke(thickStroke);

            int y = slider.getSize().height / 2;
            View view = Layers.getActiveView();
            if (view == null) {
                g.setColor(notCachedColor);
                g.drawLine(trackRect.x, y, trackRect.x + trackRect.width, y);
            } else {
                int len = view.getMaximumFrameNumber();
                for (int i = 0; i < len; i++) {
                    int begin = (int) ((float) i / len * trackRect.width);
                    int end = (int) ((float) (i + 1) / len * trackRect.width);

                    if (end == begin)
                        end++;

                    AtomicBoolean status = view.getImageCacheStatus(i);
                    if (status == null)
                        g.setColor(notCachedColor);
                    else {
                        boolean complete = status.get();
                        if (complete)
                            g.setColor(completeCachedColor);
                        else
                            g.setColor(partialCachedColor);
                    }
                    g.drawLine(trackRect.x + begin, y, trackRect.x + end, y);
                }
            }
            g.dispose();
        }

        // Overrides the track listener to access currentX
        private class TimeTrackListener extends TrackListener {
            public int getCurrentX() {
                return currentMouseX;
            }
        }
    }
}
