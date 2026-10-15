package org.helioviewer.jhv.event;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SWEK {

    public enum NumericType {INTEGER, DECIMAL}

    public enum Operand {
        BIGGER_OR_EQUAL(">="),
        SMALLER_OR_EQUAL("<=");

        public final String representation;

        Operand(String _representation) {
            representation = _representation;
        }

    }

    public record ParameterFilter(String type, double min, double max, double startValue, double stepSize, String units) {
        public List<Operand> operands() {
            return switch (type.toLowerCase()) {
                case "doublemaxfilter", "flarefilter" -> List.of(Operand.BIGGER_OR_EQUAL);
                case "doubleminfilter" -> List.of(Operand.SMALLER_OR_EQUAL);
                case "doubleminmaxfilter" -> List.of(Operand.BIGGER_OR_EQUAL, Operand.SMALLER_OR_EQUAL);
                default -> List.of();
            };
        }

        public boolean isFlare() {
            return "flarefilter".equalsIgnoreCase(type);
        }
    }

    public record Param(String name, double value, Operand operand) {}

    public record Parameter(String name, String displayName, ParameterFilter filter, boolean visible) {}

    public record RelatedOn(String parameterFrom, String parameterWith) {}

    public record Relation(SWEKGroup group, SWEKGroup relatedWith, List<RelatedOn> relatedOnList) {
        public Relation {
            relatedOnList = List.copyOf(relatedOnList);
        }
    }

    public record Source(String name, List<Parameter> generalParameters, SWEKHandler handler, Map<String, NumericType> numericParameters) {
        public Source {
            generalParameters = List.copyOf(generalParameters);
            Map<String, NumericType> types = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            numericParameters.forEach((parameter, type) -> {
                NumericType previous = types.putIfAbsent(parameter, type);
                if (previous != null && previous != type)
                    throw new IllegalArgumentException("Conflicting numeric types for " + parameter + " in " + name);
            });
            numericParameters = Collections.unmodifiableMap(types);
        }
    }

}
