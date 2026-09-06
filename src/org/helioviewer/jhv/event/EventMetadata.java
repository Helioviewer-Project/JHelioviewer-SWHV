package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.List;

public record EventMetadata(EventParameter[] allParameters, EventParameter[] visibleParameters) {

    public static final EventMetadata EMPTY = new EventMetadata(new EventParameter[0], new EventParameter[0]);

    public static final class Builder {
        private final SWEKSupplier supplier;
        private final boolean full;
        private final List<EventParameter> allParameters = new ArrayList<>();
        private final List<EventParameter> visibleParameters = new ArrayList<>();

        public Builder(SWEKSupplier _supplier, boolean _full) {
            supplier = _supplier;
            full = _full;
        }

        public void add(String key, String displayName, String value, boolean visible) {
            if (!visible && !full)
                return;
            EventParameter parameter = new EventParameter(key,
                    displayName != null ? displayName : key.replace("_", " ").trim(), value);
            allParameters.add(parameter);
            if (visible) visibleParameters.add(parameter);
        }

        public void add(String key, String value) {
            SWEK.Parameter parameter = supplier.findParameter(key);
            add(key, parameter != null ? parameter.displayName() : null, value, parameter != null && parameter.visible());
        }

        public EventMetadata build() {
            return new EventMetadata(allParameters.toArray(new EventParameter[0]), visibleParameters.toArray(new EventParameter[0]));
        }
    }
}
