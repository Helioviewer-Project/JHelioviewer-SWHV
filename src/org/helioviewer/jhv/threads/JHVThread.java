package org.helioviewer.jhv.threads;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.database.EventDatabase;
import org.jetbrains.annotations.NotNull;

public class JHVThread {

    public static void afterExecute(Runnable r, Throwable t) {
        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) r;
                if (future.isDone()) {
                    future.get();
                }
            } catch (CancellationException e) {
                t = e;
            } catch (ExecutionException e) {
                t = e.getCause();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // ??? ignore/reset
            }
        }
        if (t != null) {
            t.printStackTrace();
        }
    }

    // this creates daemon threads
    public static class NamedThreadFactory implements ThreadFactory {

        final String name;

        public NamedThreadFactory(String _name) {
            name = _name;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            return thread;
        }
    }

    public static class ConnectionThread extends Thread {

        private static final int CURRENT_VERSION_SCHEMA = 7;
        private static Connection connection;

        public ConnectionThread(Runnable r, String name) {
            super(r, name);
        }

        private static void createSchema() {
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.setQueryTimeout(30);
                    statement.executeUpdate("CREATE TABLE if not exists event_type (id INTEGER PRIMARY KEY AUTOINCREMENT, name STRING , supplier STRING, UNIQUE(name, supplier) ON CONFLICT IGNORE)");
                    statement.executeUpdate("CREATE TABLE if not exists events (id INTEGER PRIMARY KEY AUTOINCREMENT, type_id INTEGER, uid STRING , start BIGINTEGER, end BIGINTEGER, archiv BIGINTEGER , data BLOB, FOREIGN KEY(type_id) REFERENCES event_type(id), UNIQUE(uid) ON CONFLICT FAIL)");
                    statement.executeUpdate("CREATE INDEX if not exists evt_uid ON events (uid);");
                    statement.executeUpdate("CREATE INDEX if not exists evt_end ON events (end);");
                    statement.executeUpdate("CREATE INDEX if not exists evt_start ON events (start);");
                    statement.executeUpdate("CREATE TABLE if not exists event_link (id INTEGER PRIMARY KEY AUTOINCREMENT, left_id INTEGER, right_id INTEGER, FOREIGN KEY(left_id) REFERENCES events(id), FOREIGN KEY(right_id) REFERENCES events(id), UNIQUE(left_id, right_id) ON CONFLICT IGNORE)");
                    statement.executeUpdate("CREATE INDEX if not exists evt_left ON event_link (left_id);");
                    statement.executeUpdate("CREATE INDEX if not exists evt_left ON event_link (right_id);");
                    statement.executeUpdate("CREATE TABLE if not exists date_range (id INTEGER PRIMARY KEY AUTOINCREMENT, type_id INTEGER , start BIGINTEGER , end BIGINTEGER, FOREIGN KEY(type_id) REFERENCES event_type(id))");
                    statement.executeUpdate("CREATE TABLE if not exists version (version INTEGER PRIMARY KEY, hash INTEGER );");
                }

                try (PreparedStatement pstatement = connection.prepareStatement("INSERT INTO version(version, hash) VALUES(?, ?)")) {
                    pstatement.setQueryTimeout(30);
                    pstatement.setInt(1, CURRENT_VERSION_SCHEMA);
                    pstatement.setInt(2, EventDatabase.config_hash);
                    pstatement.executeUpdate();
                }
            } catch (SQLException e) {
                Log.error("Could not create database connection" + e);
                connection = null;
            }
        }

        public static Connection getConnection() {
            if (connection == null) {
                try {
                    String filepath = JHVDirectory.EVENTS.getPath() + "events.db";
                    File f = new File(filepath);
                    boolean fexist = f.exists() && !f.isDirectory();
                    connection = DriverManager.getConnection("jdbc:sqlite:" + filepath);

                    if (!fexist) {
                        createSchema();
                    } else {
                        String sqlt = "SELECT version, hash from version LIMIT 1";
                        int found_version = -1;
                        int found_hash = -1;

                        try (PreparedStatement pstatement = connection.prepareStatement(sqlt)) {
                            pstatement.setQueryTimeout(30);

                            try (ResultSet rs = pstatement.executeQuery()) {
                                if (rs.next()) {
                                    found_version = rs.getInt(1);
                                    found_hash = rs.getInt(2);
                                }
                            }
                        }

                        if (found_version != CURRENT_VERSION_SCHEMA || EventDatabase.config_hash != found_hash) {
                            connection.close();
                            new File(filepath).delete();
                            connection = DriverManager.getConnection("jdbc:sqlite:" + filepath);
                            createSchema();
                        }
                    }
                } catch (SQLException e) {
                    Log.error("Could not create database connection" + e);
                    try {
                        connection.close();
                    } catch (SQLException ignore) {
                    }
                    connection = null;
                }
            }
            if (connection != null) {
                try {
                    connection.setAutoCommit(false);
                } catch (SQLException e) {
                    Log.error("Could not set autocommit off");
                }
            }
            return connection;
        }
    }

    public static class NamedDbThreadFactory extends NamedThreadFactory {

        public NamedDbThreadFactory(String _name) {
            super(_name);
        }

        @Override
        public ConnectionThread newThread(@NotNull Runnable r) {
            JHVThread.ConnectionThread thread = new JHVThread.ConnectionThread(r, name);
            thread.setDaemon(true);
            return thread;
        }
    }

}
