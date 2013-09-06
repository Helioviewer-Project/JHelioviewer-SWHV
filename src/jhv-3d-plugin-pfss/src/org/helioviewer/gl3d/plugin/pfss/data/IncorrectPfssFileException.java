package org.helioviewer.gl3d.plugin.pfss.data;

/**
 * Exception that is raised when the PfssPlugin cannot read a given input file.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class IncorrectPfssFileException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -3816779713324720599L;

    public IncorrectPfssFileException(final Throwable cause) {
        super(cause);
    }

}
