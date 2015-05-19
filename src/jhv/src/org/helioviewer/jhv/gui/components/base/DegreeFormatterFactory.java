package org.helioviewer.jhv.gui.components.base;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.text.DefaultFormatter;

@SuppressWarnings({"serial"})
public class DegreeFormatterFactory extends AbstractFormatterFactory {

    private final String format;

    public DegreeFormatterFactory(String format) {
        super();
        this.format = format;
    }

    @Override
    public AbstractFormatter getFormatter(JFormattedTextField tf) {
        return new DefaultFormatter() {
            @Override
            public Object stringToValue(String string) {
                if (string == null || string.length() == 0) {
                    return new Double(0.);
                }
                return Double.parseDouble(string.substring(0, string.length() - 1));
            }

            @Override
            public String valueToString(Object value) {
                return String.format(format, value);
            }

            @Override
            public Class<?> getValueClass() {
                return Double.class;
            }
        };
    }

}
