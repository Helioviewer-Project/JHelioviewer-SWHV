package org.helioviewer.jhv.gui.component;

import java.text.ParseException;

import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

@SuppressWarnings("serial")
public class TerminatedFormatterFactory extends DefaultFormatterFactory {

    public TerminatedFormatterFactory(String _format, String _terminator, double _min, double _max) {
        super(new TerminatedFormatter(_format, _terminator, _min, _max));
    }

    private static class TerminatedFormatter extends DefaultFormatter {

        private final String format;
        private final String terminator;
        private final double min;
        private final double max;

        TerminatedFormatter(String _format, String _terminator, double _min, double _max) {
            format = _format;
            terminator = _terminator;

            if (_min > _max)
                _max = _min;
            min = _min;
            max = _max;
            setOverwriteMode(false);
        }

        @Override
        public Object stringToValue(String string) throws ParseException {
            double value = 0;
            if (string != null) {
                int t = string.indexOf(terminator);
                if (t > 0)
                    string = string.substring(0, t);

                try {
                    value = Double.parseDouble(string);
                } catch (Exception e) {
                    throw new ParseException("Could not parse number: " + string, 0);
                }
            }
            return Math.clamp(value, min, max);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            try {
                return String.format(format, value) + terminator;
            } catch (Exception e) {
                throw new ParseException("Could not convert object: " + value, 0);
            }
        }

        @Override
        public Class<?> getValueClass() {
            return Double.class;
        }

    }

}
