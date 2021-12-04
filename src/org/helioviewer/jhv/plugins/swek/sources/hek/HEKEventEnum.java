package org.helioviewer.jhv.plugins.swek.sources.hek;

enum HEKEventEnum {
    ACTIVE_REGION("ActiveRegion", "Active Region", "AR"), CORONAL_MASS_EJECTION("CME", "Coronal Mass Ejection", "CE"), CORONAL_DIMMING(
            "CoronalDimming", "Coronal Dimming", "CD"), CORONAL_HOLE("CoronalHole", "Coronal Hole", "CH"), CORONAL_WAVE("CoronalWave",
            "Coronal Wave", "CW"), FILAMENT("Filament", "Filament", "FI"), FILAMENT_ERUPTION("FilamentEruption", "Filament Eruption", "FE"), FLARE(
            "Flare", "Flare", "FL"), SUNSPOT("Sunspot", "Sunspot", "SS"), EMERGING_FLUX("EmergingFlux", "Emerging Flux", "EF"), ERUPTION(
            "Eruption", "Eruption", "ER"), UNKNOWN("Unknown", "Unknown", "UK");

    // The abbreviation of the HEKEvent
    private final String eventAbbreviation;
    // The name of the SWEK Event
    private final String swekEventName;

    HEKEventEnum(String _hekEventName, String _swekEventName, String _eventAbbreviation) {
        eventAbbreviation = _eventAbbreviation;
        swekEventName = _swekEventName;
    }

    public static String getHEKEventAbbreviation(String eventType) {
        for (HEKEventEnum event : values()) {
            if (event.swekEventName.equals(eventType)) {
                return event.eventAbbreviation;
            }
        }
        return UNKNOWN.eventAbbreviation;
    }

}
