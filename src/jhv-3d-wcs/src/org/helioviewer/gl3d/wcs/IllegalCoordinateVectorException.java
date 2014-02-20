package org.helioviewer.gl3d.wcs;

/**
 * An exception that is raised by various components of the WCS package if a
 * provided {@link CoordinateVector} is not valid within its
 * {@link CoordinateSystem}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class IllegalCoordinateVectorException extends IllegalArgumentException {
    private static final long serialVersionUID = 5164183299005916920L;

    public IllegalCoordinateVectorException() {
        super();
    }

    public IllegalCoordinateVectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalCoordinateVectorException(String s) {
        super(s);
    }

    public IllegalCoordinateVectorException(Throwable cause) {
        super(cause);
    }
}
