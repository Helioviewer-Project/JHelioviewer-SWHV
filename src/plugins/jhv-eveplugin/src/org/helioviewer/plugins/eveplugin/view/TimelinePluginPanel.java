package org.helioviewer.plugins.eveplugin.view;

import java.awt.GridBagConstraints;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class TimelinePluginPanel extends JPanel {

    private static final long serialVersionUID = -2175620741820580148L;
    private JTabbedPane tabs;

    public TimelinePluginPanel() {
        super();
        initVisualComponents();
    }

    private void initVisualComponents() {
        tabs = new JTabbedPane();
        tabs.addTab("Layers", ControlsPanel.getSingletonInstance());
        tabs.addTab("Radio Options", new RadioOptionsPanel());

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        add(tabs, gc);
    }
}
