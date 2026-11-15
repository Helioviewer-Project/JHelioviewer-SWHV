package org.helioviewer.jhv.event;

import org.helioviewer.jhv.math.Quat;

public class SolarEvent {

    // Arc angles are in the Earth view at the event start
    public record CMEParameters(double speedKmPerSecond, double principalAngleDegree, double angularWidthDegree, Quat earthView) {
        public static final CMEParameters DEFAULT = new CMEParameters(500, 0, 0, null);
    }

    public record Link(int firstId, int secondId) {}

    public record LinkRef(String firstUid, String secondUid) {}

    public final long start;
    public final long end;
    private final int id;
    private final SWEKSupplier supplier;

    private final EventMetadata metadata;
    private final EventGeometry positionInformation;
    private final CMEParameters cmeParameters;

    public SolarEvent(SWEKSupplier supplier, int id, long start, long end) {
        this(supplier, id, start, end, null, CMEParameters.DEFAULT, EventMetadata.EMPTY);
    }

    public SolarEvent(SWEKSupplier _supplier, int _id, long _start, long _end,
                      EventGeometry _positionInformation, CMEParameters _cmeParameters, EventMetadata _metadata) {
        metadata = _metadata;
        positionInformation = _positionInformation;
        cmeParameters = _cmeParameters;
        supplier = _supplier;
        start = _start;
        end = _end;
        id = _id;
    }

    public EventParameter[] getAllEventParameters() {
        return metadata.allParameters();
    }

    public EventParameter[] getVisibleEventParameters() {
        return metadata.visibleParameters();
    }

    public EventGeometry getPositionInformation() {
        return positionInformation;
    }

    public CMEParameters getCMEParameters() {
        return cmeParameters;
    }

    // Arcs need a direction in the Earth view
    public boolean isArc() {
        return cmeParameters.earthView() != null;
    }

    public int getUniqueID() {
        return id;
    }

    public SWEKSupplier getSupplier() {
        return supplier;
    }

}
