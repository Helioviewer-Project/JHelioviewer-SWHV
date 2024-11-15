package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.GLImage;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

public class DifferencePanel implements FilterDetails {

    private final JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private final JPanel buttonPanel = new JPanel(new BorderLayout());
    private final JLabel title = new JLabel(" Difference ", JLabel.RIGHT);

    public DifferencePanel(ImageLayer layer) {
        ButtonGroup modeGroup = new ButtonGroup();
        for (GLImage.DifferenceMode mode : GLImage.DifferenceMode.values()) {
            JRadioButton item = new JRadioButton(mode.toString());
            if (mode == layer.getGLImage().getDifferenceMode())
                item.setSelected(true);
            item.addActionListener(e -> {
                layer.getGLImage().setDifferenceMode(mode);
                MovieDisplay.display();
            });
            modeGroup.add(item);
            modePanel.add(item);
        }

        JideButton syncButton = new JideButton(Buttons.sync);
        syncButton.setToolTipText("Synchronize time intervals of other layers");
        syncButton.addActionListener(e -> MoviePanel.getInstance().syncLayersSpan(layer.getStartTime(), layer.getEndTime()));
        buttonPanel.add(syncButton, BorderLayout.LINE_END);
    }

    @Override
    public Component getFirst() {
        return title;
    }

    @Override
    public Component getSecond() {
        return modePanel;
    }

    @Override
    public Component getThird() {
        return buttonPanel;
    }

}
