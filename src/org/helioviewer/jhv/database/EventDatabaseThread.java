package org.helioviewer.jhv.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.JHVDirectory;

public class EventDatabaseThread extends Thread {

    private static final int CURRENT_VERSION_SCHEMA = 10;
    private static Connection connection;

    public EventDatabaseThread(Runnable r, String name) {
        super(r, name);
    }

    private static void createSchema() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.executeUpdate("CREATE TABLE if not exists event_type (id INTEGER PRIMARY KEY AUTOINCREMENT, name STRING, supplier STRING, UNIQUE(name, supplier) ON CONFLICT IGNORE)");
            statement.executeUpdate("CREATE TABLE if not exists events (id INTEGER PRIMARY KEY AUTOINCREMENT, type_id INTEGER, uid STRING, start BIGINTEGER, end BIGINTEGER, archiv BIGINTEGER, data BLOB, FOREIGN KEY(type_id) REFERENCES event_type(id), UNIQUE(uid) ON CONFLICT FAIL)");
            statement.executeUpdate("CREATE INDEX if not exists evt_uid ON events (uid)");
            statement.executeUpdate("CREATE INDEX if not exists evt_end ON events (end)");
            statement.executeUpdate("CREATE INDEX if not exists evt_start ON events (start)");
            statement.executeUpdate("CREATE TABLE if not exists event_link (id INTEGER PRIMARY KEY AUTOINCREMENT, left_id INTEGER, right_id INTEGER, FOREIGN KEY(left_id) REFERENCES events(id), FOREIGN KEY(right_id) REFERENCES events(id), UNIQUE(left_id, right_id) ON CONFLICT IGNORE)");
            statement.executeUpdate("CREATE INDEX if not exists evt_left ON event_link (left_id)");
            statement.executeUpdate("CREATE INDEX if not exists evt_left ON event_link (right_id)");
            statement.executeUpdate("CREATE TABLE if not exists date_range (id INTEGER PRIMARY KEY AUTOINCREMENT, type_id INTEGER, start BIGINTEGER, end BIGINTEGER, FOREIGN KEY(type_id) REFERENCES event_type(id))");
            statement.executeUpdate("CREATE TABLE if not exists version (version INTEGER PRIMARY KEY, hash INTEGER)");
        }

        try (PreparedStatement pstatement = connection.prepareStatement("INSERT INTO version(version, hash) VALUES(?, ?)")) {
            pstatement.setQueryTimeout(30);
            pstatement.setInt(1, CURRENT_VERSION_SCHEMA);
            pstatement.setInt(2, EventDatabase.config_hash);
            pstatement.executeUpdate();
        }
    }

    @Nonnull
    static Connection getConnection() throws Exception {
        if (connection != null)
            return connection;

        Path path = Path.of(JHVDirectory.EVENTS.getPath(), "events.db");
        boolean fexist = Files.isWritable(path);
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);

        if (fexist) {
            int found_version = -1;
            int found_hash = -1;
            try (PreparedStatement pstatement = connection.prepareStatement("SELECT version, hash from version LIMIT 1")) {
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
                Files.delete(path);
                connection = DriverManager.getConnection("jdbc:sqlite:" + path);
                createSchema();
            }
        } else {
            createSchema();
        }

        connection.setAutoCommit(false);
        return connection;
    }

}
