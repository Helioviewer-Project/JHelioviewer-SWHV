/**
 *
 */
package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.viewmodelplugin.overlay.OverlayPanel;

/**
 * The main visual component of the SWEK-plugin.
 * 
 * @author Bram.Bourgoignie@oma.be
 * 
 */
public class SWEKPluginPanel extends OverlayPanel {

    /** Serial version UID */
    private static final long serialVersionUID = 212085486418646472L;

    /** The singleton panel used */
    private static SWEKPluginPanel swekPluginPanel;

    private final SWEKConfigurationManager configManager;

    private SWEKPluginPanel() {
        this.configManager = SWEKConfigurationManager.getSingletonInstance();
        initVisualComponents();

    }

    /**
     * Initializes the visual components.
     */
    private void initVisualComponents() {
        this.setLayout(new BorderLayout());
        JPanel eventTypePanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(eventTypePanel, BoxLayout.Y_AXIS);
        eventTypePanel.setLayout(boxLayout);
        for (SWEKEventType eventType : this.configManager.getEventTypes()
                .values()) {
            EventPanel eventPanel = new EventPanel(eventType);
            eventPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            eventTypePanel.add(eventPanel);
        }
        JScrollPane sp = new JScrollPane(eventTypePanel);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(sp, BorderLayout.CENTER);
        sp.setBackground(Color.CYAN);
        eventTypePanel.setBackground(Color.RED);
        setBackground(Color.BLUE);
    }

    /**
     * Gives the main SWEKPluginPanel.
     * 
     * @return The swekPluginPanel.
     */
    public static SWEKPluginPanel getSWEKPluginPanelInstance() {
        if (swekPluginPanel == null) {
            swekPluginPanel = new SWEKPluginPanel();
        }
        return swekPluginPanel;
    }
}
