package org.helioviewer.jhv.database;

public class JHVDatabaseParam {

    public static final String DBSTRINGTYPE = "TEXT";
    public static final String DBINTTYPE = "INTEGER";
    public static final String DBDOUBLETYPE = "REAL";

    private final Object value;
    private final String paramName;

    public JHVDatabaseParam(int _value, String _paramName) {
        value = _value;
        paramName = _paramName;
    }

    public JHVDatabaseParam(String _value, String _paramName) {
        value = _value;
        paramName = _paramName;
    }

    public JHVDatabaseParam(double _value, String _paramName) {
        value = _value;
        paramName = _paramName;
    }

    public String getStringValue() {
        return (String) value;
    }

    public int getIntValue() {
        return (Integer) value;
    }

    public double getDoubleValue() {
        return (Double) value;
    }

    public String getParamName() {
        return paramName;
    }

    public boolean isInt() {
        return value instanceof Integer;
    }

    public boolean isString() {
        return value instanceof String;
    }

    public boolean isDouble() {
        return value instanceof Double;
    }

}
