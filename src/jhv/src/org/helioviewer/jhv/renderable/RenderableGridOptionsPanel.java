package org.helioviewer.jhv.renderable;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

public class RenderableGridOptionsPanel extends JPanel {
    private JSpinner gridResolutionXSpinner;
    private JSpinner gridResolutionYSpinner;
    RenderableGrid grid;

    public RenderableGridOptionsPanel(RenderableGrid renderableGrid) {
        grid = renderableGrid;
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        add(new JLabel("Grid "));
        createGridResolutionX(renderableGrid);
        createGridResolutionY(renderableGrid);

        gridResolutionXSpinner.setMinimumSize(new Dimension(42, 20));
        gridResolutionXSpinner.setPreferredSize(new Dimension(62, 22));
        gridResolutionXSpinner.setMaximumSize(new Dimension(82, 22));

        add(gridResolutionXSpinner);
        add(Box.createRigidArea(new Dimension(5, 0)));
        gridResolutionYSpinner.setMinimumSize(new Dimension(42, 20));
        gridResolutionYSpinner.setPreferredSize(new Dimension(62, 22));
        gridResolutionYSpinner.setMaximumSize(new Dimension(82, 22));

        add(gridResolutionYSpinner);
        add(Box.createHorizontalGlue());
    }

    public void createGridResolutionX(RenderableGrid renderableGrid) {
        gridResolutionXSpinner = new JSpinner();
        gridResolutionXSpinner.setModel(new SpinnerNumberModel(new Double(renderableGrid.getLonstepDegrees()), new Double(1), new Double(90), new Double(0.1)));
        gridResolutionXSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                grid.setLonstepDegrees((Double) gridResolutionXSpinner.getValue());
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(gridResolutionXSpinner);
    }

    public void createGridResolutionY(RenderableGrid renderableGrid) {
        gridResolutionYSpinner = new JSpinner();
        gridResolutionYSpinner.setModel(new SpinnerNumberModel(new Double(renderableGrid.getLatstepDegrees()), new Double(1), new Double(90), new Double(0.1)));
        gridResolutionYSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                grid.setLatstepDegrees((Double) gridResolutionYSpinner.getValue());
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(gridResolutionYSpinner);
    }
}
