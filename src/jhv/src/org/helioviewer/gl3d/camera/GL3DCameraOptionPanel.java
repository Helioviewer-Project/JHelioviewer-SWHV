package org.helioviewer.gl3d.camera;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.basegui.components.WheelSupport;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.display.Displayer;

public abstract class GL3DCameraOptionPanel extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    abstract public void deactivate();

    private JPanel gridPanel;
    private JSpinner gridResolutionXSpinner;
    private JSpinner gridResolutionYSpinner;
    private JCheckBox gridVisibleCheckbox;
    private final FontComboBox fontComboBox;

    private final GL3DCamera camera;
    private JSpinner fontSizeSpinner;

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

        this.gridResolutionXSpinner.setMaximumSize(new Dimension(6, 22));
        this.gridPanel.add(this.gridResolutionXSpinner);
        this.gridPanel.add(Box.createHorizontalGlue());

        this.gridResolutionYSpinner.setMaximumSize(new Dimension(6, 22));
        this.gridPanel.add(this.gridResolutionYSpinner);
        this.gridPanel.add(Box.createHorizontalGlue());

        this.gridPanel.add(new JSeparator(SwingConstants.VERTICAL));
        this.gridPanel.add(Box.createHorizontalGlue());

        this.createFontSizeSpinner();
        this.gridPanel.add(fontSizeSpinner);

        createVisibleCheckBox();
        this.gridPanel.add(gridVisibleCheckbox);

        add(this.gridPanel);

        //add(this.fontComboBox);
    }

    private void createVisibleCheckBox() {
        gridVisibleCheckbox = new JCheckBox("Visible");
        gridVisibleCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    camera.getGrid().getDrawBits().on(Bit.Hidden);
                } else {
                    camera.getGrid().getDrawBits().off(Bit.Hidden);
                }
                Displayer.getSingletonInstance().display();

            }
        });
        gridVisibleCheckbox.setSelected(false);

    }

    public void createGridResolutionX() {
        this.gridResolutionXSpinner = new JSpinner();
        this.gridResolutionXSpinner.setModel(new SpinnerNumberModel(new Integer(20), new Integer(2), new Integer(250), new Integer(1)));
        camera.setGridResolutionX((Integer) gridResolutionXSpinner.getValue());
        this.gridResolutionXSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                camera.setGridResolutionX((Integer) gridResolutionXSpinner.getValue());
                Displayer.getSingletonInstance().render();
            }
        });
        WheelSupport.installMouseWheelSupport(this.gridResolutionXSpinner);
    }

    public void createGridResolutionY() {
        this.gridResolutionYSpinner = new JSpinner();
        this.gridResolutionYSpinner.setModel(new SpinnerNumberModel(new Integer(20), new Integer(2), new Integer(250), new Integer(1)));
        camera.setGridResolutionY((Integer) gridResolutionYSpinner.getValue());
        this.gridResolutionYSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                camera.setGridResolutionY((Integer) gridResolutionYSpinner.getValue());
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

    public JCheckBox getGridVisibleCheckbox() {
        return gridVisibleCheckbox;
    }

    public JSpinner getGridResolutionXSpinner() {
        return gridResolutionXSpinner;
    }

    public JSpinner getGridResolutionYSpinner() {
        return gridResolutionYSpinner;
    }

    public void setGridVisibleCheckbox(JCheckBox fovCheckbox) {
        this.gridVisibleCheckbox = fovCheckbox;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        JComboBox source = (JComboBox) evt.getSource();
        String item = (String) source.getSelectedItem();
        camera.getGrid().setFont(item);
    }
}
