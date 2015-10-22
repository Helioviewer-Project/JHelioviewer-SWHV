package org.helioviewer.jhv.plugins.swek.download;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.helioviewer.jhv.base.logging.Log;

public enum SWEKOperand {
    EQUALS("="), NOT_EQUAL("!="), BIGGER(">"), SMALLER("<"), BIGGER_OR_EQUAL(">="), SMALLER_OR_EQUAL("<="), LIKE("like");

    private String representation;

    private SWEKOperand(String representation) {
        this.representation = representation;
    }

    /**
     * Gets the string representation.
     * 
     * @return the string representation
     */
    public String getStringRepresentation() {
        return representation;
    }

    /**
     * Gets the URL encoded representation.
     * 
     * @return the URL encoded representation
     */
    public String URLEncodedRepresentation() {
        try {
            return URLEncoder.encode(representation, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.error("Could not encode " + representation + ". Returned the normal representation.");
            return representation;
        }
    }
}
