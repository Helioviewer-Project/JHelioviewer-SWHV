package org.helioviewer.jhv.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.log.Log;

public class DataSourcesDB {

    private static final DataSourcesDB instance = new DataSourcesDB();

    public static void init() {
    }

    private static Connection connection;
    private static PreparedStatement insert;
    private static PreparedStatement select;

    private DataSourcesDB() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite::memory:");

            try (Statement create = connection.createStatement()) {
                create.setQueryTimeout(30);
                create.executeUpdate("drop table if exists DataSources"); // debug
                create.executeUpdate("CREATE TABLE DataSources (sourceId INTEGER, server STRING, observatory STRING, dataset STRING, start INTEGER, end INTEGER, UNIQUE(sourceId, server) ON CONFLICT REPLACE)");
            }

            insert = connection.prepareStatement("INSERT INTO DataSources(sourceId, server, observatory, dataset, start, end) VALUES(?,?,?,?,?,?)");
            insert.setQueryTimeout(30);
            select = connection.prepareStatement("SELECT sourceId,server FROM DataSources WHERE server=? AND observatory LIKE ? AND dataset LIKE ?");
            select.setQueryTimeout(30);
        } catch (SQLException e) {
            Log.error("Could not create database connection", e);
            try {
                connection.close();
            } catch (Exception ignore) {
            }
            connection = null;
        }
    }

    public static void doInsert(int sourceId, String server, String observatory, String dataset, long start, long end) {
        try {
            insert.setInt(1, sourceId);
            insert.setString(2, server);
            insert.setString(3, observatory);
            insert.setString(4, dataset);
            insert.setLong(5, start);
            insert.setLong(6, end);
            insert.executeUpdate();
        } catch (Exception e) {
            Log.error("Failed to insert", e);
        }
    }

    public static ArrayList<Pair<Integer, String>> doSelect(@Nonnull String server, @Nonnull String observatory, @Nonnull String dataset) {
        ArrayList<Pair<Integer, String>> res = new ArrayList<>();
        try {
            select.setString(1, server);
            select.setString(2, "%" + observatory + "%");
            select.setString(3, "%" + dataset + "%");

            try (ResultSet rs = select.executeQuery()) {
                while (rs.next())
                    res.add(new Pair<>(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception e) {
            Log.error("Failed to select", e);
        }
        return res;
    }

}
