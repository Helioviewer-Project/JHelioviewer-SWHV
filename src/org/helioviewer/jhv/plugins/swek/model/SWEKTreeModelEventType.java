package org.helioviewer.jhv.plugins.swek.model;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.json.JSONObject;

// The SWEK tree model representation of the SWEK event type
public class SWEKTreeModelEventType extends SWEKTreeModelElement {

    /** The swekEventType for this treemodel event type */
    private final SWEKEventType swekEventType;

    /** List with SWEKSuppliers for this event type */
    private final List<SWEKTreeModelSupplier> swekTreeSuppliers = new ArrayList<>();

    /**
     * Creates a SWEK tree model event type for the given SWEK event type.
     * 
     * @param _swekEventType
     *            The event type for which the SWEK tree model event type is
     *            created
     */
    public SWEKTreeModelEventType(SWEKEventType _swekEventType) {
        super(false, _swekEventType.getEventIcon());
        swekEventType = _swekEventType;
        fillSWEKTreeSuppliers();
    }

    /**
     * Gets the event type of this SWEK tree model event type.
     * 
     * @return the SWEK event type
     */
    public SWEKEventType getSwekEventType() {
        return swekEventType;
    }

    /**
     * Gets the list of SWEK tree model suppliers for this SWEK tree model event
     * type.
     * 
     * @return a list of SWEK tree model suppliers
     */
    public List<SWEKTreeModelSupplier> getSwekTreeSuppliers() {
        return swekTreeSuppliers;
    }

    /**
     * Fills the list of SWEK tree model suppliers for this swek event type.
     */
    private void fillSWEKTreeSuppliers() {
        for (SWEKSupplier swekSupplier : swekEventType.getSuppliers()) {
            swekTreeSuppliers.add(new SWEKTreeModelSupplier(swekSupplier));
        }
    }

    public void serialize(JSONObject swekObject) {
        JSONObject eventType = new JSONObject();
        eventType.put("selected", isCheckboxSelected());
        JSONObject suppliers = new JSONObject();
        eventType.put("suppliers", suppliers);
        swekObject.put(swekEventType.getEventName(), eventType);
        for (SWEKTreeModelSupplier supplier : swekTreeSuppliers) {
            supplier.serialize(suppliers);
        }
    }

    public void deserialize(JSONObject swekObject, EventTypePanelModel eventPanelModel) {
        JSONObject eventType = swekObject.optJSONObject(swekEventType.getEventName());
        boolean selected;
        if(eventType !=null) {
            selected = eventType.optBoolean("selected", false);
            setCheckboxSelected(selected);
            JSONObject suppliers = eventType.optJSONObject("suppliers");
            if (suppliers != null) {
                for (SWEKTreeModelSupplier supplier : swekTreeSuppliers) {
                    supplier.deserialize(suppliers, eventPanelModel, swekEventType);
                }
            }
        }
    }

}
