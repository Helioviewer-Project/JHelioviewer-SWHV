package org.helioviewer.jhv.event;

import java.util.List;

public class SWEK {

    public enum NumericType { INTEGER, REAL }

    public enum Operand {
        BIGGER_OR_EQUAL(">="),
        SMALLER_OR_EQUAL("<=");

        public final String representation;

        Operand(String _representation) {
            representation = _representation;
        }

    }

    public record ParameterFilter(String type, double min, double max, double startValue, double stepSize, String units,
                                  String dbType) {}

    public record Param(String name, double value, Operand operand) {}

    public record Parameter(String name, String displayName, ParameterFilter filter, boolean visible) {}

    public record RelatedOn(String parameterFrom, String parameterWith, String dbType) {}

    public record RelatedEvents(SWEKGroup group, SWEKGroup relatedWith, List<RelatedOn> relatedOnList) {}

    public record Source(String name, List<Parameter> generalParameters, SWEKHandler handler) {}

}
