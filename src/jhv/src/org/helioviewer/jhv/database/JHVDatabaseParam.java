package org.helioviewer.jhv.database;

public class JHVDatabaseParam {
    public static String DBSTRINGTYPE = "TEXT";
    public static String DBINTTYPE = "INTEGER";

    private int intp;
    private String stringp;
    private final String dbType;
    private final String paramName;
    private final boolean isInt;
    private final boolean isString;

    public JHVDatabaseParam(String _dbType, int value, String _paramName) {
        dbType = _dbType;
        intp = value;
        paramName = _paramName;
        isInt = true;
        isString = false;
    }

    public JHVDatabaseParam(String _dbType, String value, String _paramName) {
        dbType = _dbType;
        stringp = value;
        paramName = _paramName;
        isInt = false;
        isString = true;
    }

    public String getDbTyp() {
        return dbType;
    }

    public String getStringValue() {
        return stringp;
    }

    public Integer getIntValue() {
        return intp;
    }

    public String getParamName() {
        return paramName;
    }

    public boolean isInt() {
        return isInt;
    }

    public boolean isString() {
        return isString;
    }
}
