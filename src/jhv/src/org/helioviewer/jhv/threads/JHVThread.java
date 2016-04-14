package org.helioviewer.jhv.threads;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ThreadFactory;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.database.JHVDatabase;

public class JHVThread {

    // this creates daemon threads
    public static class NamedThreadFactory implements ThreadFactory {

        protected final String name;

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            return thread;
        }
    }

    public static class ConnectionThread extends Thread {
        private static Connection connection;
        private static final int CURRENT_VERSION_SCHEMA = 5;
        public static final HashMap<JHVEventType, RequestCache> downloadedCache = new HashMap<JHVEventType, RequestCache>();

        public ConnectionThread(Runnable r, String name) {
            super(r, name);
        }

        private static void createSchema() {
            try {

                Statement statement = connection.createStatement();
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
                statement.close();
                String sqlt = "INSERT INTO version(version, hash) VALUES(?, ?)";

                PreparedStatement pstatement = connection.prepareStatement(sqlt);
                pstatement.setQueryTimeout(30);
                pstatement.setInt(1, CURRENT_VERSION_SCHEMA);
                pstatement.setInt(2, JHVDatabase.config_hash);

                pstatement.executeUpdate();
                pstatement.close();
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
                        PreparedStatement pstatement = connection.prepareStatement(sqlt);
                        pstatement.setQueryTimeout(30);
                        ResultSet rs = pstatement.executeQuery();
                        if (!rs.isClosed() && rs.next()) {
                            found_version = rs.getInt(1);
                            found_hash = rs.getInt(2);
                        }
                        rs.close();
                        pstatement.close();
                        if (found_version != CURRENT_VERSION_SCHEMA || JHVDatabase.config_hash != found_hash) {
                            connection.close();
                            new File(filepath).delete();
                            connection = DriverManager.getConnection("jdbc:sqlite:" + filepath);
                            createSchema();
                        }
                    }
                } catch (SQLException e) {
                    Log.error("Could not create database connection" + e);
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

        public NamedDbThreadFactory(String name) {
            super(name);
        }

        @Override
        public ConnectionThread newThread(Runnable r) {
            JHVThread.ConnectionThread thread = new JHVThread.ConnectionThread(r, name);
            thread.setDaemon(true);
            return thread;
        }
    }

}
