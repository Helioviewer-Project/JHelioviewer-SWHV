package org.helioviewer.jhv.plugins.swek;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.gui.component.JHVSpinner;

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
            spinnerFormat.repeat("0", Math.max(0, Math.max(decimalPlacesMax, decimalPlacesMin)));
        }
        return spinnerFormat.toString();
    }

    private static JHVSpinner generateFlareSpinner(SWEK.Parameter parameter) {
        SWEK.ParameterFilter filter = parameter.filter();
        return new JHVSpinner(new FlareSpinnerModel(
                filter.startValue(), filter.min(), filter.max(), filter.stepSize()));
    }

    private static JHVSpinner generateMinOrMaxSpinner(SWEK.Parameter parameter) {
        SWEK.ParameterFilter filter = parameter.filter();
        double min = filter.min();
        double max = filter.max();

        JHVSpinner spinner = new JHVSpinner(filter.startValue(), min, max, filter.stepSize());
        spinner.setEditor(new JHVSpinner.NumberEditor(spinner, getSpinnerFormat(min, max)));
        return spinner;
    }

    static List<FilterPanel> createFilterPanel(SWEKSupplier supplier, FilterDialog filterDialog) {
        List<FilterPanel> panels = new ArrayList<>();
        for (SWEK.Parameter p : supplier.getParameterList()) {
            SWEK.ParameterFilter filter = p.filter();
            if (filter != null) {
                String filterType = filter.type().toLowerCase();
                switch (filterType) {
                    case "doublemaxfilter" ->
                            panels.add(new FilterPanel(p, generateMinOrMaxSpinner(p), filterDialog, SWEK.Operand.BIGGER_OR_EQUAL));
                    case "doubleminfilter" ->
                            panels.add(new FilterPanel(p, generateMinOrMaxSpinner(p), filterDialog, SWEK.Operand.SMALLER_OR_EQUAL));
                    case "doubleminmaxfilter" -> {
                        panels.add(new FilterPanel(p, generateMinOrMaxSpinner(p), filterDialog, SWEK.Operand.BIGGER_OR_EQUAL));
                        panels.add(new FilterPanel(p, generateMinOrMaxSpinner(p), filterDialog, SWEK.Operand.SMALLER_OR_EQUAL));
                    }
                    case "flarefilter" ->
                            panels.add(new FilterPanel(p, generateFlareSpinner(p), filterDialog, SWEK.Operand.BIGGER_OR_EQUAL));
                    default -> {}
                }
            }
        }
        return panels;
    }

}
