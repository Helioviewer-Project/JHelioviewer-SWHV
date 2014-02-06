package org.helioviewer.viewmodel.view;

/**
 * View to support filters working over several frames.
 * <p>
 * The normal playback is triggered by the normal view chain and an
 * implementation is supposed to work normally as a cache of the previous
 * frames.
 * <p>
 * To support setting the frame, zooming etc, or to support e.g. to apply a
 * running difference against a constant other frame, it creates a slave view
 * chain. If then data which is not available is requested, the main view chain
 * triggers the slave view chain to get the missing data.
 * <p>
 * The triggering of the slave view chain should only be used very, very rarely
 * and classes using this interface should request such data only very rarely.
 * <p>
 * Implementations may fill up the image with black to avoid a second decoding
 * if the region has only changed slightly.
 * 
 * @author Helge Dietert
 */
public interface TimeMachineView extends ModifiableInnerViewView {
    /**
     * Gives back an object to access the data of different frames and set it
     * up.
     * 
     * @return Reference to access the extra data
     */
    public TimeMachineData getTimeMachineData();
}
