package org.helioviewer.jhv.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.Directories;

final class EventDatabaseThread {

    private static final int CURRENT_VERSION_SCHEMA = 12;
    private static Connection connection;

    private EventDatabaseThread() {
    }

    private static void createSchema() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.executeUpdate("CREATE TABLE if not exists event_type (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, supplier TEXT, UNIQUE(name, supplier) ON CONFLICT IGNORE)");
            statement.executeUpdate("CREATE TABLE if not exists events (id INTEGER PRIMARY KEY AUTOINCREMENT, type_id INTEGER, uid TEXT, start INTEGER, end INTEGER, archiv INTEGER, data BLOB, FOREIGN KEY(type_id) REFERENCES event_type(id), UNIQUE(uid) ON CONFLICT FAIL)");
            statement.executeUpdate("CREATE TABLE event_parameter (event_id INTEGER, name TEXT COLLATE NOCASE, type_id INTEGER NOT NULL, " +
                    "value ANY NOT NULL CHECK(typeof(value) IN ('integer','real')), PRIMARY KEY(event_id,name), " +
                    "FOREIGN KEY(event_id) REFERENCES events(id), FOREIGN KEY(type_id) REFERENCES event_type(id)) STRICT, WITHOUT ROWID");
            statement.executeUpdate("CREATE INDEX parameter_value ON event_parameter(type_id,name,value,event_id)");
            statement.executeUpdate("CREATE INDEX if not exists evt_type_start ON events (type_id, start)");
            statement.executeUpdate("CREATE INDEX if not exists evt_type_end ON events (type_id, end)");
            statement.executeUpdate("CREATE TABLE if not exists event_link (left_id INTEGER, right_id INTEGER, PRIMARY KEY(left_id, right_id) ON CONFLICT IGNORE, CHECK(left_id < right_id), FOREIGN KEY(left_id) REFERENCES events(id), FOREIGN KEY(right_id) REFERENCES events(id)) WITHOUT ROWID");
            statement.executeUpdate("CREATE INDEX if not exists evt_right ON event_link (right_id)");
            statement.executeUpdate("CREATE TABLE if not exists date_range (type_id INTEGER, start INTEGER, end INTEGER, PRIMARY KEY(type_id, start, end) ON CONFLICT IGNORE, FOREIGN KEY(type_id) REFERENCES event_type(id)) WITHOUT ROWID");
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

        Path path = Path.of(Directories.CACHE.getPath(), "events.db");
        boolean fexist = Files.exists(path);
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
            } catch (Exception e) {
                Log.warn("Could not read version table, database might be corrupted or outdated: " + e.getMessage());
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
