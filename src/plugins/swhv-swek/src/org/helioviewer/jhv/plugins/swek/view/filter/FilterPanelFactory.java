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

    /**
     * Creates a list of filter panels for the given event type.
     * 
     * @param eventType
     *            the event type for which to create the filter panels
     * @return a list with filterpanels
     */
    public static List<AbstractFilterPanel> createFilterPanel(SWEKEventType eventType) {
        List<AbstractFilterPanel> panels = new ArrayList<AbstractFilterPanel>();
        for (SWEKParameter parameter : eventType.getParameterList()) {
            if (parameter.getParameterFilter() != null) {
                String filterType = parameter.getParameterFilter().getFilterType().toLowerCase();
                if (filterType.equals("doublevaluefilter")) {
                    panels.add(new DoubleValueFilterPanel(eventType, parameter));
                } else if (filterType.equals("doubleminmaxfilter")) {
                    panels.add(new DoubleMinMaxFilterPanel(eventType, parameter));
                } else if (filterType.equals("doublemaxfilter")) {
                    panels.add(new DoubleMaxFilterPanel(eventType, parameter));
                } else if (filterType.equals("doubleminfilter")) {
                    panels.add(new DoubleMinFilterPanel(eventType, parameter));
                }
            }
        }
        return panels;
    }

}
