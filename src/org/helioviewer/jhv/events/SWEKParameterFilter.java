package org.helioviewer.jhv.events;

public record SWEKParameterFilter(String type, Double min, Double max, Double startValue, Double stepSize, String units,
                                  String dbType) {
}
