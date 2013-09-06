package org.helioviewer.jhv.internal_plugins.filter.contrast;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.gui.components.WheelSupport;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodelplugin.filter.FilterAlignmentDetails;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager.Area;

/**
 * Panel containing a slider for changing the contrast of the image.
 * 
 * @author Markus Langenberg
 */
public class ContrastPanel extends FilterPanel implements ChangeListener, MouseListener, FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;
    private static final float sliderToContrastScale = 25.0f;

    private JSlider contrastSlider;
    private JLabel title;
    private JLabel contrastLabel;
    private ContrastFilter filter;

    /**
     * Default constructor.
     */
    public ContrastPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Contrast:");
        title.setPreferredSize(new Dimension(FilterPanel.titleWidth, FilterPanel.height));
        add(title);

        contrastSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        contrastSlider.setMajorTickSpacing(20);
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

        setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */

    public void setFilter(Filter filter) {
        if (filter instanceof ContrastFilter) {
            this.filter = (ContrastFilter) filter;
            this.filter.setPanel(this);
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */

    public Area getArea() {
        return Area.TOP;
    }

    /**
     * Sets the gamma value of the image.
     */
    public void stateChanged(ChangeEvent e) {
        filter.setContrast((float) contrastSlider.getValue() / sliderToContrastScale);
        contrastLabel.setText(Integer.toString(contrastSlider.getValue()));
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, snaps the slider to 0 if it is close to it.
     */
    public void mouseReleased(MouseEvent e) {
        int sliderValue = contrastSlider.getValue();

        if (sliderValue <= 2 && sliderValue >= -2 && sliderValue != 0) {
            contrastSlider.setValue(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getDetails() {
        return FilterAlignmentDetails.POSITION_CONTRAST;
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        contrastSlider.setEnabled(enabled);
        contrastLabel.setEnabled(enabled);
        title.setEnabled(enabled);
    }

    /**
     * Sets the panel values.
     * 
     * This may be useful, if the values are changed from another source than
     * the panel itself.
     * 
     * @param gamma
     *            New gamma value, must be within [0.1, 10]
     */
    void setValue(float contrast) {
        contrastSlider.setValue((int) (contrast * sliderToContrastScale));
    }
}
