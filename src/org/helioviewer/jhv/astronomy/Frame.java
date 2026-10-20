package org.helioviewer.jhv.astronomy;

public enum Frame {
    SOLO_HCI("HCI"), SOLO_HEEQ("HEEQ"), SOLO_HEE("HEE"), SOLO_IAU_SUN_2009("IAU_SUN"), J2000("J2000");

    private final String uiStr;

    Frame(String _uiStr) {
        uiStr = _uiStr;
    }

    public String uiString() {
        return uiStr;
    }

}
