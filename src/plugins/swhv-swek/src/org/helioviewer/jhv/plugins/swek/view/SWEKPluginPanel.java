package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;

// The main visual component of the SWEK-plugin
@SuppressWarnings("serial")
public class SWEKPluginPanel extends JPanel {

    private static SWEKPluginPanel swekPluginPanel;

    private SWEKPluginPanel() {
        setLayout(new BorderLayout());

        JPanel eventTypePanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(eventTypePanel, BoxLayout.Y_AXIS);
        eventTypePanel.setLayout(boxLayout);

        for (SWEKEventType eventType : SWEKConfigurationManager.getOrderedEventTypes()) {
            EventPanel eventPanel = new EventPanel(eventType);
            eventPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            eventTypePanel.add(eventPanel);
        }
        add(eventTypePanel, BorderLayout.CENTER);
    }

    public static SWEKPluginPanel getSWEKPluginPanelInstance() {
        if (swekPluginPanel == null) {
            swekPluginPanel = new SWEKPluginPanel();
        }
        return swekPluginPanel;
    }

}
