package org.helioviewer.jhv.gui.component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.InputMapUIResource;

import com.formdev.flatlaf.jideoss.ui.FlatRangeSliderUI;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.swing.RangeSlider;

// Range slider over [min, max] in steps of 1/scale; values are exact multiples of 1/scale.
@SuppressWarnings("serial")
public final class JHVRangeSlider extends RangeSlider {

    private final int scale;

    @Override
    public void updateUI() {
        LookAndFeelFactory.installJideExtension();
        setUI(new FlatRangeSliderUI() {
            @Override
            protected void installAdditionalKeyboardActions(JSlider slider) {
                // JIDE adds range-only shortcuts to otherwise shared slider maps.
                InputMap keys = new InputMapUIResource();
                keys.setParent(SwingUtilities.getUIInputMap(slider, JComponent.WHEN_FOCUSED));
                SwingUtilities.replaceUIInputMap(slider, JComponent.WHEN_FOCUSED, keys);
                ActionMap actions = new ActionMapUIResource();
                actions.setParent(SwingUtilities.getUIActionMap(slider));
                SwingUtilities.replaceUIActionMap(slider, actions);
                super.installAdditionalKeyboardActions(slider);
            }
        });
    }

    public JHVRangeSlider(double min, double max, double low, double high, int _scale) {
        super(JHVSlider.toStep(min, _scale), JHVSlider.toStep(max, _scale), JHVSlider.toStep(low, _scale), JHVSlider.toStep(high, _scale));
        scale = _scale;
        setRangeDraggable(true);
        WheelSupport.installMouseWheelSupport(this);

        int defaultLow = getLowValue();
        int defaultHigh = getHighValue();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();
                    // one model update: a single change event, both ends at once
                    getModel().setRangeProperties(defaultLow, defaultHigh - defaultLow, getMinimum(), getMaximum(), false);
                }
            }
        });
    }

    public double getLowDouble() {
        return getLowValue() / (double) scale;
    }

    public double getHighDouble() {
        return getHighValue() / (double) scale;
    }

}
