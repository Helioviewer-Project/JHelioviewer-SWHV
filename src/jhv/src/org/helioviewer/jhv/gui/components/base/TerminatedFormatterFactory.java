package org.helioviewer.jhv.gui.components.base;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.text.DefaultFormatter;

import org.helioviewer.jhv.base.logging.Log;

@SuppressWarnings("serial")
public class TerminatedFormatterFactory extends AbstractFormatterFactory {

    private final String format;
    private final String terminator;
    private final double min, max;

    public TerminatedFormatterFactory(String format, String terminator, double min, double max) {
        this.format = format;
        this.terminator = terminator;

        if (min > max)
            max = min;

        this.min = min;
        this.max = max;
    }

    @Override
    public AbstractFormatter getFormatter(JFormattedTextField tf) {
        return new DefaultFormatter() {
            @Override
            public Object stringToValue(String string) {
                Double value = 0.;

                if (string != null && string.length() != 0) {
                    int t = string.indexOf(terminator);
                    if (t > 0) {
                        try {
                            value = Double.valueOf(string.substring(0, t));
                        } catch (NumberFormatException e) {
                            Log.warn("Could not parse number");
                        }
                    } else {
                        try {
                            value = Double.valueOf(string);
                        } catch (NumberFormatException e) {
                            Log.warn("Could not parse number");
                        }
                    }
                }

                if (value < min)
                    value = min;
                else if (value > max)
                    value = max;

                return value;
            }

            @Override
            public String valueToString(Object value) {
                return String.format(format, value) + terminator;
            }

            @Override
            public Class<?> getValueClass() {
                return Double.class;
            }
        };
    }

}
