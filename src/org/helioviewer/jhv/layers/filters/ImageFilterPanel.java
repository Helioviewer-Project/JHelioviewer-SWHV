package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

public class ImageFilterPanel implements FilterDetails {

    private final JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private final JPanel buttonPanel = new JPanel(new BorderLayout());
    private final JLabel title = new JLabel("Filter ", JLabel.RIGHT);

    public ImageFilterPanel(ImageLayer layer) {
        ButtonGroup modeGroup = new ButtonGroup();
        for (ImageFilter.Type type : ImageFilter.Type.values()) {
            JRadioButton item = new JRadioButton(type.toString());
            item.setToolTipText(type.description);
            if (type == layer.getView().getFilter())
                item.setSelected(true);
            item.addActionListener(e -> {
                layer.getView().clearCache();
                layer.getView().setFilter(type);
                MovieDisplay.render(1);
            });
            modeGroup.add(item);
            modePanel.add(item);
        }

        JideToggleButton enhanceButton = new JideToggleButton(Buttons.corona, layer.getGLImage().getEnhanced());
        enhanceButton.setToolTipText("Enhance off-disk corona");
        enhanceButton.addActionListener(e -> {
            layer.getGLImage().setEnhanced(enhanceButton.isSelected());
            MovieDisplay.display();
        });
        buttonPanel.add(enhanceButton, BorderLayout.LINE_END);
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
