package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.List;

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

    private JHVEventParameter[] allParameters = new JHVEventParameter[0];
    private JHVEventParameter[] visibleParameters = new JHVEventParameter[0];

    private List<JHVEventParameter> allParametersArray = new ArrayList<>();
    private List<JHVEventParameter> visibleParametersArray = new ArrayList<>();
    private final JHVPositionInformation positionInformation;
    private final CMEParameters cmeParameters;

    public JHVEvent(SWEKSupplier supplier, int id, long start, long end) {
        this(supplier, id, start, end, null, CMEParameters.DEFAULT);
    }

    public JHVEvent(SWEKSupplier _supplier, int _id, long _start, long _end, JHVPositionInformation _positionInformation, CMEParameters _cmeParameters) {
        positionInformation = _positionInformation;
        cmeParameters = _cmeParameters;
        supplier = _supplier;
        start = _start;
        end = _end;
        id = _id;
    }

    public JHVEventParameter[] getAllEventParameters() {
        return allParameters;
    }

    public JHVEventParameter[] getVisibleEventParameters() {
        return visibleParameters;
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

    public void addParameter(String key, String displayName, String value, boolean visible, boolean full) {
        if (allParametersArray == null)
            throw new IllegalStateException("Cannot add parameters after finishParams");

        if (!visible && !full)
            return;
        JHVEventParameter parameter = new JHVEventParameter(key,
                displayName != null ? displayName : key.replace("_", " ").trim(), value);
        allParametersArray.add(parameter);
        if (visible) visibleParametersArray.add(parameter);
    }

    public void addParameter(String key, String value, boolean full) {
        SWEK.Parameter p = supplier.findParameter(key);

        boolean visible = (p != null) && p.visible();
        String displayName = (p != null) ? p.displayName() : null;

        addParameter(key, displayName, value, visible, full);
    }

    public void finishParams() {
        allParameters = allParametersArray.toArray(new JHVEventParameter[0]);
        visibleParameters = visibleParametersArray.toArray(new JHVEventParameter[0]);

        allParametersArray = null;
        visibleParametersArray = null;
    }
}
