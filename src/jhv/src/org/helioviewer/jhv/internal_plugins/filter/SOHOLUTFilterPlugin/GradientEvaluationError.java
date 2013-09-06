package org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin;

/**
 * Error while evaluating a gimp gradient file
 * 
 * @author Helge Dietert
 * 
 */
public class GradientEvaluationError extends GradientError {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public GradientEvaluationError() {
    }

    public GradientEvaluationError(String s) {
        super(s);
    }
}
