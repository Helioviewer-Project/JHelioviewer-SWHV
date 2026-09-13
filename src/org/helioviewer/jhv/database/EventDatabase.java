package org.helioviewer.jhv.database;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.event.EventBatch;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;

public class EventDatabase {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(new AppThread.NamedThreadFactory("EventDatabase"));
    private static long batchSequence;

    private static final long ONEWEEK = 1000 * 60 * 60 * 24 * 7;
    public static int config_hash;

    private static final String INSERT_EVENT = "INSERT INTO events(uid) VALUES(?)";
    private static final String UPSERT_EVENT =
            "INSERT INTO events(type_id, uid, start, end, archiv, data) VALUES(?,?,?,?,?,?) " +
                    "ON CONFLICT(uid) DO UPDATE SET type_id=excluded.type_id, start=excluded.start, end=excluded.end, archiv=excluded.archiv, data=excluded.data RETURNING id";
    private static final String SELECT_EVENT_TYPE = "SELECT id FROM event_type WHERE name=? AND supplier=?";
    private static final String INSERT_EVENT_TYPE = "INSERT INTO event_type(name, supplier) VALUES(?,?)";
    private static final String DELETE_PARAMETERS = "DELETE FROM event_parameter WHERE event_id=?";
    private static final String INSERT_PARAMETER = "INSERT INTO event_parameter(event_id,name,type_id,value) VALUES(?,?,?,?)";
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
    private static final String SELECT_PARAMETER_ASSOCIATIONS =
            "SELECT min(a.event_id, b.event_id), max(a.event_id, b.event_id) " +
                    "FROM events AS e JOIN event_parameter AS a ON a.event_id=e.id JOIN event_parameter AS b ON b.value=a.value " +
                    "WHERE e.type_id=? AND e.start<=? AND e.end>=? AND a.name=? AND b.type_id=? AND b.name=? AND a.event_id!=b.event_id";
    private static final String SELECT_EVENT =
            "SELECT e.id, e.start, e.end, e.data, event_type.supplier FROM events AS e " +
                    "LEFT JOIN event_type ON e.type_id=event_type.id WHERE e.id=?";
    private static final HashMap<String, PreparedStatement> statements = new HashMap<>();
    private static final HashMap<SWEKSupplier, RequestCache> storedIntervals = new HashMap<>();

    public record EventDetails(SolarEvent event, List<SolarEvent> relatedEvents) {}

    private record StoredEvent(byte[] json, SWEKSupplier type, int id, long start, long end) {}

    private record StoredDetails(StoredEvent event, List<StoredEvent> relatedEvents) {}

    private record StoredBatch(long sequence, List<StoredEvent> events, List<SolarEvent.Link> associations) {}

    public static boolean storeRemotePage(SWEKHandler.RemotePage remotePage, SWEKSupplier supplier) {
        try {
            executor.submit(() -> {
                storePage(remotePage, supplier);
                return null;
            }).get();
            return true;
        } catch (Exception e) {
            Log.error("Could not store event page", e);
            return false;
        }
    }

    private static void storePage(SWEKHandler.RemotePage remotePage, SWEKSupplier supplier) throws Exception {
        Connection connection = EventDatabaseConnection.getConnection();
        try {
            storeEvents(remotePage.events(), supplier);
            storeAssociations(remotePage.associations());
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        }
    }

    private static void storeEvents(List<SWEKHandler.RemoteEvent> remoteEvents, SWEKSupplier supplier) throws Exception {
        int typeId = findOrInsertEventTypeId(supplier);

        PreparedStatement statement = getPreparedStatement(UPSERT_EVENT);
        PreparedStatement delete = getPreparedStatement(DELETE_PARAMETERS);
        PreparedStatement parameter = getPreparedStatement(INSERT_PARAMETER);
        Set<String> fields = SWEKCatalog.indexedParameters(supplier).keySet();

        for (SWEKHandler.RemoteEvent remoteEvent : remoteEvents) {
            statement.setInt(1, typeId);
            statement.setString(2, remoteEvent.uid());
            statement.setLong(3, remoteEvent.start());
            statement.setLong(4, remoteEvent.end());
            statement.setLong(5, remoteEvent.archiv());
            statement.setBytes(6, remoteEvent.compressedJson());
            int eventId;
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next())
                    throw new SQLException("Could not store event " + remoteEvent.uid());
                eventId = rs.getInt(1);
            }

            delete.setInt(1, eventId);
            delete.executeUpdate();
            for (String field : fields) {
                Number value = remoteEvent.indexedValues().get(field);
                if (value == null) continue;
                parameter.setInt(1, eventId);
                parameter.setString(2, field);
                parameter.setInt(3, typeId);
                switch (value) {
                    case Integer i -> parameter.setInt(4, i);
                    case Double d -> parameter.setDouble(4, d);
                    default -> throw new IllegalArgumentException("Unsupported indexed event value: " + value);
                }
                parameter.executeUpdate();
            }
        }
    }

    private static void storeAssociations(List<SolarEvent.LinkRef> links) throws Exception {
        PreparedStatement pstatement = getPreparedStatement(INSERT_LINK);

        for (SolarEvent.LinkRef link : links) {
            int id0 = findOrInsertEventId(link.firstUid());
            int id1 = findOrInsertEventId(link.secondUid());
            insertAssociation(pstatement, id0, id1);
        }
    }

    private static void insertAssociation(PreparedStatement statement, int id0, int id1) throws SQLException {
        if (id0 == id1)
            return;

        statement.setInt(1, Math.min(id0, id1));
        statement.setInt(2, Math.max(id0, id1));
        statement.executeUpdate();
    }

    public static EventBatch loadEvents(long start, long end, SWEKSupplier type, List<SWEK.Param> params) throws Exception {
        StoredBatch batch = executor.submit(() -> new StoredBatch(++batchSequence,
                queryEvents(start, end, type, params), queryAssociations(start, end, type))).get();
        return new EventBatch(batch.sequence(), parseEvents(batch.events(), false), batch.associations());
    }

    private static List<StoredEvent> queryEvents(long start, long end, SWEKSupplier type, List<SWEK.Param> params) throws Exception {
        List<StoredEvent> eventList = new ArrayList<>();
        int typeId = findEventTypeId(type);
        if (typeId == -1)
            return eventList;

        StringBuilder joins = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            SWEK.Param param = params.get(i);
            if (!SWEKCatalog.indexedParameters(type).containsKey(param.name()))
                throw new IllegalArgumentException("Unknown indexed parameter: " + param.name());
            String alias = "p" + i;
            joins.append(" JOIN event_parameter ").append(alias).append(" ON ").append(alias).append(".event_id=e.id AND ")
                    .append(alias).append(".type_id=e.type_id AND ").append(alias).append(".name=? AND ")
                    .append(alias).append(".value").append(param.operand().representation).append('?');
        }
        String sql = "SELECT e.id, e.start, e.end, e.data FROM events AS e" + joins +
                " WHERE e.type_id=? AND e.start<=? AND e.end>=? ORDER BY e.start, e.end";
        PreparedStatement pstatement = getPreparedStatement(sql);
        int parameterIndex = 1;
        for (SWEK.Param param : params) {
            pstatement.setString(parameterIndex++, param.name());
            pstatement.setDouble(parameterIndex++, param.value());
        }
        pstatement.setInt(parameterIndex++, typeId);
        pstatement.setLong(parameterIndex++, end);
        pstatement.setLong(parameterIndex, start);

        try (ResultSet rs = pstatement.executeQuery()) {
            while (rs.next()) {
                eventList.add(readStoredEvent(rs, type));
            }
        }
        return eventList;
    }

    private static List<SolarEvent.Link> queryAssociations(long start, long end, SWEKSupplier type) throws Exception {
        List<SolarEvent.Link> links = new ArrayList<>();
        int typeId = findEventTypeId(type);
        if (typeId == -1)
            return links;

        List<SWEK.RelatedOn> parameters = associationParameters(type.group());
        String sql = SELECT_ASSOCIATIONS + (" UNION " + SELECT_PARAMETER_ASSOCIATIONS).repeat(parameters.size()) + " ORDER BY 1,2";
        PreparedStatement pstatement = getPreparedStatement(sql);
        pstatement.setInt(1, typeId);
        pstatement.setLong(2, end);
        pstatement.setLong(3, start);
        pstatement.setInt(4, typeId);
        pstatement.setLong(5, end);
        pstatement.setLong(6, start);
        int index = 7;
        for (SWEK.RelatedOn field : parameters) {
            pstatement.setInt(index++, typeId);
            pstatement.setLong(index++, end);
            pstatement.setLong(index++, start);
            pstatement.setString(index++, field.parameterFrom());
            pstatement.setInt(index++, typeId);
            pstatement.setString(index++, field.parameterWith());
        }

        try (ResultSet rs = pstatement.executeQuery()) {
            while (rs.next()) {
                links.add(new SolarEvent.Link(rs.getInt(1), rs.getInt(2)));
            }
        }
        return links;
    }

    private static List<SWEK.RelatedOn> associationParameters(SWEKGroup group) {
        List<SWEK.RelatedOn> parameters = new ArrayList<>();
        for (SWEK.Relation relation : SWEKCatalog.getRelations()) {
            if (relation.group() != group || relation.relatedWith() != group)
                continue;
            for (SWEK.RelatedOn field : relation.relatedOnList()) {
                parameters.add(field);
                if (!field.parameterFrom().equals(field.parameterWith()))
                    parameters.add(new SWEK.RelatedOn(field.parameterWith(), field.parameterFrom()));
            }
        }
        return parameters;
    }

    public static EventDetails getEventDetails(int id) throws Exception {
        StoredDetails details = executor.submit(() -> {
            StoredEvent event = queryEvent(id);
            return new StoredDetails(event, collectRelationEvents(id, event.type()));
        }).get();
        return new EventDetails(parseJSON(details.event(), true), parseEvents(details.relatedEvents(), true));
    }

    private static StoredEvent queryEvent(int id) throws Exception {
        PreparedStatement statement = getPreparedStatement(SELECT_EVENT);
        statement.setInt(1, id);
        try (ResultSet result = statement.executeQuery()) {
            if (!result.next())
                throw new SQLException("Event not found: " + id);
            return readStoredEvent(result, SWEKCatalog.getSupplier(result.getString(5)));
        }
    }

    private static List<StoredEvent> collectRelationEvents(int id, SWEKSupplier supplier) {
        SWEKGroup group = supplier.group();
        List<StoredEvent> storedEvents = new ArrayList<>();

        for (SWEK.Relation relation : SWEKCatalog.getRelations()) {
            if (relation.group() == group) {
                for (SWEK.RelatedOn parameters : relation.relatedOnList()) {
                    addRelationEvents(storedEvents, id, supplier, relation.relatedWith(),
                            parameters.parameterFrom(), parameters.parameterWith());
                }
            }

            if (relation.relatedWith() == group) {
                for (SWEK.RelatedOn parameters : relation.relatedOnList()) {
                    if (relation.group() == group && parameters.parameterFrom().equals(parameters.parameterWith()))
                        continue;
                    addRelationEvents(storedEvents, id, supplier, relation.group(),
                            parameters.parameterWith(), parameters.parameterFrom());
                }
            }
        }

        return storedEvents;
    }

    private static void addRelationEvents(List<StoredEvent> storedEvents, int id, SWEKSupplier supplier,
                                          SWEKGroup relatedGroup, String eventParameter, String relatedParameter) {
        for (SWEKSupplier relatedSupplier : SWEKCatalog.getSuppliers(relatedGroup)) {
            if (relatedSupplier == supplier)
                continue;

            try {
                storedEvents.addAll(queryRelationEvents(id, relatedSupplier, eventParameter, relatedParameter));
            } catch (Exception e) {
                Log.error(e);
            }
        }
    }

    private static List<StoredEvent> queryRelationEvents(int eventId, SWEKSupplier rightType,
                                                         String leftParameter, String rightParameter) throws Exception {
        List<StoredEvent> ret = new ArrayList<>();
        int rightTypeId = findEventTypeId(rightType);
        if (rightTypeId == -1)
            return ret;

        String sql = "SELECT e.id, e.start, e.end, e.data FROM events AS e WHERE e.id IN (" +
                "SELECT tr.event_id " +
                "FROM event_parameter AS tl JOIN event_parameter AS tr ON tl.value=tr.value " +
                "WHERE tl.event_id=? AND tr.type_id=? AND tl.name=? AND tr.name=?)";
        PreparedStatement statement = getPreparedStatement(sql);
        statement.setInt(1, eventId);
        statement.setInt(2, rightTypeId);
        statement.setString(3, leftParameter);
        statement.setString(4, rightParameter);

        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                ret.add(readStoredEvent(rs, rightType));
            }
        }
        return ret;
    }

    public static boolean addStoredInterval(long start, long end, SWEKSupplier type) {
        try {
            executor.submit(() -> {
                storeInterval(start, end, type);
                return null;
            }).get();
            return true;
        } catch (Exception e) {
            Log.error("Could not store event date range", e);
            return false;
        }
    }

    private static void storeInterval(long start, long end, SWEKSupplier type) throws Exception {
        RequestCache typedCache = getStoredIntervals(type);
        int typeId = findOrInsertEventTypeId(type);
        Connection connection = EventDatabaseConnection.getConnection();
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
    }

    public static boolean isStored(long start, long end, SWEKSupplier type) {
        try {
            return executor.submit(() -> containsInterval(start, end, type)).get();
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    private static boolean containsInterval(long start, long end, SWEKSupplier type) throws Exception {
        for (Interval interval : getStoredIntervals(type).getAllRequestIntervals()) {
            if (interval.start() > start)
                return false;
            if (interval.end() >= end)
                return true;
        }
        return false;
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

    private static int findOrInsertEventTypeId(SWEKSupplier supplier) throws Exception {
        int typeId = findEventTypeId(supplier);
        if (typeId == -1) {
            PreparedStatement statement = getPreparedStatement(INSERT_EVENT_TYPE);
            statement.setString(1, supplier.group().getName());
            statement.setString(2, supplier.id());
            statement.executeUpdate();
            typeId = findEventTypeId(supplier);
        }
        if (typeId == -1)
            throw new SQLException("Could not create event type " + supplier.id());
        return typeId;
    }

    private static int findEventTypeId(SWEKSupplier supplier) throws Exception {
        PreparedStatement pstatement = getPreparedStatement(SELECT_EVENT_TYPE);
        pstatement.setString(1, supplier.group().getName());
        pstatement.setString(2, supplier.id());

        try (ResultSet rs = pstatement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static int findOrInsertEventId(String uid) throws Exception {
        int id = findEventId(uid);
        if (id == -1) {
            PreparedStatement statement = getPreparedStatement(INSERT_EVENT);
            statement.setString(1, uid);
            statement.executeUpdate();
            id = findEventId(uid);
        }
        if (id == -1)
            throw new SQLException("Could not create event " + uid);
        return id;
    }

    private static int findEventId(String uid) throws Exception {
        PreparedStatement pstatement = getPreparedStatement(SELECT_EVENT_ID_FROM_UID);
        pstatement.setString(1, uid);

        try (ResultSet rs = pstatement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static PreparedStatement getPreparedStatement(String statement) throws Exception {
        PreparedStatement pstat = statements.get(statement);
        if (pstat == null) {
            pstat = EventDatabaseConnection.getConnection().prepareStatement(statement);
            pstat.setQueryTimeout(30);
            statements.put(statement, pstat);
        }
        return pstat;
    }

    private static StoredEvent readStoredEvent(ResultSet result, SWEKSupplier supplier) throws SQLException {
        return new StoredEvent(result.getBytes(4), supplier, result.getInt(1), result.getLong(2), result.getLong(3));
    }

    private static SolarEvent parseJSON(StoredEvent storedEvent, boolean full) throws Exception {
        try (InputStream bais = new ByteArrayInputStream(storedEvent.json); InputStream is = new GZIPInputStream(bais)) {
            return storedEvent.type.source().handler().parseEventJSON(JSONUtils.get(is), storedEvent.type, storedEvent.id, storedEvent.start, storedEvent.end, full);
        }
    }

    private static List<SolarEvent> parseEvents(List<StoredEvent> storedEvents, boolean full) {
        HashSet<Integer> ids = new HashSet<>();
        List<SolarEvent> events = new ArrayList<>();
        for (int i = 0; i < storedEvents.size(); i++) {
            StoredEvent storedEvent = storedEvents.get(i);
            storedEvents.set(i, null);
            if (!ids.add(storedEvent.id))
                continue;

            try {
                events.add(parseJSON(storedEvent, full));
            } catch (Exception e) {
                Log.error(e);
            }
        }
        return events;
    }

}
