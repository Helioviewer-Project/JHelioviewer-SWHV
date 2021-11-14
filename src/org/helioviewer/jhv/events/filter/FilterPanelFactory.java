package org.helioviewer.jhv.events.filter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKOperand;
import org.helioviewer.jhv.events.SWEKParameter;
import org.helioviewer.jhv.events.SWEKParameterFilter;
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
        SWEKParameterFilter filter = parameter.filter();
        double min = filter.min() == null ? 1e-8 : filter.min();
        double max = filter.max() == null ? 1e-3 : filter.max();
        double start = filter.startValue() == null ? 1e-5 : filter.startValue();
        double step = filter.stepSize() == null ? 0.5 : filter.stepSize();

        FlareSpinnerModel minimumSpinnerModel = new FlareSpinnerModel(start, min, max, step);
        JSpinner spinner = new JSpinner(minimumSpinnerModel);
        spinner.setEditor(new JSpinner.DefaultEditor(spinner));
        spinner.addChangeListener(e -> filterDialog.filterParameterChanged());
        WheelSupport.installMouseWheelSupport(spinner);
        return spinner;
    }

    private static JSpinner generateMinOrMaxSpinner(FilterDialog filterDialog, SWEKParameter parameter) {
        SWEKParameterFilter filter = parameter.filter();
        double min = filter.min() == null ? Double.MIN_VALUE : filter.min();
        double max = filter.max() == null ? Double.MAX_VALUE : filter.max();
        double start = filter.startValue() == null ? (max - min) * 0.5 : filter.startValue();
        double step = filter.stepSize() == null ? (max - min) * 0.01 : filter.stepSize();

        SpinnerModel minimumSpinnerModel = new SpinnerNumberModel(start, min, max, step);
        JSpinner spinner = new JSpinner(minimumSpinnerModel);
        spinner.setEditor(new JSpinner.NumberEditor(spinner, getSpinnerFormat(min, max)));
        spinner.addChangeListener(e -> filterDialog.filterParameterChanged());
        WheelSupport.installMouseWheelSupport(spinner);
        return spinner;
    }

    static List<FilterPanel> createFilterPanel(SWEKGroup group, SWEKSupplier supplier, FilterDialog filterDialog, boolean enabled) {
        List<FilterPanel> panels = new ArrayList<>();
        for (SWEKParameter parameter : group.getParameterList()) {
            SWEKParameterFilter filter = parameter.filter();
            if (filter != null) {
                String filterType = filter.type().toLowerCase();
                switch (filterType) {
                    case "doublemaxfilter" -> panels.add(new FilterPanel(supplier, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.BIGGER_OR_EQUAL, enabled));
                    case "doubleminfilter" -> panels.add(new FilterPanel(supplier, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.SMALLER_OR_EQUAL, enabled));
                    case "doubleminmaxfilter" -> {
                        panels.add(new FilterPanel(supplier, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.BIGGER_OR_EQUAL, enabled));
                        panels.add(new FilterPanel(supplier, parameter, generateMinOrMaxSpinner(filterDialog, parameter), filterDialog, SWEKOperand.SMALLER_OR_EQUAL, enabled));
                    }
                    case "flarefilter" -> panels.add(new FilterPanel(supplier, parameter, generateFlareSpinner(filterDialog, parameter), filterDialog, SWEKOperand.BIGGER_OR_EQUAL, enabled));
                    default -> {
                    }
                }
            }
        }
        return panels;
    }

}
