package org.helioviewer.jhv.plugins.swek.view.filter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.data.container.cache.SWEKOperand;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.plugins.swek.view.FilterDialog;

public class FilterPanelFactory {

    private static String getSpinnerFormat(double minimumValue, double maximumValue) {
        StringBuilder spinnerFormat = new StringBuilder("0");
        String minString = (new BigDecimal(minimumValue, new MathContext(12))).stripTrailingZeros().toPlainString();
        String maxString = (new BigDecimal(maximumValue, new MathContext(12))).stripTrailingZeros().toPlainString();
        int integerPlacesMin = minString.indexOf('.');
        int decimalPlacesMin = minString.length() - integerPlacesMin - 1;
        int integerPlacesMax = maxString.indexOf('.');
        int decimalPlacesMax = maxString.length() - integerPlacesMax - 1;
        if (integerPlacesMax != -1 && integerPlacesMin != -1) {
            spinnerFormat.append('.');
            for (int i = 0; i < Math.max(decimalPlacesMax, decimalPlacesMin); i++) {
                spinnerFormat.append('0');
            }
        }
        return spinnerFormat.toString();
    }

    private static JSpinner generateFlareSpinner(FilterDialog filterDialog) {
        /*
        double min = parameter.getParameterFilter().getMin() == null ? 1e-8 : parameter.getParameterFilter().getMin();
        double max = parameter.getParameterFilter().getMax() == null ? 1e-3 : parameter.getParameterFilter().getMin();
        double start = parameter.getParameterFilter().getStartValue() == null ? 1e-5 : parameter.getParameterFilter().getMin();
        double step = parameter.getParameterFilter().getStepSize() == null ? 0.1 : parameter.getParameterFilter().getMin();
         */
        FlareSpinnerModel minimumSpinnerModel = new FlareSpinnerModel("A1.0", "X20.0", "C1.0", 0.2);
        JSpinner spinner = new JSpinner(minimumSpinnerModel);
        spinner.setEditor(new JSpinner.DefaultEditor(spinner));
        spinner.addChangeListener(e -> filterDialog.filterParameterChanged());
        WheelSupport.installMouseWheelSupport(spinner);
        return spinner;
    }

    private static JSpinner generateMinOrMaxSpinner(FilterDialog filterDialog, SWEKParameter parameter) {
        double min = parameter.getParameterFilter().getMin() == null ? Double.MIN_VALUE : parameter.getParameterFilter().getMin();
        double max = parameter.getParameterFilter().getMax() == null ? Double.MAX_VALUE : parameter.getParameterFilter().getMax();
        double start = parameter.getParameterFilter().getStartValue() == null ? (max - min) * 0.5 : parameter.getParameterFilter().getStartValue();
        double step = parameter.getParameterFilter().getStepSize() == null ? (max - min) * 0.01 : parameter.getParameterFilter().getStepSize();

        SpinnerModel minimumSpinnerModel = new SpinnerNumberModel(start, min, max, step);
        JSpinner spinner = new JSpinner(minimumSpinnerModel);
        spinner.setEditor(new JSpinner.NumberEditor(spinner, getSpinnerFormat(min, max)));
        spinner.addChangeListener(e -> filterDialog.filterParameterChanged());
        WheelSupport.installMouseWheelSupport(spinner);
        return spinner;
    }

    public static List<FilterPanel> createFilterPanel(SWEKEventType eventType, FilterDialog filterDialog) {
        List<FilterPanel> panels = new ArrayList<>();
        for (SWEKParameter parameter : eventType.getParameterList()) {
            if (parameter.getParameterFilter() != null) {
                String filterType = parameter.getParameterFilter().getFilterType().toLowerCase();
                switch (filterType) {
                    case "doublemaxfilter":
                        panels.add(new FilterPanel(eventType, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.BIGGER_OR_EQUAL));
                        break;
                    case "doubleminfilter":
                        panels.add(new FilterPanel(eventType, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.SMALLER_OR_EQUAL));
                        break;
                    case "doubleminmaxfilter":
                        panels.add(new FilterPanel(eventType, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.BIGGER_OR_EQUAL));
                        panels.add(new FilterPanel(eventType, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.SMALLER_OR_EQUAL));
                        break;
                    case "flarefilter":
                        panels.add(new FilterPanel(eventType, parameter, generateFlareSpinner(filterDialog), filterDialog, SWEKOperand.BIGGER_OR_EQUAL));
                        break;
                }
            }
        }
        return panels;
    }

}
