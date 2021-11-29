package org.helioviewer.jhv.events;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SWEK {

    public enum Operand {
        EQUALS("="), NOT_EQUAL("!="), BIGGER(">"), SMALLER("<"), BIGGER_OR_EQUAL(">="), SMALLER_OR_EQUAL("<="), LIKE("like");

        public final String representation;
        public final String encodedRepresentation;

        Operand(String _representation) {
            representation = _representation;
            encodedRepresentation = URLEncoder.encode(representation, StandardCharsets.UTF_8);
        }

    }

    public record ParameterFilter(String type, Double min, Double max, Double startValue, Double stepSize, String units,
                                  String dbType) {
    }

    public record Param(String name, String value, Operand operand) {
    }

    public record Parameter(String name, String displayName, ParameterFilter filter, boolean visible) {
    }

    // Holds the related parameters of related events
    public record RelatedOn(Parameter parameterFrom, Parameter parameterWith, String dbType) {
    }

    public record RelatedEvents(SWEKGroup group, SWEKGroup relatedWith, List<RelatedOn> relatedOnList) {
    }

    public record Source(String name, List<Parameter> generalParameters, SWEKHandler handler) {
    }

}
