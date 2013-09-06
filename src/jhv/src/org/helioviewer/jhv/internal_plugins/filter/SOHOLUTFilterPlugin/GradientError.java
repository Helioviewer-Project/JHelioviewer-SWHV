package org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin;

/**
 * Error related to read and evaluate a gimp gradient file
 * 
 * @author Helge Dietert
 * 
 */
public class GradientError extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public GradientError() {
        super();
    }

    public GradientError(String arg0) {
        super(arg0);
    }
}