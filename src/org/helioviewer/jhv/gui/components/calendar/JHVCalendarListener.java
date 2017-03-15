package org.helioviewer.jhv.gui.components.calendar;

/**
 * The listener interface for receiving JHVCalendarEvents. The class that is
 * interested in processing an JHVCalendarEvent implements this interface, and
 * the object created with that class is registered with a component, using the
 * component's addJHVCalendarListener method. When the action event occurs, that
 * object's actionPerformed method is invoked.
 */
public interface JHVCalendarListener {

    void actionPerformed(JHVCalendarEvent e);

}
