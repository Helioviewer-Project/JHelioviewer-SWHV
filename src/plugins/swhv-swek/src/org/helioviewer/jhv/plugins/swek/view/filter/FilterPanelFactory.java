package org.helioviewer.jhv.plugins.swek.view.filter;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;

/**
 * Creates the correct FilterPanel depending on the given event type.
 * 
 * @author Bram Bourgoignie (Bram.Borugoignie@oma.be)
 * 
 */
public class FilterPanelFactory {

    public static List<AbstractFilterPanel> createFilterPanel(SWEKEventType eventType) {
        List<AbstractFilterPanel> panels = new ArrayList<AbstractFilterPanel>();
        for (SWEKParameter parameter : eventType.getParameterList()) {
            if (parameter.getParameterFilter() != null) {
                if (parameter.getParameterFilter().getFilterType().toLowerCase().equals("valuefilter")) {
                    panels.add(new DoubleValueFilterPanel(eventType, parameter));
                }
            }
        }
        return panels;
    }
}
