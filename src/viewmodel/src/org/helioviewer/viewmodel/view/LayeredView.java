package org.helioviewer.viewmodel.view;

/**
 * View to merged multiple Views.
 * 
 * <p>
 * The LayeredView a central element of the view chain. It is responsible for
 * displaying multiple views, which are organized as a stack of layers. The
 * basic functionality this includes to add, move and remove layers.
 * 
 * <p>
 * When drawing the different layers, the layer with index zero is drawn first,
 * so the stack of layers is drawn in order bottom to top.
 * 
 * <p>
 * The position of the layers in relation to each other is calculated based on
 * their regions. Thus, every view that is connected as a layer must provide a
 * {@link RegionView}. Also, every layer has to provide a {@link MetaDataView}
 * and a {@link ViewportView}. To take care of this requirement, implement the
 * {@link ImageInfoView} as recommended. Since a LayeredView can be used as a
 * layer itself, its implementation also should implement {@link RegionView},
 * {@link ViewportView} and {@link MetaDataView} as well.
 * 
 * <p>
 * As an additional feature, the LayeredView support hiding layers.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface LayeredView extends View {

    /**
     * Returns number of layers currently connected to the LayeredView.
     * 
     * @return Number of layers
     * @see #getNumberOfVisibleLayer
     */
    public int getNumLayers();

    /**
     * Returns, whether the given view is visible.
     * 
     * If the given view is not a direct child of the LayeredView, returns false
     * in any case.
     * 
     * @param view
     *            View to test for visibility
     * @return True if the view is visible
     * @see #toggleVisibility
     */
    public boolean isVisible(View view);

    /**
     * Returns number of layers currently visible.
     * 
     * This number is lesser or equal to the number of total layers currently
     * connected to the LayeredView.
     * 
     * @return Number of visible layers
     * @see #getNumLayers
     */
    public int getNumberOfVisibleLayer();

    /**
     * Toggles the visibility if the given view.
     * 
     * If the given view is not a direct child of the LayeredView, nothing
     * happens.
     * 
     * @param view
     *            View to toggle visibility
     * @see #isVisible
     */
    public void toggleVisibility(View view);

    /**
     * Adds a view as a new layer to the LayeredView.
     * 
     * The new layer is inserted on top of the current stack, thus will be drawn
     * as last.
     * 
     * @param newLayer
     *            View to add as a new layer
     * @see #removeLayer
     */
    public void addLayer(View newLayer);

    /**
     * Adds a view as a new layer to the LayeredView.
     * 
     * The new layer is inserted at the given position of the current stack.
     * 
     * @param newLayer
     *            View to add as a new layer
     * @see #removeLayer
     */
    public void addLayer(View newLayer, int newIndex);

    /**
     * Removes a layer from the LayeredView.
     * 
     * @param index
     *            position of the layer within the stack of layers.
     * @see #addLayer
     */
    public void removeLayer(int index);

    /**
     * Removes a layer from the LayeredView.
     * 
     * If the given view is not a direct child of the LayeredView, nothing
     * happens.
     * 
     * @param view
     *            View to remove from the LayeredView
     * @see #addLayer
     */
    public void removeLayer(View view);

    /**
     * Removes all layers of the layered view. This method should be preferred
     * over calling removeLayer(View) for every single layer.
     */
    public void removeAllLayers();

    /**
     * Returns the position of the view within the stack of layers.
     * 
     * Zero indicates the most bottom view. If the given view is not a direct
     * child of the LayeredView, the function returns -1.
     * 
     * @param view
     *            View to search for within the stack of layers
     * @return Position of the view within stack
     * @see #moveView
     */
    public int getLayerLevel(View view);

    /**
     * Returns the view at a given position within the stack of layers.
     * 
     * @param index
     *            Position within the stack of layers
     * @return View at given position
     */
    public View getLayer(int index);

    /**
     * Moves a layer to a different position within the stack of layers.
     * 
     * If the given view is not a direct child of the LayeredView, nothing
     * happens.
     * 
     * @param view
     *            Layer to move to a new position
     * @param newLevel
     *            new position
     * @see #getLayerLevel
     * @see #getLayer
     */
    public void moveView(View view, int newLevel);

}
