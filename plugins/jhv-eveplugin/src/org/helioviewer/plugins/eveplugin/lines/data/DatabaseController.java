package org.helioviewer.plugins.eveplugin.lines.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;

/**
 * 
 * @author Stephan Pagel
 * */
public class DatabaseController {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final String TABLE_NAME = "EVE_L2";
    private static final String COLUMN_DATE = "EVEDATE";

    /** the sole instance of this class */
    private static final DatabaseController singletonInstance = new DatabaseController();

    private final Properties properties = new Properties();
    private Connection connection = null;

    private final Object addQueueLock = new Object();
    private final LinkedList<DatabaseController.QueueItem> addQueue = new LinkedList<DatabaseController.QueueItem>();
    private boolean startNewThread = true;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    private DatabaseController() {
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        } catch (ClassNotFoundException e) {
            Log.error("", e);
        }

        properties.put("user", EVESettings.DATABASE_USERNAME);
        properties.put("password", EVESettings.DATABASE_PASSWORD);

        openConnection();

        if (connection != null) {
            try {
                if (!doesTableExists()) {
                    createTable();
                }
            } catch (SQLException e) {
                Log.error("", e);
            }
        }
    }

    public static DatabaseController getSingletonInstance() {
        return singletonInstance;
    }

    public void close() {
        closeConnection();
    }

    public void addToDatabase(final Band band, final EVEValue[] values) {
        return;

    }

    public EVEValue[] getDataInInterval(final Band band, final Interval<Date> interval) {
        if (connection == null) {
            return null;
        }

        final String columnName = prepareColumnName(band.getTitle());

        try {
            if (!doesColumnExists(columnName)) {
                return new EVEValue[0];
            }
        } catch (SQLException e) {
            Log.error("", e);
            return null;
        }

        final LinkedList<EVEValue> values = new LinkedList<EVEValue>();

        try {
            final Statement statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SELECT " + COLUMN_DATE + ", " + columnName + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + " BETWEEN " + interval.getStart().getTime() + " AND " + interval.getEnd().getTime() + " ORDER BY " + COLUMN_DATE);

            while (resultSet.next()) {
                values.add(new EVEValue(new Date(resultSet.getLong(1)), resultSet.getDouble(2)));
            }
        } catch (SQLException e) {
            Log.error("", e);
            return null;
        }

        return values.toArray(new EVEValue[0]);
    }

    private void openConnection() {
        try {
            connection = DriverManager.getConnection("jdbc:derby:" + EVESettings.EVE_DATA + ";create=true", properties);
        } catch (SQLException e) {
            Log.error("", e);

            connection = null;
        }
    }

    private void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            Log.error("", e);
        }
    }

    private void createTable() throws SQLException {
        final Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE " + TABLE_NAME + " (" + COLUMN_DATE + " BIGINT NOT NULL, PRIMARY KEY (" + COLUMN_DATE + "))");
        statement.close();
    }

    private boolean doesTableExists() throws SQLException {
        final ResultSet resultSet = connection.getMetaData().getTables("%", "%", "%", new String[] { "TABLE" });
        boolean databaseExists = false;

        while (resultSet.next() && !databaseExists) {
            if (resultSet.getString("TABLE_NAME").equalsIgnoreCase(TABLE_NAME)) {
                databaseExists = true;
            }
        }

        return databaseExists;
    }

    private boolean doesColumnExists(final String name) throws SQLException {
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery("SELECT c.COLUMNNAME FROM SYS.SYSTABLES t RIGHT OUTER JOIN SYS.SYSCOLUMNS c ON t.TABLEID = c.REFERENCEID WHERE t.TABLENAME = '" + TABLE_NAME + "'");

        boolean columnExists = false;

        while (resultSet.next() && !columnExists) {
            if (resultSet.getString("COLUMNNAME").equalsIgnoreCase(name)) {
                columnExists = true;
            }
        }

        resultSet.close();
        statement.close();

        return columnExists;
    }

    private void addColumn(final String name) throws SQLException {

        final Statement statement = connection.createStatement();
        statement.execute("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + name + " DOUBLE DEFAULT NULL");
        statement.close();
    }

    private String prepareColumnName(final String name) {
        return name.replaceAll("-", "_").toUpperCase();
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Queue Item
    // //////////////////////////////////////////////////////////////////////////////

    private class QueueItem {
        public final String columnName;
        public final EVEValue[] values;

        public QueueItem(final String columnName, final EVEValue[] values) {
            this.columnName = columnName;
            this.values = values;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Database Thread
    // //////////////////////////////////////////////////////////////////////////////

    private class AddToDatabaseThread implements Runnable {

        public void run() {
            while (true) {
                QueueItem queueItem = null;

                synchronized (addQueueLock) {
                    if (addQueue.size() > 0) {
                        queueItem = addQueue.pop();
                    } else {
                        startNewThread = true;
                        break;
                    }
                }

                try {
                    final PreparedStatement selectStatement = connection.prepareStatement("SELECT " + COLUMN_DATE + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + " = ?");
                    final PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO " + TABLE_NAME + " (" + COLUMN_DATE + ", " + queueItem.columnName + ") VALUES (?,?)");
                    final PreparedStatement updateStatement = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET " + queueItem.columnName + " = ? WHERE " + COLUMN_DATE + " = ?");

                    for (final EVEValue value : queueItem.values) {
                        // check if a row for given date exists already
                        selectStatement.setLong(1, value.date.getTime());
                        selectStatement.execute();

                        final ResultSet rs = selectStatement.getResultSet();
                        final boolean updateColumn = rs.next();
                        rs.close();

                        // if a row exists update the value otherwise insert a
                        // new row
                        if (updateColumn) {
                            updateStatement.setDouble(1, value.value);
                            updateStatement.setLong(2, value.date.getTime());
                            updateStatement.execute();
                        } else {
                            insertStatement.setLong(1, value.date.getTime());
                            insertStatement.setDouble(2, value.value);
                            insertStatement.execute();
                        }
                    }

                    selectStatement.close();
                    insertStatement.close();
                    updateStatement.close();
                } catch (SQLException e) {
                    Log.error("", e);
                }
            }
        }
    }
}
