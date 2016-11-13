package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;

// The main visual component of the SWEK-plugin
@SuppressWarnings("serial")
public class SWEKPluginPanel extends JPanel {

    public SWEKPluginPanel(List<SWEKEventType> typeList) {
        setLayout(new BorderLayout());

        JPanel eventTypePanel = new JPanel();
        eventTypePanel.setLayout(new BoxLayout(eventTypePanel, BoxLayout.Y_AXIS));

        for (SWEKEventType eventType : typeList) {
            EventPanel eventPanel = new EventPanel(eventType);
            eventPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            eventTypePanel.add(eventPanel);
        }
        add(eventTypePanel, BorderLayout.CENTER);
    }

}
