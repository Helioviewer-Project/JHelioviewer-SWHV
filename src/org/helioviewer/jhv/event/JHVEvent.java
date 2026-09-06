package org.helioviewer.jhv.event;

public class JHVEvent {

    public record CMEParameters(double speedKmPerSecond, double principalAngleDegree, double angularWidthDegree) {
        public static final CMEParameters DEFAULT = new CMEParameters(500, 0, 0);
    }

    public record Link(int firstId, int secondId) {}

    public record LinkRef(String firstUid, String secondUid) {}

    public final long start;
    public final long end;
    private final int id;
    private final SWEKSupplier supplier;

    private final JHVEventMetadata metadata;
    private final JHVPositionInformation positionInformation;
    private final CMEParameters cmeParameters;

    public JHVEvent(SWEKSupplier supplier, int id, long start, long end) {
        this(supplier, id, start, end, null, CMEParameters.DEFAULT, JHVEventMetadata.EMPTY);
    }

    public JHVEvent(SWEKSupplier _supplier, int _id, long _start, long _end,
                    JHVPositionInformation _positionInformation, CMEParameters _cmeParameters, JHVEventMetadata _metadata) {
        metadata = _metadata;
        positionInformation = _positionInformation;
        cmeParameters = _cmeParameters;
        supplier = _supplier;
        start = _start;
        end = _end;
        id = _id;
    }

    public JHVEventParameter[] getAllEventParameters() {
        return metadata.allParameters();
    }

    public JHVEventParameter[] getVisibleEventParameters() {
        return metadata.visibleParameters();
    }

    public JHVPositionInformation getPositionInformation() {
        return positionInformation;
    }

    public CMEParameters getCMEParameters() {
        return cmeParameters;
    }

    public boolean isCactus() {
        return supplier.isCactus();
    }

    public int getUniqueID() {
        return id;
    }

    public SWEKSupplier getSupplier() {
        return supplier;
    }

}
