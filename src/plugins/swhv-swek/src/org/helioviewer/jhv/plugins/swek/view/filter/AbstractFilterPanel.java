package org.helioviewer.jhv.plugins.swek.view.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

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
public abstract class AbstractFilterPanel extends JPanel {

    /** The UID. */
    private static final long serialVersionUID = 8128418401123128270L;
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
    protected final FilterManager filterManager;

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
     */
    public abstract void filter();

    /**
     * Creates the visual component of the specific filter.
     * 
     * @return the component representing the filter.
     */
    public abstract JComponent initFilterComponents();

    /**
     * Creates the filter panel.
     * 
     */
    private void initVisualComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(Color.white);
        setBorder(BorderFactory.createTitledBorder(parameter.getParameterDisplayName()));
        add(initFilterComponents(), BorderLayout.CENTER);
        JButton filter = new JButton("Filter");
        filter.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                filter();
            }
        });
        add(filter, BorderLayout.PAGE_END);
    }

    /**
     * Initializes the min, max, middle and step size values based on the
     * information found in the parameter.
     * 
     */
    private void initValues() {
        min = parameter.getParameterFilter().getMin() == null ? Double.MIN_VALUE : parameter.getParameterFilter().getMin();
        max = parameter.getParameterFilter().getMax() == null ? Double.MAX_VALUE : parameter.getParameterFilter().getMax();
        stepSize = parameter.getParameterFilter().getStepSize() == null ? 0.1 : parameter.getParameterFilter().getStepSize();
        middleValue = parameter.getParameterFilter().getStartValue() == null ? (min + max) / 2 : parameter.getParameterFilter()
                .getStartValue();
    }
}
