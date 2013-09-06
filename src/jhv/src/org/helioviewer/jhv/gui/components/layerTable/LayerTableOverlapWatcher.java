package org.helioviewer.jhv.gui.components.layerTable;

import java.util.Date;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * This class performs checks if the added layers fit the rest of the already
 * added layers (concerning the timespan covered) and notifies the user if he
 * might have made a bad decision.
 * 
 * @author mnuhn
 */
public class LayerTableOverlapWatcher implements LayersListener {

    /**
     * @see LayerTableOverlapWatcher#layerAdded
     * 
     *      Parameter for determining partially overlapping (with respect to
     *      time) layers
     * 
     *      Value must be between 0.0 and 1.0
     */
    public final static double smallestValidCoverageFraction = 0.7;

    /**
     * 
     * {@inheritDoc}
     * 
     * This implementation performs checks if the newly added layer fits the
     * rest of the already added layers and notifies the user if he might have
     * made a bad decision.
     * 
     * The basic algorithm works as follows:
     * 
     * - Calulate the total timespan covered by all layers together by taking
     * the date of the earliest frame and the latest frame - Calulate the length
     * of this total timespan - For each layer, calulate the length of it's
     * individual covered timespan - If the ratio between individual timespan
     * and the total timespan is smaller than a given
     * (smallestValidCoverageFraction) value, the algorithm decides that the
     * movies do not overlap nicely
     * 
     * Note:
     * 
     * - The maximum coverage ratio is 1.0, the minimum is 0.0
     * 
     * @see LayerTableOverlapWatcher#smallestValidCoverageFraction
     * 
     *      In addition to this, a warning is shown if no timing information is
     *      available for the newly added layer.
     */
    public void layerAdded(int idx) {

        // check if some of the layers do not really overlap
        boolean isGoodOverlap = true;

        // get the full timespan of all layers
        Date first = LayersModel.getSingletonInstance().getFirstDate();
        Date last = LayersModel.getSingletonInstance().getLastDate();

        if (first == null || last == null)
            return;

        Interval<Date> spanning = new Interval<Date>(first, last);
        long full_len = last.getTime() - first.getTime();

        // loop over all individual layers and check which fraction of the full
        // timespan they cover
        for (int curLayer = 0; curLayer < LayersModel.getSingletonInstance().getNumLayers(); curLayer++) {
            ImmutableDateTime start = LayersModel.getSingletonInstance().getStartDate(curLayer);
            ImmutableDateTime end = LayersModel.getSingletonInstance().getEndDate(curLayer);

            // we have timing information
            if (start != null && end != null) {
                Date _start = start.getTime();
                Date _end = end.getTime();
                Interval<Date> span = new Interval<Date>(_start, _end);

                Interval<Date> intersection = spanning.intersectInterval(span);

                long len = (intersection.getEnd().getTime() - intersection.getStart().getTime());
                double fraction = (double) len / (double) full_len;

                if (fraction < smallestValidCoverageFraction) {
                    isGoodOverlap = false;
                }

                Log.debug("Overlap fraction for layer " + curLayer + " is " + fraction);

            }

        }

        // Show a message if no timing information is available
        if (!LayersModel.getSingletonInstance().isTimed(idx)) {
            Message.warnTitle("No Timing Information", "This movie contains no timing information.\nIt can thus not be played in sync with other movies.");
        } else if (!isGoodOverlap) {
            Message.warnTitle("Movies Barely Overlap", "Some of the movies do not (or only partially) overlap.\nSome movies can thus not (or only partially) be played in sync.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void activeLayerChanged(int idx) {
    }

    /**
     * {@inheritDoc}
     */
    public void viewportGeometryChanged() {
    }

    /**
     * {@inheritDoc}
     */
    public void timestampChanged(int idx) {
    }

    /**
     * {@inheritDoc}
     */
    public void subImageDataChanged() {
    }

    /**
     * {@inheritDoc}
     */
    public void layerChanged(int idx) {
    }

    /**
     * {@inheritDoc}
     */
    public void layerRemoved(View oldView, int oldIdx) {
    }

    /**
     * {@inheritDoc}
     */
    public void layerDownloaded(int idx) {
    }
}
