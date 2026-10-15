package org.helioviewer.jhv.event;

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

final class EventDatabaseConnection {

    private static final int CURRENT_VERSION_SCHEMA = 12;
    private Connection connection;
    private final int configHash;

    EventDatabaseConnection(int _configHash) {
        configHash = _configHash;
    }

    void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                Log.error(e);
            }
            connection = null;
        }
    }

    private void createSchema(Connection database) throws Exception {
        try (Statement statement = database.createStatement()) {
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

        try (PreparedStatement pstatement = database.prepareStatement("INSERT INTO version(version, hash) VALUES(?, ?)")) {
            pstatement.setQueryTimeout(30);
            pstatement.setInt(1, CURRENT_VERSION_SCHEMA);
            pstatement.setInt(2, configHash);
            pstatement.executeUpdate();
        }
    }

    @Nonnull
    Connection getConnection() throws Exception {
        if (connection == null)
            connection = openConnection();
        return connection;
    }

    private Connection openConnection() throws Exception {
        Path path = Path.of(Directories.CACHE.getPath(), "events.db");
        boolean exists = Files.exists(path);
        Connection database = DriverManager.getConnection("jdbc:sqlite:" + path);
        try {
            if (exists && !hasCurrentSchema(database)) {
                database.close();
                Files.delete(path);
                database = DriverManager.getConnection("jdbc:sqlite:" + path);
                exists = false;
            }
            if (!exists)
                createSchema(database);
            database.setAutoCommit(false);
            return database;
        } catch (Exception | Error e) {
            try {
                database.close();
            } catch (Exception closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    private boolean hasCurrentSchema(Connection database) {
        try (PreparedStatement statement = database.prepareStatement("SELECT version, hash from version LIMIT 1")) {
            statement.setQueryTimeout(30);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) == CURRENT_VERSION_SCHEMA
                        && result.getInt(2) == configHash;
            }
        } catch (Exception e) {
            Log.warn("Could not read version table, database might be corrupted or outdated: " + e.getMessage());
            return false;
        }
    }

}
