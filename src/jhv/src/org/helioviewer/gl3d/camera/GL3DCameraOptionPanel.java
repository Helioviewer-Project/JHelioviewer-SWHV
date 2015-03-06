package org.helioviewer.gl3d.camera;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public abstract class GL3DCameraOptionPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    abstract public void deactivate();

    private JPanel gridPanel;
    private JSpinner gridResolutionXSpinner;
    private JSpinner gridResolutionYSpinner;

    private final FontComboBox fontComboBox;

    private final GL3DCamera camera;
    private JSpinner fontSizeSpinner;
    private JButton visibleButton;
    private boolean gridVisible = false;

    public GL3DCameraOptionPanel(GL3DCamera camera) {
        this.camera = camera;
        fontComboBox = new FontComboBox();
        fontComboBox.addActionListener(this);
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    }

    public void createGridOptions() {
        this.gridPanel = new JPanel();
        this.gridPanel.setLayout(new BoxLayout(gridPanel, BoxLayout.LINE_AXIS));
        this.gridPanel.add(new JLabel("Grid "));
        this.createGridResolutionX();
        this.createGridResolutionY();

        this.gridResolutionXSpinner.setMinimumSize(new Dimension(42, 20));
        this.gridResolutionXSpinner.setPreferredSize(new Dimension(62, 22));
        this.gridResolutionXSpinner.setMaximumSize(new Dimension(82, 22));

        this.gridPanel.add(this.gridResolutionXSpinner);
        this.gridPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        this.gridResolutionYSpinner.setMinimumSize(new Dimension(42, 20));
        this.gridResolutionYSpinner.setPreferredSize(new Dimension(62, 22));
        this.gridResolutionYSpinner.setMaximumSize(new Dimension(82, 22));

        this.gridPanel.add(this.gridResolutionYSpinner);
        this.gridPanel.add(Box.createHorizontalGlue());

        this.createFontSizeSpinner();

        createGridVisibleToggleButton();
        this.gridPanel.add(visibleButton);

        add(this.gridPanel);

        //add(this.fontComboBox);
    }

    private void createGridVisibleToggleButton() {
        visibleButton = new JButton(IconBank.getIcon(JHVIcon.HIDDEN));
        visibleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (gridVisible) {
                    camera.getGrid().getDrawBits().on(Bit.Hidden);
                    camera.getFollowGrid().getDrawBits().on(Bit.Hidden);
                    visibleButton.setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
                } else {
                    camera.getGrid().getDrawBits().off(Bit.Hidden);
                    camera.getFollowGrid().getDrawBits().off(Bit.Hidden);
                    visibleButton.setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
                }
                gridVisible = !gridVisible;
                Displayer.getSingletonInstance().display();
            }
        });
        visibleButton.setToolTipText("Toggle visibility");
    }

    public void createGridResolutionX() {
        this.gridResolutionXSpinner = new JSpinner();
        this.gridResolutionXSpinner.setModel(new SpinnerNumberModel(new Double(13.2), new Double(1), new Double(90), new Double(0.1)));
        camera.setGridResolutionX((Double) gridResolutionXSpinner.getValue());
        this.gridResolutionXSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                camera.setGridResolutionX((Double) gridResolutionXSpinner.getValue());
                Displayer.getSingletonInstance().render();
            }
        });
        WheelSupport.installMouseWheelSupport(this.gridResolutionXSpinner);
    }

    public void createGridResolutionY() {
        this.gridResolutionYSpinner = new JSpinner();
        this.gridResolutionYSpinner.setModel(new SpinnerNumberModel(new Double(15.), new Double(1), new Double(90), new Double(0.1)));
        camera.setGridResolutionY((Double) gridResolutionYSpinner.getValue());
        this.gridResolutionYSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                camera.setGridResolutionY((Double) gridResolutionYSpinner.getValue());
                Displayer.getSingletonInstance().render();
            }
        });
        WheelSupport.installMouseWheelSupport(this.gridResolutionYSpinner);
    }

    public void createFontSizeSpinner() {
        this.fontSizeSpinner = new JSpinner();
        this.fontSizeSpinner.setModel(new SpinnerNumberModel(new Float(0.8f), new Float(0.3f), new Float(2.5f), new Float(0.05f)));
        this.fontSizeSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                camera.getGrid().setFontScale((Float) (fontSizeSpinner.getValue()));
                Displayer.getSingletonInstance().render();
            }
        });
        WheelSupport.installMouseWheelSupport(this.fontSizeSpinner);
    }

    public JSpinner getGridResolutionXSpinner() {
        return gridResolutionXSpinner;
    }

    public JSpinner getGridResolutionYSpinner() {
        return gridResolutionYSpinner;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        JComboBox source = (JComboBox) evt.getSource();
        String item = (String) source.getSelectedItem();
        camera.getGrid().setFont(item);
    }

    public void setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
        if (gridVisible) {
            visibleButton.setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
        } else {
            visibleButton.setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
        }
    }

}
