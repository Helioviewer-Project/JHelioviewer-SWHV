package org.helioviewer.jhv.database;

public class JHVDatabaseParam {

    public static final String DBSTRINGTYPE = "TEXT";
    public static final String DBINTTYPE = "INTEGER";
    public static final String DBDOUBLETYPE = "REAL";

    private final Object value;

    private final String dbType;
    private final String paramName;

    public JHVDatabaseParam(String _dbType, int _value, String _paramName) {
        dbType = _dbType;
        value = _value;
        paramName = _paramName;

    }

    public JHVDatabaseParam(String _dbType, String _value, String _paramName) {
        dbType = _dbType;
        value = _value;
        paramName = _paramName;
    }

    public JHVDatabaseParam(String _dbType, double _value, String _paramName) {
        dbType = _dbType;
        value = _value;
        paramName = _paramName;
    }

    public String getDbTyp() {
        return dbType;
    }

    public String getStringValue() {
        return (String) value;
    }

    public int getIntValue() {
        return (int) value;
    }

    public double getDoubleValue() {
        return (double) value;
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
