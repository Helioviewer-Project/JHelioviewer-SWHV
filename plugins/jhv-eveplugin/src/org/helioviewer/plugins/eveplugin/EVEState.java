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
    /** Indicates if the mouse was dragged.*/ 
    private boolean mouseDragging;
    
    /**
     * Private constructor, can only be used internally.
     */
    private EVEState(){
        mouseDragging = false;
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
     * Is the mouse dragged?
     * 
     * @return  True is the mouse was dragged, false if the mouse was not dragged.
     */
    public boolean isMouseDragging() {
        return mouseDragging;
    }

    /**
     * Sets whether the mouse is dragged at the moment.
     * 
     * @param mouseDragging True is the mouse is dragged, false if the mouse is not dragged.
     */
    public void setMouseDragging(boolean mouseDragging) {
        Log.debug("State set on "+mouseDragging+" by: ");
        Thread.dumpStack();
        this.mouseDragging = mouseDragging;
    }
    
    
}
