package org.helioviewer.jhv.event;

import java.util.List;

public class SWEK {

    public enum Operand {
        BIGGER_OR_EQUAL(">="),
        SMALLER_OR_EQUAL("<=");

        public final String representation;

        Operand(String _representation) {
            representation = _representation;
        }

    }

    public record ParameterFilter(String type, Double min, Double max, Double startValue, Double stepSize, String units,
                                  String dbType) {}

    public record Param(String name, String value, Operand operand) {}

    public record Parameter(String name, String displayName, ParameterFilter filter, boolean visible) {}

    public record RelatedOn(String parameterFrom, String parameterWith, String dbType) {}

    public record RelatedEvents(SWEKGroup group, SWEKGroup relatedWith, List<RelatedOn> relatedOnList) {}

    public record Source(String name, List<Parameter> generalParameters, SWEKHandler handler) {}

}
