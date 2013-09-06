package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.view.View;

/**
 * Class represents a change reason when a layer of the layered view has
 * changed.
 * 
 * @author Stephan Pagel
 * */
public class LayerChangedReason implements ChangedReason {

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    public enum LayerChangeType {
        LAYER_ADDED, LAYER_REMOVED, LAYER_VISIBILITY, LAYER_MOVED, LAYER_DOWNLOADED
    }

    // memorizes the associated view which has generated the ChangeReason
    private View view;

    // holds information what has happened with layer
    private LayerChangeType type;

    // memorize reference to first sub view of the layer
    private View subView;

    private int layerIndex;

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * 
     * @param aView
     *            View which caused the change reason.
     * @param aType
     *            Defines what has happened with the layer.
     * @param aSubView
     *            First sub view of the layer which has changed.
     * */
    public LayerChangedReason(View aView, LayerChangeType aType, View aSubView) {

        // memorize passed values
        view = aView;
        type = aType;
        subView = aSubView;
        layerIndex = -1;
    }

    /**
     * Default constructor.
     * 
     * @param aView
     *            View which caused the change reason.
     * @param aType
     *            Defines what has happened with the layer.
     * @param aSubView
     *            First sub view of the layer which has changed.
     * @param index
     *            Internal index of the layer which has changed
     * */
    public LayerChangedReason(View aView, LayerChangeType aType, View aSubView, int index) {

        // memorize passed values
        view = aView;
        type = aType;
        subView = aSubView;
        layerIndex = index;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the information what has happened to the layer.
     * 
     * @return the type of the change which indicates what has happened to the
     *         layer.
     * */
    public LayerChangeType getLayerChangeType() {
        return type;
    }

    /**
     * Returns the first sub view of the layer.
     * 
     * @return the first sub view of the layer.
     * */
    public View getSubView() {
        return subView;
    }

    /**
     * Return the index of the sub view of the layer
     */
    public int getLayerIndex() {
        return layerIndex;
    }
}
