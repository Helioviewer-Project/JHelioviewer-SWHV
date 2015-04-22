package org.helioviewer.plugins.eveplugin.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.plugins.eveplugin.events.model.EventModelListener;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.IntervalOptionPanel;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorPanel;

/**
 *
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class ControlsPanel extends JPanel implements ActionListener, EventModelListener {

    private static final long serialVersionUID = 3639870635351984819L;

    private final JPanel lineDataSelectorContainer = new JPanel();
    private final ImageIcon addIcon = IconBank.getIcon(JHVIcon.ADD);
    private final JButton addLayerButton = new JButton("Add layer", addIcon);
    private final LineDataSelectorPanel lineDataSelectorPanel = new LineDataSelectorPanel("Plot 1:");

    public ControlsPanel() {
        initVisualComponents();
    }

    private void initVisualComponents() {
        EventModel.getSingletonInstance().addEventModelListener(this);

        addLayerButton.setToolTipText("Add a new layer");
        addLayerButton.addActionListener(this);
        addLayerButton.setMargin(new Insets(0, 0, 0, 0));

        // this.setPreferredSize(new Dimension(100, 300));
        lineDataSelectorContainer.setLayout(new BoxLayout(lineDataSelectorContainer, BoxLayout.Y_AXIS));
        lineDataSelectorContainer.setPreferredSize(new Dimension(100, 130));

        this.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        add(lineDataSelectorContainer, gc);
        lineDataSelectorContainer.add(lineDataSelectorPanel);
        JPanel pageEndPanel = new JPanel();
        pageEndPanel.setBackground(Color.BLUE);

        GridBagConstraints gbc = new GridBagConstraints();

        JPanel flowPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        flowPanel.add(new IntervalOptionPanel());

        gbc.gridx = 1;
        flowPanel.add(addLayerButton, gbc);

        gc.gridy = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.weighty = 1.0;
        add(flowPanel, gc);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(addLayerButton)) {
            ImageViewerGui.getSingletonInstance().getObservationDialog().showDialog(EVESettings.OBSERVATION_UI_NAME);
        }
    }

    @Override
    public void eventsDeactivated() {
        repaint();
    }

}
