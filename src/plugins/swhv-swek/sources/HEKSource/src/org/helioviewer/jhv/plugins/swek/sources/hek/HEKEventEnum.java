package org.helioviewer.jhv.plugins.swek.sources.hek;

public enum HEKEventEnum {

    ACTIVE_REGION("ActiveRegion", "Active Region", "AR"), CORONAL_MASS_EJECTION("CME", "Coronal Mass Ejection", "CE"), CORONAL_DIMMING(
            "CoronalDimming", "Coronal Dimming", "CD"), CORONAL_HOLE("CoronalHole", "Coronal Hole", "CH"), CORONAL_WAVE("CoronalWave",
            "Coronal Wave", "CW"), FILAMENT("Filament", "Filament", "FI"), FILAMENT_ERUPTION("FilamentEruption", "Filament Eruption", "FE"), FLARE(
            "Flare", "Flare", "FL"), SUNSPOT("Sunspot", "Sunspot", "SS"), EMERGING_FLUX("EmergingFlux", "Emerging Flux", "EF"), ERUPTION(
            "Eruption", "Eruption", "ER"), UNKNOWN("Unknown", "Unknown", "UK");

    /** The abbreviation of the HEKEvent */
    private final String eventAbbreviation;

    /** The name of the SWEK Event */
    private final String swekEventName;

    /** The HEK event name */
    private final String hekEventName;

    /**
     * Private constructor of the HEKEvent enumeration.
     * 
     * 
     * @param hekEventName
     *            The hek name of the hek event
     * @param swekEventName
     *            The swek name of the event
     * @param eventAbbreviation
     *            The hek abbreviation of the hek event
     */
    private HEKEventEnum(String hekEventName, String swekEventName, String eventAbbreviation) {
        this.eventAbbreviation = eventAbbreviation;
        this.swekEventName = swekEventName;
        this.hekEventName = hekEventName;
    }

    /**
     * Gets the abbreviation of the HEK event type
     * 
     * @return the abbreviation
     */
    public String getAbbreviation() {
        return eventAbbreviation;
    }

    /**
     * Gets the event name of the SWEK event name.
     * 
     * @return the SWEK event name
     */
    public String getSWEKEventName() {
        return swekEventName;
    }

    /**
     * Gets the HEK event name.
     * 
     * @return the HEK event name
     */
    public String getHEKEventName() {
        return hekEventName;
    }
}
