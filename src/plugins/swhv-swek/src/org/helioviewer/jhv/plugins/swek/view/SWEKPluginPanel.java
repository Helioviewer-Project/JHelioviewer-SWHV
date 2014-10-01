/**
 *
 */
package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelListener;
import org.helioviewer.viewmodelplugin.overlay.OverlayPanel;

/**
 * The main visual component of the SWEK-plugin.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKPluginPanel extends OverlayPanel implements SWEKTreeModelListener {

    /** Serial version UID */
    private static final long serialVersionUID = 212085486418646472L;

    /** The singleton panel used */
    private static SWEKPluginPanel swekPluginPanel;

    /** The SWEK configuration manager */
    private final SWEKConfigurationManager configManager;

    /** Instance of the treeModel */
    private final SWEKTreeModel treeModelInstance;

    private SWEKPluginPanel() {
        configManager = SWEKConfigurationManager.getSingletonInstance();
        treeModelInstance = SWEKTreeModel.getSingletonInstance();
        treeModelInstance.addSWEKTreeModelListener(this);
        initVisualComponents();
        this.revalidate();
        this.repaint();

    }

    /**
     * Initializes the visual components.
     */
    private void initVisualComponents() {
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(150, 200));
        JPanel eventTypePanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(eventTypePanel, BoxLayout.Y_AXIS);
        eventTypePanel.setLayout(boxLayout);
        for (SWEKEventType eventType : configManager.getEventTypes().values()) {
            EventPanel eventPanel = new EventPanel(eventType);
            eventPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            eventTypePanel.add(eventPanel);
        }
        JScrollPane sp = new JScrollPane(eventTypePanel);
        add(sp, BorderLayout.CENTER);
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

    @Override
    public void expansionChanged() {
        super.revalidate();
        super.repaint();
    }
}
