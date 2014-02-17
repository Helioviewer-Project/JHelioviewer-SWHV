package org.helioviewer.viewmodel.view.bufferedimage;

import java.util.LinkedList;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.factory.BufferedImageViewFactory;
import org.helioviewer.viewmodel.factory.ViewFactory;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.AbstractBasicView;
import org.helioviewer.viewmodel.view.ModifiableInnerViewView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.TimeMachineData;
import org.helioviewer.viewmodel.view.TimeMachineView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * Implementation of a TimeMachineView in software mode which basically caches
 * as far as possible and generates a slave view chain which will triggered for
 * not cached data.
 * <p>
 * It implements RegionView, ViewportView to update the cache and will forward
 * the events in the view chain.
 * <p>
 * It assumes that a MovieView is below in the view chain. It does not directly
 * introduce a special layer forwarding these events which would also imply a
 * distinction between timed movies and non-timed movies. Instead when passing
 * though the data and caching the data as necessary it will query the frame
 * number and adopt a proper behaviour.
 * 
 * @author Helge Dietert
 */
public class BufferedImageTimeMachineView extends AbstractBasicView implements SubimageDataView, TimeMachineView, RegionView, ViewportView {
    /**
     * Number of frames to be cached, including the current one
     */
    protected int cacheSize = 1;
    /**
     * Last shown frame number to which the calls are relative to
     */
    protected int frameNumber = 0;
    /**
     * Cached underlying TimedMovieView to avoid overhead
     * 
     * @see #updatePrecomputedViews()
     */
    protected MovieView movieView;
    /**
     * Cache of previous frames
     */
    protected LinkedList<ImageData> previousCache = new LinkedList<ImageData>();
    /**
     * Cached underlying region view to avoid overhead
     * 
     * @see #updatePrecomputedViews()
     */
    protected RegionView regionView;
    /**
     * Slave view chain listener to trigger the return after a rendering through
     * that chain has been initiated
     */
    private ViewListener slaveChainListener = new ViewListener() {
        public void viewChanged(View sender, ChangeEvent aEvent) {
            System.out.println("Slave chain update: " + aEvent + " from thread " + Thread.currentThread().getId() + " with frame " + slaveMovieView.getCurrentFrameNumber());
            synchronized (slaveWait) {
                System.out.println("Slave chain notify");
                slaveWait.notify();
            }
        }
    };
    /**
     * Synchronization object to lock access to the slave view chain
     * 
     * @see #slaveView
     */
    protected Object slaveLock = new Object();
    /**
     * Cached underlying TimedMovieView in the slave view chain
     * 
     * @see #setViewSpecificImplementation(View, ChangeEvent)
     */
    protected MovieView slaveMovieView;
    /**
     * Cached underlying region view in the slave view chain
     * 
     * @see #setViewSpecificImplementation(View, ChangeEvent)
     */
    protected RegionView slaveRegionView;
    /**
     * Cached underlying subimageDataView in the slave view chain
     * 
     * @see #setViewSpecificImplementation(View, ChangeEvent)
     */
    protected SubimageDataView slaveSubimageDataView;
    /**
     * Slave view chain to create non-cached image data
     * <p>
     * The general access is locked though {@link #slaveLock} which must be hold
     * through requesting data. Once the slave view chain has finished
     * processing data the object {@link #slaveWait} gets notified through
     * {@link Object#notify()}.
     */
    protected View slaveView;
    /**
     * Cached underlying ViewportView of the slave view chain
     * 
     * @see #setViewSpecificImplementation(View, ChangeEvent)
     */
    protected ViewportView slaveViewportView;
    /**
     * Synchronization object to wait until the slave chain finished rendering.
     * 
     * @see #slaveView
     */
    protected Object slaveWait = new Object();

    /**
     * Cached underlying subimageDataView to avoid overhead
     * 
     * @see #updatePrecomputedViews()
     */
    protected SubimageDataView subimageDataView;

    /**
     * Data object to pass to the filter to access the extra information
     */
    protected TimeMachineData timeMachineData = new TimeMachineData() {
        /**
         * Call the slave view chain to obtain the requested data
         * 
         * @see org.helioviewer.viewmodel.view.TimeMachineData#getAbsoluteFrame(int)
         */
        public ImageData getAbsoluteFrame(int pos) {
            return renderThroughSlave(movieView.getCurrentFrameNumber() - pos);
        }

        /**
         * Try to answer the query from the cache. Otherwise use the slave view
         * chain.
         * 
         * @see org.helioviewer.viewmodel.view.TimeMachineData#getPreviousFrame(int)
         */
        public ImageData getPreviousFrame(int pos) {
            try {
                System.out.println("Previous frame " + pos + " with current master frame " + movieView.getCurrentFrameNumber());
                return previousCache.get(pos);
            } catch (IndexOutOfBoundsException e) {
                return renderThroughSlave(movieView.getCurrentFrameNumber() - pos);
            }
        }

        /**
         * @see org.helioviewer.viewmodel.view.TimeMachineData#setPreviousCache(int)
         */
        public void setPreviousCache(int n) {
            cacheSize = n + 1;
        }
    };

    /**
     * Cached underlying ViewportView to avoid overhead
     * 
     * @see #updatePrecomputedViews()
     */
    protected ViewportView viewportView;

    /**
     * Creates the slave view chain as a copy from the currently set view
     * <p>
     * This assumes that the view chain only consists of simple
     * ModifiableInnerViewView and one final view.
     * 
     * @param factory
     *            Factory to create the copies
     */
    protected void createSlaveViewChain(ViewFactory factory) {
        // Walk down the main view chain and create the slave view chain on the
        // way
        View currentMaster = view;
        View currentSlave = factory.createViewFromSource(currentMaster, true);
        slaveView = currentSlave;
        // Go down
        while (currentMaster instanceof ModifiableInnerViewView) {
            currentMaster = ((ModifiableInnerViewView) currentMaster).getView();
            View newSlave = factory.createViewFromSource(currentMaster, true);
            ((ModifiableInnerViewView) currentSlave).setView(newSlave);
            currentSlave = newSlave;
        }
    }

    /**
     * @see org.helioviewer.viewmodel.view.RegionView#getRegion()
     */
    public Region getRegion() {
        return regionView.getRegion();
    }

    /**
     * Passes through the image data and caches as necessary.
     * 
     * @return image data of the current movie frame
     * @see org.helioviewer.viewmodel.imagedata.ImageData.SubimataDataView#getSubimageData()
     */
    public ImageData getSubimageData() {
        Log.info("TimeMachine::getSubimageData() -> pass through frame " + movieView.getCurrentFrameNumber());
        // Update and check the frame number
        int newFrameNumber = movieView.getCurrentFrameNumber();
        if (newFrameNumber - frameNumber != 1) {
            // This is not the next frame, so we need to empty the cache
            // TODO more cases!!!
            previousCache.clear();
        }
        frameNumber = newFrameNumber;
        // Cache the data and return
        ImageData newFrame = subimageDataView.getSubimageData();
        previousCache.addFirst(newFrame);
        while (previousCache.size() > cacheSize)
            previousCache.removeLast();
        System.out.println("New cache: ");
        Log.info("TimeMachine::getSubimageData() -> new cache size " + previousCache.size());
        return newFrame;
    }

    /**
     * @see org.helioviewer.viewmodel.view.TimeMachineView#getTimeMachineData()
     */
    public TimeMachineData getTimeMachineData() {
        return timeMachineData;
    }

    /**
     * @see org.helioviewer.viewmodel.view.ViewportView#getViewport()
     */
    public Viewport getViewport() {
        return viewportView.getViewport();
    }

    /**
     * Renders the given frame through the slave view chain.
     * 
     * @param frameNumber
     *            Absolute frame number to render
     * @return image data if its a valid frame number, otherwise null or it
     *         could not be rendered
     */
    protected ImageData renderThroughSlave(int frameNumber) {
        // Out of range ?
        if (frameNumber < 0 || frameNumber > movieView.getMaximumFrameNumber())
            return null;
        // Ask slave if set
        System.out.println("Thread " + Thread.currentThread().getId() + " tries to render through secondary slave on object " + slaveWait);
        if (slaveView == null) {
            return null;
        }
        synchronized (slaveLock) {
            System.out.println("Start rendering through slave");
            synchronized (slaveWait) {
                slaveMovieView.setCurrentFrame(frameNumber, new ChangeEvent(), true);
                try {
                    slaveWait.wait();
                } catch (InterruptedException e) {
                    Log.error("BufferedImageTimeMachineView:: Could not render slave view chain", e);
                    return null;
                }
            }
        }
        return slaveSubimageDataView.getSubimageData();
    }

    /**
     * Changes the displayed region.
     * <p>
     * The tracking feature may cause this to happen very often. However, in
     * this case we could shift the image and fill up the pixel on the side with
     * black. One problem with that is that the region is given in floating
     * points units and the shifting may be not the shifting of a whole pixel
     * <p>
     * TODO Implement this, maybe rounding shifts to the next integer value. We
     * could try just recalculating these missing bars on the side.
     * 
     * @see org.helioviewer.viewmodel.view.RegionView#setRegion(org.helioviewer.viewmodel.region.Region,
     *      org.helioviewer.viewmodel.changeevent.ChangeEvent)
     */
    public boolean setRegion(Region r, ChangeEvent event) {
        Region oldRegion = regionView.getRegion();
        // Just a shift?
        if (oldRegion!=null &&r.getHeight() == oldRegion.getHeight() && r.getWidth() == oldRegion.getWidth()) {
            // TODO Rounding, shifting, (recalculate sides?)
            previousCache.clear();
        } else {
            // okay we need to invalidate the cache
            previousCache.clear();
        }
        slaveRegionView.setRegion(r, new ChangeEvent());
        return regionView.setRegion(r, event);
    }

    /**
     * Creates a new slave view chain, updates the cache and listener
     */
    private void setupSlaveViewChain() {
        if (slaveView != null)
            slaveView.removeViewListener(slaveChainListener);

        createSlaveViewChain(new BufferedImageViewFactory());

        slaveMovieView = ViewHelper.getViewAdapter(slaveView, MovieView.class);
        slaveRegionView = ViewHelper.getViewAdapter(slaveView, RegionView.class);
        slaveSubimageDataView = ViewHelper.getViewAdapter(slaveView, SubimageDataView.class);
        slaveViewportView = ViewHelper.getViewAdapter(slaveView, ViewportView.class);
        // TODO Anything needed to save CPU time? If I set the region etc. does
        // this also trigger the rendering

        slaveView.addViewListener(slaveChainListener);
    }

    /**
     * Passes down the viewport change.
     * <p>
     * We cannot simply shift the pixel and the matching seems very complicated.
     * Furthermore a viewport change does not happen often while playing a
     * movie. Hence the cache will be invalidated, i.e. its an expensive
     * operation.
     * 
     * @see org.helioviewer.viewmodel.view.ViewportView#setViewport(org.helioviewer.viewmodel.viewport.Viewport,org.helioviewer.viewmodel.changeevent.ChangeEvent)
     */
    public boolean setViewport(Viewport v, ChangeEvent event) {
        previousCache.clear();
        slaveViewportView.setViewport(v, new ChangeEvent());
        return viewportView.setViewport(v, event);
    }

    /**
     * Setup the caches and the slave view chain
     * 
     * @see org.helioviewer.viewmodel.view.AbstractBasicView#setViewSpecificImplementation(org.helioviewer.viewmodel.view.View,org.helioviewer.viewmodel.changeevent.ChangeEvent)
     */
    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        updatePrecomputedViews();
        setupSlaveViewChain();
        movieView.setReuseBuffer(false);
    }

    /**
     * Updates the precomputed results for different view adapters.
     * 
     * This adapters are precomputed to avoid unnecessary overhead appearing
     * when doing this every frame.
     */
    protected void updatePrecomputedViews() {
        movieView = ViewHelper.getViewAdapter(view, MovieView.class);
        regionView = ViewHelper.getViewAdapter(view, RegionView.class);
        subimageDataView = ViewHelper.getViewAdapter(view, SubimageDataView.class);
        viewportView = ViewHelper.getViewAdapter(view, ViewportView.class);
    }

    /**
     * Inform the listener about the events, update the cache and slave chain as
     * necessary.
     * 
     * @see org.helioviewer.viewmodel.view.ViewListener#viewChanged(org.helioviewer.viewmodel.view.View,org.helioviewer.viewmodel.changeevent.ChangeEvent)
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {
        System.out.println("Master view changed " + aEvent);
        // The region and viewport changes are already redirected to the slave
        // view chain
        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            updatePrecomputedViews();
            // Just recreate the the slave view chain
            setupSlaveViewChain();
        }

        notifyViewListeners(aEvent);
    }
}