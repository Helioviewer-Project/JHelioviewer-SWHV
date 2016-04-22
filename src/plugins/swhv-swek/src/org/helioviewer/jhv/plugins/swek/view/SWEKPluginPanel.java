package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelListener;

/**
 * The main visual component of the SWEK-plugin.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
@SuppressWarnings("serial")
public class SWEKPluginPanel extends JPanel implements SWEKTreeModelListener {

    private static SWEKPluginPanel swekPluginPanel;

    /** The SWEK configuration manager */
    private final SWEKConfigurationManager configManager;

    private SWEKPluginPanel() {
        configManager = SWEKConfigurationManager.getSingletonInstance();
        SWEKTreeModel.getSingletonInstance().addSWEKTreeModelListener(this);

        setLayout(new BorderLayout());
        // this.setPreferredSize(new Dimension(150, 200));
        JPanel eventTypePanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(eventTypePanel, BoxLayout.Y_AXIS);
        eventTypePanel.setLayout(boxLayout);
        for (SWEKEventType eventType : configManager.getOrderedEventTypes()) {
            EventPanel eventPanel = new EventPanel(eventType);
            eventPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            eventTypePanel.add(eventPanel);
        }
        // JScrollPane sp = new JScrollPane(eventTypePanel);
        add(eventTypePanel, BorderLayout.CENTER);
    }

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

    @Override
    public void startedDownloadingEventType(SWEKEventType eventType) {
    }

    @Override
    public void stoppedDownloadingEventType(SWEKEventType eventType) {
    }

}
