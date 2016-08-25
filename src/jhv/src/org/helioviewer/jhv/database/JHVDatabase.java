package org.helioviewer.jhv.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.helioviewer.jhv.base.logging.Log;

public class JHVDatabase {

    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                Log.error("The result set cannot be closed " + e.getMessage());
            }
        }
    }

    public static void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                Log.error("The statement cannot be closed " + e.getMessage());
            }
        }
    }

}
