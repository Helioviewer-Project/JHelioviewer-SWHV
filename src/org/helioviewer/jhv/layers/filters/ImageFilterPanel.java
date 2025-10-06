package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

import com.jidesoft.swing.JideSplitButton;

public class ImageFilterPanel implements FilterDetails {

    private final JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private final JPanel buttonPanel = new JPanel(new BorderLayout());
    private final JLabel title = new JLabel("Filter ", JLabel.RIGHT);

    private static String formatLabel(double value) {
        return String.format("%.1f", value);
    }

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

        JHVSlider slider = new JHVSlider(0, 30, (int) (layer.getGLImage().getEnhanced() * 10));
        JLabel label = new JLabel(formatLabel(slider.getValue() / 10.), JLabel.RIGHT);
        label.setToolTipText("<html><body>pixel\u22C5(R-R\u2609)<sup>v");
        slider.addChangeListener(e -> {
            double value = slider.getValue() / 10.;
            layer.getGLImage().setEnhanced(value);
            label.setText(formatLabel(value));
            MovieDisplay.display();
        });
        JPanel enhancePanel = new JPanel(new BorderLayout());
        enhancePanel.add(slider, BorderLayout.LINE_START);
        enhancePanel.add(label, BorderLayout.LINE_END);

        JideSplitButton enhanceButton = new JideSplitButton(Buttons.corona);
        enhanceButton.setToolTipText("Enhance radially the off-disk corona");
        enhanceButton.setAlwaysDropdown(true);
        enhanceButton.add(enhancePanel);

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
