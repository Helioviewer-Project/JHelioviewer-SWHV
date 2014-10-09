package org.helioviewer.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorPanel;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class ControlsPanel extends JPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 3639870635351984819L;

    private static ControlsPanel singletongInstance;
    private final JPanel lineDataSelectorContainer = new JPanel();
    private final ImageIcon addIcon = IconBank.getIcon(JHVIcon.ADD);
    private final JButton addLayerButton = new JButton("Add Layer", addIcon);

    private ControlsPanel() {
        initVisualComponents();
    }

    private void initVisualComponents() {
        addLayerButton.setToolTipText("Add a new layer");
        addLayerButton.addActionListener(this);

        // this.setPreferredSize(new Dimension(100, 300));
        lineDataSelectorContainer.setLayout(new BoxLayout(lineDataSelectorContainer, BoxLayout.Y_AXIS));
        lineDataSelectorContainer.setPreferredSize(new Dimension(100, 300));
        this.setLayout(new BorderLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        add(lineDataSelectorContainer, BorderLayout.CENTER);

        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        flowPanel.add(addLayerButton);
        add(flowPanel, BorderLayout.PAGE_END);

    }

    public static ControlsPanel getSingletonInstance() {
        if (singletongInstance == null) {
            singletongInstance = new ControlsPanel();
        }

        return singletongInstance;
    }

    public void addLineDataSelector(LineDataSelectorPanel lineDataSelectorPanel) {
        lineDataSelectorContainer.add(lineDataSelectorPanel);
    }

    public void removeLineDataSelector(LineDataSelectorPanel lineDataSelectorPanel) {
        lineDataSelectorContainer.remove(lineDataSelectorPanel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(addLayerButton)) {
            ObservationDialog.getSingletonInstance().showDialog(EVESettings.OBSERVATION_UI_NAME);
        }
    }
}
