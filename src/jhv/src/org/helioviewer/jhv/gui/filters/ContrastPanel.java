package org.helioviewer.jhv.gui.filters;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodelplugin.filter.FilterAlignmentDetails;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager.Area;

/**
 * Panel containing a slider for changing the contrast of the image.
 *
 * @author Markus Langenberg
 */
public class ContrastPanel extends AbstractFilterPanel implements ChangeListener, MouseListener, FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;
    private static final float sliderToContrastScale = 25.0f;

    private final JSlider contrastSlider;
    private final JLabel title;
    private final JLabel contrastLabel;
    private JHVJP2View jp2view;

    /**
     * Default constructor.
     */
    public ContrastPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Contrast");
        title.setPreferredSize(new Dimension(FilterPanel.titleWidth, FilterPanel.height));
        add(title);

        contrastSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        contrastSlider.setMajorTickSpacing(25 * 2); // twice wider
        contrastSlider.setPaintTicks(true);
        contrastSlider.setPreferredSize(new Dimension(150, contrastSlider.getPreferredSize().height));
        contrastSlider.addMouseListener(this);
        contrastSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(contrastSlider);
        add(contrastSlider);

        contrastLabel = new JLabel("0");
        contrastLabel.setHorizontalAlignment(JLabel.RIGHT);
        contrastLabel.setPreferredSize(new Dimension(FilterPanel.valueWidth, FilterPanel.height));
        add(contrastLabel);
    }

    public Area getArea() {
        return Area.TOP;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        jp2view.setContrast(contrastSlider.getValue() / sliderToContrastScale);
        contrastLabel.setText(Integer.toString(contrastSlider.getValue()));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int sliderValue = contrastSlider.getValue();
        if (sliderValue <= 2 && sliderValue >= -2 && sliderValue != 0) {
            contrastSlider.setValue(0);
        }
    }

    @Override
    public int getDetails() {
        return FilterAlignmentDetails.POSITION_CONTRAST;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        contrastSlider.setEnabled(enabled);
        contrastLabel.setEnabled(enabled);
        title.setEnabled(enabled);
    }

    void setValue(float contrast) {
        contrastSlider.setValue((int) (contrast * sliderToContrastScale));
    }

}
