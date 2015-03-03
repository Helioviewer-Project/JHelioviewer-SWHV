package org.helioviewer.viewmodel.view;

import java.util.AbstractList;

/**
 * Basic interface for every view.
 * 
 * <p>
 * A view represents an knot within the view chain. The view chain is a tree of
 * multiple views, usually with a {@link ComponentView} as a root and several
 * {@link ImageInfoView}s as leafs. Along every path through the view chain, the
 * image data is changed, for example by applying filters (see
 * {@link FilterView}), adding overlays (see {@link OverlayView} or merging
 * several partial images into one resulting image (see {@link LayeredView}. The
 * different views communicate via {@link ViewListener}s.
 * 
 * <p>
 * The design of the view chain is very loose and allows many variations, but
 * there exist one requirement has to hold: After finishing the construction of
 * any view chain, no call to getAdapter for {@link RegionView},
 * {@link ViewportView} or {@link MetaDataView} is allowed to return null, thus
 * every path of the view chain must contain all three views. To ensure that,
 * refer to {@link ImageInfoView} and {@link LayeredView}
 * 
 * <p>
 * This interface is essential for ever view. It provides functions to add and
 * remove ViewListeners, so that any other class will be informed about changes
 * in this view. Apart from that, it provides a basic mechanism to navigate
 * through the view chain: Calling getAdapter returns the next view within the
 * view chain implementing a desired interface, independent from the number and
 * structure of views in between.
 * 
 * @author Ludwig Schmidt
 */
public interface View {

    /**
     * Adds a view listener.
     * 
     * This listener will be called on every change within this view or from
     * views deeper in the view chain.
     * 
     * @param l
     *            the listener to add
     * @see #removeViewListener(ViewListener)
     */
    public void addViewListener(ViewListener l);

    /**
     * Removes a view listener.
     * 
     * The listener no longer will be informed about changes within this view or
     * from views deeper in the view chain.
     * 
     * @param l
     *            the listener to remove
     * @see #addViewListener(ViewListener)
     */
    public void removeViewListener(ViewListener l);

    /**
     * Get all view listeners
     * 
     * @return List which contains references to all view listeners
     */
    public AbstractList<ViewListener> getAllViewListeners();

    /**
     * Returns a View of given interface or class.
     * 
     * If this view implements the given interface itself, it returns itself,
     * otherwise it returns a suitable value (for example another view located
     * deeper within the the view chain, that can provide the desired
     * information, or null, if this is not possible).
     * 
     * @param c
     *            The desired interface
     * @return View implementing the interface, if available, null otherwise
     */
    public <T extends View> T getAdapter(Class<T> c);

}