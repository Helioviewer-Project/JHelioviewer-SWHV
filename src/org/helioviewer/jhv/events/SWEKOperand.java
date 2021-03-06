package org.helioviewer.jhv.events;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public enum SWEKOperand {

    EQUALS("="), NOT_EQUAL("!="), BIGGER(">"), SMALLER("<"), BIGGER_OR_EQUAL(">="), SMALLER_OR_EQUAL("<="), LIKE("like");

    public final String representation;
    public final String encodedRepresentation;

    SWEKOperand(String _representation) {
        representation = _representation;
        encodedRepresentation = URLEncoder.encode(representation, StandardCharsets.UTF_8);
    }

}
