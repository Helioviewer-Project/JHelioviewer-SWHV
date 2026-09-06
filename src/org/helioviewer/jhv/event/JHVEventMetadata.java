package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.List;

public record JHVEventMetadata(JHVEventParameter[] allParameters, JHVEventParameter[] visibleParameters) {

    public static final JHVEventMetadata EMPTY = new JHVEventMetadata(new JHVEventParameter[0], new JHVEventParameter[0]);

    public static final class Builder {
        private final SWEKSupplier supplier;
        private final boolean full;
        private final List<JHVEventParameter> allParameters = new ArrayList<>();
        private final List<JHVEventParameter> visibleParameters = new ArrayList<>();

        public Builder(SWEKSupplier _supplier, boolean _full) {
            supplier = _supplier;
            full = _full;
        }

        public void add(String key, String displayName, String value, boolean visible) {
            if (!visible && !full)
                return;
            JHVEventParameter parameter = new JHVEventParameter(key,
                    displayName != null ? displayName : key.replace("_", " ").trim(), value);
            allParameters.add(parameter);
            if (visible) visibleParameters.add(parameter);
        }

        public void add(String key, String value) {
            SWEK.Parameter parameter = supplier.findParameter(key);
            add(key, parameter != null ? parameter.displayName() : null, value, parameter != null && parameter.visible());
        }

        public JHVEventMetadata build() {
            return new JHVEventMetadata(allParameters.toArray(new JHVEventParameter[0]), visibleParameters.toArray(new JHVEventParameter[0]));
        }
    }
}
