package org.helioviewer.jhv.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public record DatabaseField(String name, Object value) {

    public static final String TEXT = "TEXT";
    public static final String INTEGER = "INTEGER";
    public static final String REAL = "REAL";

    public void bind(PreparedStatement statement, int index) throws SQLException {
        switch (value) {
            case Integer i -> statement.setInt(index, i);
            case String s -> statement.setString(index, s);
            case Double d -> statement.setDouble(index, d);
            default -> throw new IllegalArgumentException("Unsupported database field value: " + value);
        }
    }
}
