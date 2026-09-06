package org.helioviewer.jhv.database;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.SingleExecutor;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;

public class EventDatabase {

    private static final SingleExecutor executor = new SingleExecutor(new AppThread.NamedThreadFactory("EventDatabase"));

    private static final long ONEWEEK = 1000 * 60 * 60 * 24 * 7;
    public static int config_hash;

    private static final String INSERT_EVENT = "INSERT INTO events(uid) VALUES(?)";
    private static final String UPSERT_EVENT =
            "INSERT INTO events(type_id, uid, start, end, archiv, data) VALUES(?,?,?,?,?,?) " +
                    "ON CONFLICT(uid) DO UPDATE SET type_id=excluded.type_id, start=excluded.start, end=excluded.end, archiv=excluded.archiv, data=excluded.data RETURNING id";
    private static final String SELECT_EVENT_TYPE = "SELECT id FROM event_type WHERE name=? AND supplier=?";
    private static final String INSERT_EVENT_TYPE = "INSERT INTO event_type(name, supplier) VALUES(?,?)";
    private static final String INSERT_LINK = "INSERT INTO event_link(left_id, right_id) VALUES(?,?)";
    private static final String SELECT_EVENT_ID_FROM_UID = "SELECT id FROM events WHERE uid=?";
    private static final String DELETE_DATERANGES = "DELETE FROM date_range WHERE type_id=?";
    private static final String INSERT_DATERANGE = "INSERT INTO date_range(type_id,  start, end) VALUES(?,?,?)";
    private static final String SELECT_DATERANGE = "SELECT start, end FROM date_range where type_id=? order by start, end ";
    private static final String SELECT_LAST_EVENT = "SELECT end FROM events WHERE type_id=? order by end DESC LIMIT 1";
    private static final String SELECT_ASSOCIATIONS =
            "SELECT event_link.left_id, event_link.right_id FROM events JOIN event_link ON events.id=event_link.left_id " +
                    "WHERE events.type_id=? AND events.start<=? AND events.end>=? UNION " +
                    "SELECT event_link.left_id, event_link.right_id FROM events JOIN event_link ON events.id=event_link.right_id " +
                    "WHERE events.type_id=? AND events.start<=? AND events.end>=?";
    private static final String SELECT_EVENT =
            "SELECT e.id, e.start, e.end, e.data, event_type.supplier FROM events AS e " +
                    "LEFT JOIN event_type ON e.type_id=event_type.id WHERE e.id=?";
    private static final HashMap<String, PreparedStatement> statements = new HashMap<>();
    private static final HashMap<SWEKSupplier, RequestCache> storedIntervals = new HashMap<>();

    private static PreparedStatement getPreparedStatement(String statement) throws Exception {
        PreparedStatement pstat = statements.get(statement);
        if (pstat == null) {
            pstat = EventDatabaseThread.getConnection().prepareStatement(statement);
            pstat.setQueryTimeout(30);
            statements.put(statement, pstat);
        }
        return pstat;
    }

    private static int findOrInsertEventTypeId(SWEKSupplier supplier) throws Exception {
        int typeId = findEventTypeId(supplier);
        if (typeId == -1) {
            insertEventTypeIfNotExist(supplier);
            typeId = findEventTypeId(supplier);
        }
        if (typeId == -1)
            throw new SQLException("Could not create event type " + SWEKCatalog.key(supplier));
        return typeId;
    }

    private static int findEventTypeId(SWEKSupplier supplier) throws Exception {
        int typeId = -1;
        PreparedStatement pstatement = getPreparedStatement(SELECT_EVENT_TYPE);
        pstatement.setString(1, supplier.group().getName());
        pstatement.setString(2, SWEKCatalog.key(supplier));

        try (ResultSet rs = pstatement.executeQuery()) {
            if (rs.next()) {
                typeId = rs.getInt(1);
            }
        }
        return typeId;
    }

    private static void insertEventTypeIfNotExist(SWEKSupplier eventType) throws Exception {
        PreparedStatement pstatement = getPreparedStatement(INSERT_EVENT_TYPE);
        pstatement.setString(1, eventType.group().getName());
        pstatement.setString(2, SWEKCatalog.key(eventType));
        pstatement.executeUpdate();

        StringBuilder createtbl = new StringBuilder("CREATE TABLE ").append(eventType.dbName())
                .append(" (event_id INTEGER PRIMARY KEY ON CONFLICT REPLACE");
        SWEKCatalog.databaseFields(eventType).forEach((key, value) ->
                createtbl.append(',').append(key).append(' ').append(switch (value) { case INTEGER -> "INTEGER"; case DECIMAL -> "REAL"; }));
        createtbl.append(", FOREIGN KEY(event_id) REFERENCES events(id))");

        Connection connection = pstatement.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.executeUpdate(createtbl.toString());
            createEventTableIndexes(statement, eventType);
        }
        connection.commit();
    }

    private static void createEventTableIndexes(Statement statement, SWEKSupplier eventType) throws Exception {
        String tableName = eventType.dbName();
        for (String field : SWEKCatalog.databaseFields(eventType).keySet())
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS " + tableName + '_' + field + " ON " + tableName + " (" + field + ")");
    }

    private static int findOrInsertEventId(String uid) throws Exception {
        int id = findEventId(uid);
        if (id == -1) {
            insertVoidEvent(uid);
            id = findEventId(uid);
        }
        if (id == -1)
            throw new SQLException("Could not create event " + uid);
        return id;
    }

    private static int findEventId(String uid) throws Exception {
        int id = -1;
        PreparedStatement pstatement = getPreparedStatement(SELECT_EVENT_ID_FROM_UID);
        pstatement.setString(1, uid);

        try (ResultSet rs = pstatement.executeQuery()) {
            if (rs.next()) {
                id = rs.getInt(1);
            }
        }
        return id;
    }

    private static void insertVoidEvent(String uid) throws Exception {
        PreparedStatement pstatement = getPreparedStatement(INSERT_EVENT);
        pstatement.setString(1, uid);
        pstatement.executeUpdate();
    }

    private static void insertAssociation(PreparedStatement statement, int id0, int id1) throws SQLException {
        if (id0 == id1)
            return;

        statement.setInt(1, Math.min(id0, id1));
        statement.setInt(2, Math.max(id0, id1));
        statement.executeUpdate();
    }

    public static boolean storeRemotePage(SWEKHandler.RemotePage remotePage, SWEKSupplier supplier) {
        try {
            executor.invokeAndWait(new StoreRemotePage(remotePage, supplier));
            return true;
        } catch (Exception e) {
            Log.error("Could not store event page", e);
            return false;
        }
    }

    private record StoreRemotePage(SWEKHandler.RemotePage remotePage,
                                   SWEKSupplier supplier) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            Connection connection = EventDatabaseThread.getConnection();
            try {
                storeEvents(remotePage.events(), supplier);
                storeAssociations(remotePage.associations());
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
            return null;
        }
    }

    private static void storeAssociations(List<JHVEvent.LinkRef> links) throws Exception {
        PreparedStatement pstatement = getPreparedStatement(INSERT_LINK);

        for (JHVEvent.LinkRef link : links) {
            int id0 = findOrInsertEventId(link.firstUid());
            int id1 = findOrInsertEventId(link.secondUid());
            insertAssociation(pstatement, id0, id1);
        }
    }

    private static void bindIndexedValue(PreparedStatement statement, int index, Number value) throws SQLException {
        switch (value) {
            case Integer i -> statement.setInt(index, i);
            case Double d -> statement.setDouble(index, d);
            default -> throw new IllegalArgumentException("Unsupported indexed event value: " + value);
        }
    }

    private static void storeEvents(List<SWEKHandler.RemoteEvent> remoteEvents, SWEKSupplier supplier) throws Exception {
        int[] eventIds = new int[remoteEvents.size()];
        int typeId = findOrInsertEventTypeId(supplier);

        PreparedStatement statement = getPreparedStatement(UPSERT_EVENT);

        for (int i = 0; i < remoteEvents.size(); i++) {
            SWEKHandler.RemoteEvent event2db = remoteEvents.get(i);
            statement.setInt(1, typeId);
            statement.setString(2, event2db.uid());
            statement.setLong(3, event2db.start());
            statement.setLong(4, event2db.end());
            statement.setLong(5, event2db.archiv());
            statement.setBytes(6, event2db.compressedJson());
            int eventId;
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next())
                    throw new SQLException("Could not store event " + event2db.uid());
                eventId = rs.getInt(1);
            }

            StringBuilder fieldString = new StringBuilder();
            StringBuilder varString = new StringBuilder();
            List<Number> values = new ArrayList<>();
            for (String field : SWEKCatalog.databaseFields(supplier).keySet()) {
                Number value = event2db.indexedValues().get(field);
                if (value == null) continue;
                values.add(value);
                fieldString.append(',').append(field);
                varString.append(",?");
            }
            String full_statement = "INSERT INTO " + supplier.dbName() + "(event_id" + fieldString + ") VALUES(?" + varString + ')';
            PreparedStatement pstatement = getPreparedStatement(full_statement);
            pstatement.setInt(1, eventId);

            int index = 2;
            for (Number value : values) {
                bindIndexedValue(pstatement, index, value);
                index++;
            }
            pstatement.executeUpdate();
            eventIds[i] = eventId;
        }
        storeRelatedEventLinks(eventIds, supplier);
    }

    private static void storeRelatedEventLinks(int[] eventIds, SWEKSupplier type) throws Exception {
        String table = type.dbName();
        SWEKGroup group = type.group();
        for (SWEK.RelatedEvents relation : SWEKCatalog.getRelatedEvents()) {
            if (relation.group() != group || relation.relatedWith() != group)
                continue;

            for (SWEK.RelatedOn relatedOn : relation.relatedOnList()) {
                String sql = "INSERT INTO event_link(left_id, right_id) " +
                        "SELECT DISTINCT min(a.event_id, b.event_id), max(a.event_id, b.event_id) " +
                        "FROM " + table + " AS a JOIN " + table + " AS b ON a." + relatedOn.parameterFrom() + "=b." + relatedOn.parameterWith() + ' ' +
                        "WHERE a.event_id!=b.event_id AND (a.event_id=? OR b.event_id=?)";
                PreparedStatement statement = getPreparedStatement(sql);
                for (int eventId : eventIds) {
                    statement.setInt(1, eventId);
                    statement.setInt(2, eventId);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static JHVEvent parseJSON(JsonEvent jsonEvent, boolean full) throws Exception {
        try (InputStream bais = new ByteArrayInputStream(jsonEvent.json); InputStream is = new GZIPInputStream(bais)) {
            return jsonEvent.type.source().handler().parseEventJSON(JSONUtils.get(is), jsonEvent.type, jsonEvent.id, jsonEvent.start, jsonEvent.end, full);
        }
    }

    private static List<JHVEvent> parseFullJSON(List<JsonEvent> jsonEvents) {
        HashSet<Integer> ids = new HashSet<>();
        List<JHVEvent> events = new ArrayList<>();
        for (int i = 0; i < jsonEvents.size(); i++) {
            JsonEvent jsonEvent = jsonEvents.get(i);
            jsonEvents.set(i, null);
            if (!ids.add(jsonEvent.id))
                continue;

            try {
                events.add(parseJSON(jsonEvent, true));
            } catch (Exception e) {
                Log.error(e);
            }
        }
        return events;
    }

    public static EventDetails getEventDetails(int id, SWEKSupplier supplier) throws Exception {
        JsonEventDetails details = executor.invokeAndWait(() ->
                new JsonEventDetails(queryEvent(id), collectRelationEvents(id, supplier)));
        return new EventDetails(parseJSON(details.event(), true), parseFullJSON(details.relatedEvents()));
    }

    private static JsonEvent queryEvent(int id) throws Exception {
        PreparedStatement statement = getPreparedStatement(SELECT_EVENT);
        statement.setInt(1, id);
        try (ResultSet result = statement.executeQuery()) {
            if (!result.next())
                throw new SQLException("Event not found: " + id);
            return new JsonEvent(result.getBytes(4), SWEKCatalog.getSupplier(result.getString(5)),
                    result.getInt(1), result.getLong(2), result.getLong(3));
        }
    }

    private static List<JsonEvent> collectRelationEvents(int id, SWEKSupplier supplier) {
        SWEKGroup group = supplier.group();
        List<JsonEvent> jsonEvents = new ArrayList<>();

        for (SWEK.RelatedEvents re : SWEKCatalog.getRelatedEvents()) {
            if (re.group() == group) {
                for (SWEK.RelatedOn swon : re.relatedOnList()) {
                    addRelationEvents(jsonEvents, id, supplier, re.relatedWith(),
                            swon.parameterFrom(), swon.parameterWith());
                }
            }

            if (re.relatedWith() == group) {
                for (SWEK.RelatedOn swon : re.relatedOnList()) {
                    if (re.group() == group && swon.parameterFrom().equals(swon.parameterWith()))
                        continue;
                    addRelationEvents(jsonEvents, id, supplier, re.group(),
                            swon.parameterWith(), swon.parameterFrom());
                }
            }
        }

        return jsonEvents;
    }

    private static void addRelationEvents(List<JsonEvent> jsonEvents, int id, SWEKSupplier supplier,
                                          SWEKGroup relatedGroup, String eventParameter, String relatedParameter) {
        for (SWEKSupplier relatedSupplier : SWEKCatalog.getSuppliers(relatedGroup)) {
            if (relatedSupplier == supplier)
                continue;

            try {
                jsonEvents.addAll(queryRelationEvents(id, supplier, relatedSupplier, eventParameter, relatedParameter));
            } catch (Exception e) {
                Log.error(e);
            }
        }
    }

    public static boolean addStoredInterval(long start, long end, SWEKSupplier type) {
        try {
            executor.invokeAndWait(new AddStoredInterval(start, end, type));
            return true;
        } catch (Exception e) {
            Log.error("Could not store event date range", e);
            return false;
        }
    }

    private record AddStoredInterval(long start, long end, SWEKSupplier type) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            RequestCache typedCache = getStoredIntervals(type);
            int typeId = findOrInsertEventTypeId(type);
            Connection connection = EventDatabaseThread.getConnection();
            typedCache.adaptRequestCache(start, end);
            try {
                PreparedStatement delete = getPreparedStatement(DELETE_DATERANGES);
                delete.setInt(1, typeId);
                delete.executeUpdate();

                PreparedStatement pstatement = getPreparedStatement(INSERT_DATERANGE);
                for (Interval interval : typedCache.getAllRequestIntervals()) {
                    pstatement.setInt(1, typeId);
                    pstatement.setLong(2, interval.start());
                    pstatement.setLong(3, interval.end());
                    pstatement.executeUpdate();
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                storedIntervals.remove(type);
                throw e;
            }
            return null;
        }
    }

    public static boolean isStored(long start, long end, SWEKSupplier type) {
        try {
            return executor.invokeAndWait(new IsStored(start, end, type));
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    private record IsStored(long start, long end, SWEKSupplier type) implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            for (Interval interval : getStoredIntervals(type).getAllRequestIntervals()) {
                if (interval.start() > start)
                    return false;
                if (interval.end() >= end)
                    return true;
            }
            return false;
        }
    }

    private static RequestCache getStoredIntervals(SWEKSupplier type) throws Exception {
        RequestCache typedCache = storedIntervals.get(type);
        if (typedCache != null)
            return typedCache;

        typedCache = new RequestCache();
        int typeId = findEventTypeId(type);
        if (typeId == -1) {
            storedIntervals.put(type, typedCache);
            return typedCache;
        }

        long last_timestamp = getLastEvent(typeId);
        long now = System.currentTimeMillis();
        long lastEvent = last_timestamp == Long.MIN_VALUE ? now : Math.min(now, last_timestamp);
        long invalidationDate = lastEvent - ONEWEEK * 2;

        PreparedStatement pstatement = getPreparedStatement(SELECT_DATERANGE);
        pstatement.setInt(1, typeId);
        try (ResultSet rs = pstatement.executeQuery()) {
            while (rs.next()) {
                long beginDate = rs.getLong(1);
                long endDate = Math.min(invalidationDate, rs.getLong(2));
                if (beginDate < endDate)
                    typedCache.adaptRequestCache(beginDate, endDate);
            }
        }
        storedIntervals.put(type, typedCache);
        return typedCache;
    }

    private static long getLastEvent(int typeId) throws Exception {
        long last_timestamp = Long.MIN_VALUE;
        PreparedStatement pstatement = getPreparedStatement(SELECT_LAST_EVENT);
        pstatement.setInt(1, typeId);
        try (ResultSet rs = pstatement.executeQuery()) {
            if (rs.next()) {
                last_timestamp = rs.getLong(1);
            }
        }
        return last_timestamp;
    }

    public record EventDetails(JHVEvent event, List<JHVEvent> relatedEvents) {}

    private record JsonEvent(byte[] json, SWEKSupplier type, int id, long start, long end) {}

    private record JsonEventDetails(JsonEvent event, List<JsonEvent> relatedEvents) {}

    public static List<JHVEvent> events2Program(long start, long end, SWEKSupplier type, List<SWEK.Param> params) throws Exception {
        return executor.invokeAndWait(new Events2Program(start, end, type, params));
    }

    private record Events2Program(long start, long end, SWEKSupplier type, List<SWEK.Param> params)
            implements Callable<List<JHVEvent>> {
        @Override
        public List<JHVEvent> call() throws Exception {
            List<JHVEvent> eventList = new ArrayList<>();
            int typeId = findEventTypeId(type);
            if (typeId == -1)
                return eventList;

            String join = "LEFT JOIN " + type.dbName() + " AS tp ON tp.event_id=e.id";
            StringBuilder filters = new StringBuilder();
            for (SWEK.Param p : params) {
                filters.append("AND tp.").append(p.name()).append(p.operand().representation).append("? ");
            }
            String sqlt = "SELECT e.id, e.start, e.end, e.data FROM events AS e " + join +
                    " WHERE e.type_id=? AND e.start<=? AND e.end>=? " + filters + " order by e.start, e.end ";
            PreparedStatement pstatement = getPreparedStatement(sqlt);
            pstatement.setInt(1, typeId);
            pstatement.setLong(2, end);
            pstatement.setLong(3, start);
            int parameterIndex = 4;
            for (SWEK.Param param : params)
                pstatement.setDouble(parameterIndex++, param.value());

            try (ResultSet rs = pstatement.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    long _start = rs.getLong(2);
                    long _end = rs.getLong(3);
                    byte[] json = rs.getBytes(4);
                    try {
                        eventList.add(parseJSON(new JsonEvent(json, type, id, _start, _end), false));
                    } catch (Exception e) {
                        Log.error(e);
                    }
                }
            }
            return eventList;
        }
    }

    public static List<JHVEvent.Link> associations2Program(long start, long end, SWEKSupplier type) throws Exception {
        return executor.invokeAndWait(new Associations2Program(start, end, type));
    }

    private record Associations2Program(long start, long end, SWEKSupplier type)
            implements Callable<List<JHVEvent.Link>> {
        @Override
        public List<JHVEvent.Link> call() throws Exception {
            List<JHVEvent.Link> assocList = new ArrayList<>();
            int typeId = findEventTypeId(type);
            if (typeId == -1)
                return assocList;

            PreparedStatement pstatement = getPreparedStatement(SELECT_ASSOCIATIONS);
            pstatement.setInt(1, typeId);
            pstatement.setLong(2, end);
            pstatement.setLong(3, start);
            pstatement.setInt(4, typeId);
            pstatement.setLong(5, end);
            pstatement.setLong(6, start);

            try (ResultSet rs = pstatement.executeQuery()) {
                while (rs.next()) {
                    assocList.add(new JHVEvent.Link(rs.getInt(1), rs.getInt(2)));
                }
            }
            return assocList;
        }
    }

    private static List<JsonEvent> queryRelationEvents(int eventId, SWEKSupplier leftType, SWEKSupplier rightType,
                                                       String leftParameter, String rightParameter) throws Exception {
        List<JsonEvent> ret = new ArrayList<>();
        if (findEventTypeId(leftType) == -1 || findEventTypeId(rightType) == -1)
            return ret;

        String sql = "SELECT e.id, e.start, e.end, e.data, event_type.supplier FROM events AS e " +
                "LEFT JOIN event_type ON e.type_id=event_type.id WHERE e.id IN (" +
                "SELECT CASE WHEN tl.event_id=? THEN tr.event_id ELSE tl.event_id END " +
                "FROM " + leftType.dbName() + " AS tl " +
                "JOIN " + rightType.dbName() + " AS tr ON tl." + leftParameter + "=tr." + rightParameter + ' ' +
                "WHERE tl.event_id!=tr.event_id AND (tl.event_id=? OR tr.event_id=?))";
        PreparedStatement statement = getPreparedStatement(sql);
        statement.setInt(1, eventId);
        statement.setInt(2, eventId);
        statement.setInt(3, eventId);

        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt(1);
                long start = rs.getLong(2);
                long end = rs.getLong(3);
                byte[] json = rs.getBytes(4);
                ret.add(new JsonEvent(json, SWEKCatalog.getSupplier(rs.getString(5)), id, start, end));
            }
        }
        return ret;
    }

}
