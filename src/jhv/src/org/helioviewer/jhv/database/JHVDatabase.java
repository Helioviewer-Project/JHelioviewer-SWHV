package org.helioviewer.jhv.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.helioviewer.jhv.base.logging.Log;

public class JHVDatabase {

    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                Log.error("The result set cannot be closed " + e.getMessage());
            }
        }
    }

    public static void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                Log.error("The statement cannot be closed " + e.getMessage());
            }
        }
    }

}
