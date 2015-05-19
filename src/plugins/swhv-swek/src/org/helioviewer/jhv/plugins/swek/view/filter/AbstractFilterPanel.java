package org.helioviewer.jhv.plugins.swek.view.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.MathContext;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.download.FilterManager;

/**
 * An abstract representation of a filter panel. Two abstract functions are
 * provided: the filter function and the initFilterComponents function. These
 * functions must be implemented. They respectively represent the functions
 * called when the filter button is pressed and the creation of the filter
 * specific visual components.
 * 
 * @author Bram.Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
@SuppressWarnings({"serial"})
public abstract class AbstractFilterPanel extends JPanel {

    /** the SWEK parameter for this filter */
    protected SWEKParameter parameter;
    /** the SWEK the event type */
    protected SWEKEventType eventType;
    /** The minimum value of the filter */
    protected Double min;
    /** The maximum value of the filter */
    protected Double max;
    /** The middle value of the filter */
    protected Double middleValue;
    /** The stepSize */
    protected Double stepSize;
    /** The filter manager instance */
    protected String units;

    protected final FilterManager filterManager;

    protected final JToggleButton filterToggleButton = new JToggleButton("Filter");

    /**
     * Creates an abstract Filter panel.
     * 
     * @param parameter
     *            the parameter to filter
     * @param eventType
     *            the event type
     */
    public AbstractFilterPanel(SWEKEventType eventType, SWEKParameter parameter) {
        this.parameter = parameter;
        this.eventType = eventType;
        filterManager = FilterManager.getSingletonInstance();
        initValues();
        initVisualComponents();
    }

    /**
     * Method should handle the filter call. Pass the input to the responsible
     * classes to handle the filtering.
     * 
     * @param activ
     *            true if the filter is activated, false if the filter is not
     *            activated
     */
    public abstract void filter(boolean active);

    /**
     * Creates the visual component of the specific filter.
     * 
     * @return the component representing the filter.
     */
    public abstract JComponent initFilterComponents();

    protected String getSpinnerFormat(double minimumValue, double maximumValue) {
        StringBuilder spinnerFormat = new StringBuilder("0");
        String minString = (new BigDecimal(minimumValue, new MathContext(12))).stripTrailingZeros().toPlainString();
        String maxString = (new BigDecimal(maximumValue, new MathContext(12))).stripTrailingZeros().toPlainString();
        int integerPlacesMin = minString.indexOf('.');
        int decimalPlacesMin = minString.length() - integerPlacesMin - 1;
        int integerPlacesMax = maxString.indexOf('.');
        int decimalPlacesMax = maxString.length() - integerPlacesMax - 1;
        if (integerPlacesMax != -1 && integerPlacesMin != -1) {
            spinnerFormat.append(".");
            for (int i = 0; i < Math.max(decimalPlacesMax, decimalPlacesMin); i++) {
                spinnerFormat.append("0");
            }
        }
        return spinnerFormat.toString();
    }

    /**
     * Creates the filter panel.
     */
    private void initVisualComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(Color.white);
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder(parameter.getParameterDisplayName());
        JComponent filter = initFilterComponents();
        contentPanel.setPreferredSize(new Dimension(Math.max((int) border.getMinimumSize(contentPanel).getWidth(), (int) filter.getMinimumSize().getWidth()) + 20, ((int) filter.getMinimumSize().getHeight()) + 55));
        contentPanel.setBorder(BorderFactory.createTitledBorder(parameter.getParameterDisplayName()));
        contentPanel.add(initFilterComponents(), BorderLayout.CENTER);
        filterToggleButton.setSelected(filterManager.isFiltered(eventType, parameter));
        filterToggleButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                filter(filterToggleButton.isSelected());
            }
        });
        contentPanel.add(filterToggleButton, BorderLayout.PAGE_END);
        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Initializes the min, max, middle and step size values based on the
     * information found in the parameter.
     */
    private void initValues() {
        min = parameter.getParameterFilter().getMin() == null ? Double.MIN_VALUE : parameter.getParameterFilter().getMin();
        max = parameter.getParameterFilter().getMax() == null ? Double.MAX_VALUE : parameter.getParameterFilter().getMax();
        stepSize = parameter.getParameterFilter().getStepSize() == null ? 0.1 : parameter.getParameterFilter().getStepSize();
        middleValue = parameter.getParameterFilter().getStartValue() == null ? (min + max) / 2 : parameter.getParameterFilter().getStartValue();
        units = parameter.getParameterFilter().getUnits();
    }

}
