package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayer;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.RangeSlider;

public class LevelsPanel implements FilterDetails {

    private final RangeSlider slider;
    private final JLabel label;
    private final JideButton autoButton;
    private final JPanel buttonPanel;

    static String align3(int value) {
        if (value < -99)
            return value + "%";
        if (value < -9)
            return "\u2007" + value + '%';
        if (value < 0)
            return "\u2007\u2007" + value + '%';
        if (value < 10)
            return "\u2007\u2007\u2007" + value + '%';
        if (value < 100)
            return "\u2007\u2007" + value + '%';
        return "\u2007" + value + '%';
    }

    private static String format(int low, int high) {
        return "<html>" + align3(low) + "<br/>" + align3(high);
    }

    public LevelsPanel(ImageLayer layer) {
        double offset = layer.getGLImage().getBrightOffset();
        double scale = layer.getGLImage().getBrightScale();
        int high = (int) (100 * (offset + scale));

        slider = new RangeSlider(-101, 201, (int) (offset * 100), high);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    slider.setLowValue(0);
                    slider.setHighValue(100);
                }
            }
        });
        slider.setRangeDraggable(true);
        label = new JLabel(format(slider.getLowValue(), slider.getHighValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setBrightness(lo / 100., (hi - lo) / 100.);
            label.setText(format(lo, hi));
            Displayer.display();
        });
        WheelSupport.installMouseWheelSupport(slider);

        autoButton = new JideButton(Buttons.brightness);
        autoButton.setToolTipText("Auto brightness");
        autoButton.addActionListener(e -> {
            slider.setLowValue(0);
            slider.setHighValue((int) (layer.getAutoBrightness() * 100));
        });

        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(label, BorderLayout.LINE_START);
        buttonPanel.add(autoButton, BorderLayout.LINE_END);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Levels", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return slider;
    }

    @Override
    public Component getLabel() {
        return buttonPanel;
    }

    public void syncFont() {
        autoButton.setFont(label.getFont());
    }

}
