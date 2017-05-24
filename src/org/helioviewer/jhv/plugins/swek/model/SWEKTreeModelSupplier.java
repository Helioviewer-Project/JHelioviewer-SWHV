package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.json.JSONObject;

public class SWEKTreeModelSupplier extends SWEKTreeModelElement {

    /** The SWEK supplier for this SWEK tree supplier */
    private final SWEKSupplier swekSupplier;

    /**
     * Creates a SWEK tree supplier for the given SWEkSupplier.
     * 
     * @param _swekSupplier
     *            The SWEK supplier for this SWEK tree supplier
     */
    public SWEKTreeModelSupplier(SWEKSupplier _swekSupplier) {
        super(false);
        swekSupplier = _swekSupplier;
    }

    /**
     * Gets the SWEK supplier.
     * 
     * @return The SWEK supplier
     */
    public SWEKSupplier getSwekSupplier() {
        return swekSupplier;
    }

    public void serialize(JSONObject suppliers) {
        suppliers.put(swekSupplier.getSupplierName(), isCheckboxSelected());
    }

    public void deserialize(JSONObject suppliers, EventTypePanelModel eventPanelModel, SWEKEventType swekEventType) {
        boolean selected = suppliers.optBoolean(swekSupplier.getSupplierName(), false);
        setCheckboxSelected(selected);
        if (selected) {
            eventPanelModel.fireNewEventTypeAndSourceActive(swekEventType, swekSupplier);
        } else {
            eventPanelModel.fireNewEventTypeAndSourceInactive(swekEventType, swekSupplier);
        }
    }

}
