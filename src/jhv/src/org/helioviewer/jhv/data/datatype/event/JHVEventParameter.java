package org.helioviewer.jhv.data.datatype.event;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.regex.Pattern;

public class JHVEventParameter {
    /** The name of the parameter */
    private String parameterName;

    /** How the parameter is displayed */
    private String parameterDisplayName;

    /** The value of the parameter */
    private String parameterValue;

    /** The parameter display value */
    private String displayParameterValue;

    /**
     * Default constructor.
     *
     */
    public JHVEventParameter() {
        parameterName = "";
        parameterDisplayName = "";
        parameterValue = "";
        displayParameterValue = "";
    }

    /**
     * Creates a JHVEvent parameter with a parameter name, parameter display
     * name and parameter value.
     *
     * @param parameterName
     *            the parameter name
     * @param parameterDisplayName
     *            the parameter display name
     * @param parameterValue
     *            the parameter value
     */
    public JHVEventParameter(String parameterName, String parameterDisplayName, String parameterValue) {
        this.parameterName = parameterName;
        this.parameterDisplayName = parameterDisplayName;
        this.parameterValue = parameterValue;
        displayParameterValue = beautifyParameterValue(parameterValue);
    }

    /**
     * Gets the parameter name.
     *
     * @return the name of the parameter
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Sets the parameter name.
     *
     * @param parameterName
     *            the name of the parameter
     */
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    /**
     * Gets the parameter display name.
     *
     * @return the parameter display name
     */
    public String getParameterDisplayName() {
        return parameterDisplayName;
    }

    /**
     * Sets the parameter display name.
     *
     * @param parameterDisplayName
     *            the display name of the parameter
     */
    public void setParameterDisplayName(String parameterDisplayName) {
        this.parameterDisplayName = parameterDisplayName;
    }

    /**
     * Gets the value of the parameter.
     *
     * @return the value of the parameter
     */
    public String getParameterValue() {
        return parameterValue;
    }

    /**
     * Sets the value of the parameter.
     *
     * @param parameterValue
     *            the value of the parameter
     */
    public void setParameterValue(String parameterValue) {
        this.parameterValue = parameterValue;
        displayParameterValue = beautifyParameterValue(parameterValue);
    }

    /**
     * Gets the parameter display value
     */
    public String getDisplayParameterValue() {
        return displayParameterValue;
    }

    private String beautifyParameterValue(String parameterValue) {
        if (isDouble(parameterValue) && !isInteger(parameterValue)) {
            NumberFormat formatter = new DecimalFormat("0.###E0");
            String result = formatter.format(Double.parseDouble(parameterValue));
            if (result.contains("E0")) {
                return result.substring(0, result.length() - 2);
            } else {
                return result;
            }
        } else {
            return parameterValue;
        }
    }

    private boolean isDouble(String value) {
        final String Digits = "(\\p{Digit}+)";
        final String HexDigits = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp = "[eE][+-]?" + Digits;
        final String fpRegex = ("[\\x00-\\x20]*" + // Optional leading
                // "whitespace"
                "[+-]?(" + // Optional sign character
                "NaN|" + // "NaN" string
                "Infinity|" + // "Infinity" string

                // A decimal floating-point string representing a finite
                // positive
                // number without a leading sign has at most five basic pieces:
                // Digits . Digits ExponentPart FloatTypeSuffix
                //
                // Since this method allows integer-only strings as input
                // in addition to strings of floating-point literals, the
                // two sub-patterns below are simplifications of the grammar
                // productions from the Java Language Specification, 2nd
                // edition, section 3.10.2.

                // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +

        // . Digits ExponentPart_opt FloatTypeSuffix_opt
        "(\\.(" + Digits + ")(" + Exp + ")?)|" +

                // Hexadecimal strings
                "((" +
                // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                "(0[xX]" + HexDigits + "(\\.)?)|" +

                // 0[xX] HexDigits_opt . HexDigits BinaryExponent
                // FloatTypeSuffix_opt
                "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

                ")[pP][+-]?" + Digits + "))" + "[fFdD]?))" + "[\\x00-\\x20]*");// Optional
        // trailing
        // "whitespace"

        return value != null && Pattern.matches(fpRegex, value);
    }

    private boolean isInteger(String value) {
        return value.matches("\\d+");
    }
}
