package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.filter.FilterListener;
import org.helioviewer.viewmodel.imagedata.ImageData;

/**
 * Implementation of FilterView, providing the capability to apply filters on
 * the image.
 *
 * <p>
 * This view allows to filter the image data by using varies filters. Every time
 * the image data changes, the view calls the filter to calculate the new image
 * data. Apart from that, it feeds the filter with all other informations to do
 * its job, such as the current region, meta data or the full image.
 *
 * <p>
 * For further information on how to use filters, see
 * {@link org.helioviewer.viewmodel.filter}
 *
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 *
 */
public class StandardFilterView extends AbstractBasicView implements FilterView, SubimageDataView, ViewListener, FilterListener {

    protected Filter filter;

    protected ImageData filteredData;
    protected SubimageDataView subimageDataView;

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter getFilter() {
        return filter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFilter(Filter f) {
        if (filter != null) {
            filter.removeFilterListener(this);
        }

        filter = f;
        if (filter != null) {
            filter.addFilterListener(this);
        }

        refilter();

        // join change reasons to a change event
        ChangeEvent event = new ChangeEvent();
        event.addReason(new SubImageDataChangedReason(this));

        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageData getSubimageData() {
        if (subimageDataView != null) {
            return subimageDataView.getSubimageData();
        } else
            return null;
    }

    /**
     * Prepares the actual filter process.
     *
     * This function feeds the filter with all the additional informations it
     * needs to do its job, such as the region, meta data and the full image.
     */
    protected void refilterPrepare() {
    }

    /**
     * If we have a time machine below we want to force it staying below the
     * filter view
     *
     * @see org.helioviewer.viewmodel.view.AbstractBasicView#setView(org.helioviewer.viewmodel.view.View)
     */
    @Override
    public void setView(View newView) {
        super.setView(newView);
    }

    /**
     * Refilters the image.
     *
     * Calls the filter and fires a ChangeEvent afterwards.
     */
    protected void refilter() {
        updatePrecomputedViews();
        if (filter != null && view != null) {
            /* synchronized (filter) */{
                refilterPrepare();

                if (subimageDataView != null) {
                    filteredData = subimageDataView.getSubimageData();

                }
            }
        } else {
            filteredData = null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * In this case, refilters the image, if there is one.
     */
    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        updatePrecomputedViews();

        if (subimageDataView != null) {
            refilter();
            changeEvent.addReason(new SubImageDataChangedReason(this));
        }
    }

    /**
     * {@inheritDoc}
     *
     * In case the image data has changed, applies the filter.
     */
    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            updatePrecomputedViews();
            refilter();
        } else if (aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
            refilter();
        }

        notifyViewListeners(aEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterChanged(Filter f) {
        refilter();

        ChangeEvent event = new ChangeEvent();
        event.addReason(new SubImageDataChangedReason(this));
        notifyViewListeners(event);
    }

    /**
     * Updates the precomputed results for different view adapters.
     *
     * This adapters are precomputed to avoid unnecessary overhead appearing
     * when doing this every frame.
     */
    protected void updatePrecomputedViews() {
        subimageDataView = ViewHelper.getViewAdapter(view, SubimageDataView.class);
    }

}
