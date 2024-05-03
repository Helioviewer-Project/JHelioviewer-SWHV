package org.helioviewer.jhv.base;

import java.util.regex.Pattern;

public class Regex {

    public static final Pattern HREF = Pattern.compile("href=\"(.*?)\"");

    public static final Pattern FloatingPoint = Pattern.compile("[\\x00-\\x20]*[+-]?(NaN|Infinity|((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*");
    public static final Pattern Integer = Pattern.compile("\\d+");

    public static final Pattern Comma = Pattern.compile(",");
    public static final Pattern Equal = Pattern.compile("=");
    public static final Pattern GT = Pattern.compile(">");
    public static final Pattern Return = Pattern.compile("\n");
    public static final Pattern Space = Pattern.compile(" ");
    public static final Pattern MultiSpace = Pattern.compile("\\s+");
    public static final Pattern HttpField = Pattern.compile(": ");
    public static final Pattern MultiCommaSpace = Pattern.compile(",+|\\s+");

}
