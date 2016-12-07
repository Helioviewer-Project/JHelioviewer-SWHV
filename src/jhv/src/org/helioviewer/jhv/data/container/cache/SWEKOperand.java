package org.helioviewer.jhv.data.container.cache;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.helioviewer.jhv.base.logging.Log;

public enum SWEKOperand {

    EQUALS("="), NOT_EQUAL("!="), BIGGER(">"), SMALLER("<"), BIGGER_OR_EQUAL(">="), SMALLER_OR_EQUAL("<="), LIKE("like");

    private final String representation;

    SWEKOperand(String _representation) {
        representation = _representation;
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
            return URLEncoder.encode(representation, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            Log.error("Could not encode " + representation + ". Returned the normal representation.");
            return representation;
        }
    }

}
