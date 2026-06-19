package org.helioviewer.jhv.event;

public final class SWEKGroup {

    private final String name;
    private final String iconKey;

    public SWEKGroup(String _name, String _iconKey) {
        name = _name.intern();
        iconKey = _iconKey;
    }

    public String getName() {
        return name;
    }

    public String getIconKey() {
        return iconKey;
    }
}
