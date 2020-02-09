package org.helioviewer.jhv.events.filter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.events.SWEKOperand;
import org.helioviewer.jhv.events.SWEKParameter;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

class FilterPanelFactory {

    private static String getSpinnerFormat(double minimumValue, double maximumValue) {
        StringBuilder spinnerFormat = new StringBuilder("0");
        String minString = new BigDecimal(minimumValue, new MathContext(12)).stripTrailingZeros().toPlainString();
        String maxString = new BigDecimal(maximumValue, new MathContext(12)).stripTrailingZeros().toPlainString();
        int integerPlacesMin = minString.indexOf('.');
        int decimalPlacesMin = minString.length() - integerPlacesMin - 1;
        int integerPlacesMax = maxString.indexOf('.');
        int decimalPlacesMax = maxString.length() - integerPlacesMax - 1;
        if (integerPlacesMax != -1 && integerPlacesMin != -1) {
            spinnerFormat.append('.');
            spinnerFormat.append("0".repeat(Math.max(0, Math.max(decimalPlacesMax, decimalPlacesMin))));
        }
        return spinnerFormat.toString();
    }

    private static JSpinner generateFlareSpinner(FilterDialog filterDialog, SWEKParameter parameter) {
        double min = parameter.getParameterFilter().getMin() == null ? 1e-8 : parameter.getParameterFilter().getMin();
        double max = parameter.getParameterFilter().getMax() == null ? 1e-3 : parameter.getParameterFilter().getMax();
        double start = parameter.getParameterFilter().getStartValue() == null ? 1e-5 : parameter.getParameterFilter().getStartValue();
        double step = parameter.getParameterFilter().getStepSize() == null ? 0.5 : parameter.getParameterFilter().getStepSize();

        FlareSpinnerModel minimumSpinnerModel = new FlareSpinnerModel(start, min, max, step);
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

    static List<FilterPanel> createFilterPanel(SWEKSupplier supplier, FilterDialog filterDialog, boolean enabled) {
        List<FilterPanel> panels = new ArrayList<>();
        for (SWEKParameter parameter : supplier.getGroup().getParameterList()) {
            if (parameter.getParameterFilter() != null) {
                String filterType = parameter.getParameterFilter().getFilterType().toLowerCase();
                switch (filterType) {
                    case "doublemaxfilter":
                        panels.add(new FilterPanel(supplier, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.BIGGER_OR_EQUAL, enabled));
                        break;
                    case "doubleminfilter":
                        panels.add(new FilterPanel(supplier, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.SMALLER_OR_EQUAL, enabled));
                        break;
                    case "doubleminmaxfilter":
                        panels.add(new FilterPanel(supplier, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.BIGGER_OR_EQUAL, enabled));
                        panels.add(new FilterPanel(supplier, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.SMALLER_OR_EQUAL, enabled));
                        break;
                    case "flarefilter":
                        panels.add(new FilterPanel(supplier, parameter, generateFlareSpinner(filterDialog, parameter), filterDialog, SWEKOperand.BIGGER_OR_EQUAL, enabled));
                        break;
                    default:
                        break;
                }
            }
        }
        return panels;
    }

}
