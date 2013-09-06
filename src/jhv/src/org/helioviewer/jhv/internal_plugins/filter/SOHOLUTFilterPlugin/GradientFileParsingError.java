package org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin;

/**
 * Error when reading a gimp gradient file
 * 
 * @author Helge Dietert
 */
public class GradientFileParsingError extends GradientError {
    private static final long serialVersionUID = 1L;

    public GradientFileParsingError() {
    }

    public GradientFileParsingError(String s) {
        super(s);
    }
}
