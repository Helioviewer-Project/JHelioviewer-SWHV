package org.helioviewer.plugins.eveplugin;

import org.helioviewer.base.logging.Log;

/**
 * This singleton holds some general state of the Eve plugin.
 * 
 * 
 * @author Bram.Bourgoignie@oma.be
 */

public class EVEState {

    /** The singleton instance.*/
    private static EVEState instance;
    /** Indicates if the mouse was dragged in the time interval selector.*/ 
    private boolean mouseTimeIntervalDragging;
    /** Indicates if the mouse was dragged in the value interval selector.*/
    private boolean mouseValueIntervalDragging;
    
    
    /**
     * Private constructor, can only be used internally.
     */
    private EVEState(){
        mouseTimeIntervalDragging = false;
        mouseValueIntervalDragging = false;
    }
    
    /**
     * Gives the singleton instance of the EVEState class.
     * 
     * @return  The EVEState singleton object
     */
    public static EVEState getSingletonInstance(){
        if (instance == null){
            instance = new EVEState();
        }
        return instance;
    }

    /**
     * Returns true if the mouse was dragged in the time interval pane.
     * 
     * @return  True is the mouse was dragged, false if the mouse was not dragged.
     */
    public boolean isMouseTimeIntervalDragging() {
        return mouseTimeIntervalDragging;
    }

    /**
     * Sets whether the mouse is dragged in the time interval pane at the moment.
     * 
     * @param mouseDragging True is the mouse is dragged, false if the mouse is not dragged.
     */
    public void setMouseTimeIntervalDragging(boolean mouseDragging) {
        Log.info("State set on "+mouseDragging+" by: ");
        this.mouseTimeIntervalDragging = mouseDragging;
    }

    /**
     * Returns true if the mouse was dragged in the value range pane.
     * 
     * @return  True is the mouse was dragged, false if the mouse was not dragged.
     */
    public boolean isMouseValueIntervalDragging() {
        return mouseValueIntervalDragging;
    }

    /**
     * Sets whether the mouse is dragged in the time interval pane at the moment.
     * 
     * @param mouseValueIntervalDragging    True is the mouse is dragged, false if the mouse is not dragged.
     */
    public void setMouseValueIntervalDragging(boolean mouseValueIntervalDragging) {
        this.mouseValueIntervalDragging = mouseValueIntervalDragging;
    }
    
    
}
