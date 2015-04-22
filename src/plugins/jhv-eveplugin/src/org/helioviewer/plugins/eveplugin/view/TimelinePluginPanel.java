package org.helioviewer.plugins.eveplugin.view;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDateSelectorTablePanel;

public class TimelinePluginPanel extends JPanel {

    private static final long serialVersionUID = -2175620741820580148L;
    private JTabbedPane tabs;

    public TimelinePluginPanel() {
        super();
        initVisualComponents();
    }

    private void initVisualComponents() {
        tabs = new JTabbedPane();
        tabs.addTab("Layers", new ControlsPanel());
        tabs.addTab("Radio Options", new RadioOptionsPanel());

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(tabs);
        add(new LineDateSelectorTablePanel());
    }
}
