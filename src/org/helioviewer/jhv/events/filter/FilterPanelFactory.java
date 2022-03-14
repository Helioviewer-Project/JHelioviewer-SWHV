package org.helioviewer.jhv.events.filter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.events.SWEK;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.gui.components.base.JHVSpinner;

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

    private static JHVSpinner generateFlareSpinner(FilterDialog filterDialog, SWEK.Parameter parameter) {
        SWEK.ParameterFilter filter = parameter.filter();
        double min = filter.min() == null ? 1e-8 : filter.min();
        double max = filter.max() == null ? 1e-3 : filter.max();
        double start = filter.startValue() == null ? 1e-5 : filter.startValue();
        double step = filter.stepSize() == null ? 0.5 : filter.stepSize();

        JHVSpinner spinner = new JHVSpinner(new FlareSpinnerModel(start, min, max, step));
        spinner.addChangeListener(e -> filterDialog.filterParameterChanged());
        return spinner;
    }

    private static JHVSpinner generateMinOrMaxSpinner(FilterDialog filterDialog, SWEK.Parameter parameter) {
        SWEK.ParameterFilter filter = parameter.filter();
        double min = filter.min() == null ? Double.MIN_VALUE : filter.min();
        double max = filter.max() == null ? Double.MAX_VALUE : filter.max();
        double start = filter.startValue() == null ? (max - min) * 0.5 : filter.startValue();
        double step = filter.stepSize() == null ? (max - min) * 0.01 : filter.stepSize();

        JHVSpinner spinner = new JHVSpinner(start, min, max, step);
        spinner.setEditor(new JHVSpinner.NumberEditor(spinner, getSpinnerFormat(min, max)));
        spinner.addChangeListener(e -> filterDialog.filterParameterChanged());
        return spinner;
    }

    static List<FilterPanel> createFilterPanel(SWEKGroup group, SWEKSupplier supplier, FilterDialog filterDialog, boolean enabled) {
        List<FilterPanel> panels = new ArrayList<>();
        for (SWEK.Parameter p : group.getParameterList()) {
            SWEK.ParameterFilter filter = p.filter();
            if (filter != null) {
                String filterType = filter.type().toLowerCase();
                switch (filterType) {
                    case "doublemaxfilter" ->
                            panels.add(new FilterPanel(supplier, p, generateMinOrMaxSpinner(filterDialog, p), filterDialog, SWEK.Operand.BIGGER_OR_EQUAL, enabled));
                    case "doubleminfilter" ->
                            panels.add(new FilterPanel(supplier, p, generateMinOrMaxSpinner(filterDialog, p), filterDialog, SWEK.Operand.SMALLER_OR_EQUAL, enabled));
                    case "doubleminmaxfilter" -> {
                        panels.add(new FilterPanel(supplier, p, generateMinOrMaxSpinner(filterDialog, p), filterDialog, SWEK.Operand.BIGGER_OR_EQUAL, enabled));
                        panels.add(new FilterPanel(supplier, p, generateMinOrMaxSpinner(filterDialog, p), filterDialog, SWEK.Operand.SMALLER_OR_EQUAL, enabled));
                    }
                    case "flarefilter" ->
                            panels.add(new FilterPanel(supplier, p, generateFlareSpinner(filterDialog, p), filterDialog, SWEK.Operand.BIGGER_OR_EQUAL, enabled));
                    default -> {
                    }
                }
            }
        }
        return panels;
    }

}
