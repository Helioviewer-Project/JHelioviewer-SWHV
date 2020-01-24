package org.helioviewer.jhv.database;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.interval.RequestCache;
import org.helioviewer.jhv.events.JHVAssociation;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKParam;
import org.helioviewer.jhv.events.SWEKRelatedEvents;
import org.helioviewer.jhv.events.SWEKRelatedOn;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.threads.SingleExecutor;

public class EventDatabase {

    private static final SingleExecutor executor = new SingleExecutor(new JHVThread.NamedClassThreadFactory(EventDatabaseThread.class, "EventDatabase"));

    public static class Event2Db {
        final byte[] compressedJson;
        final long start;
        final long end;
        final long archiv;
        final String uid;
        final ArrayList<JHVDatabaseParam> paramList;

        public Event2Db(byte[] _compressedJson, long _start, long _end, long _archiv, String _uid, ArrayList<JHVDatabaseParam> _paramList) {
            compressedJson = _compressedJson;
            start = _start;
            end = _end;
            uid = _uid;
            archiv = _archiv;
            paramList = _paramList;
        }

    }

    private static final long ONEWEEK = 1000 * 60 * 60 * 24 * 7;
    public static int config_hash;

    private static final String INSERT_EVENT = "INSERT INTO events(uid) VALUES(?)";
    private static final String INSERT_FULL_EVENT = "INSERT INTO events(type_id, uid, start, end, archiv, data) VALUES(?,?,?,?,?,?)";
    private static final String SELECT_EVENT_TYPE = "SELECT id FROM event_type WHERE name=? AND supplier=?";
    private static final String INSERT_EVENT_TYPE = "INSERT INTO event_type(name, supplier) VALUES(?,?)";
    private static final String INSERT_LINK = "INSERT INTO event_link(left_id, right_id) VALUES(?,?)";
    private static final String SELECT_EVENT_ID_FROM_UID = "SELECT id FROM events WHERE uid=?";
    private static final String SELECT_LAST_INSERT = "SELECT last_insert_rowid()";
    private static final String UPDATE_EVENT = "UPDATE events SET type_id=?, uid=?,  start=?, end=?, data=? WHERE id=?";
    private static final String DELETE_DATERANGE = "DELETE FROM date_range where type_id=?";
    private static final String INSERT_DATERANGE = "INSERT INTO date_range(type_id,  start, end) VALUES(?,?,?)";
    private static final String SELECT_DATERANGE = "SELECT start, end FROM date_range where type_id=? order by start, end ";
    private static final String SELECT_LAST_EVENT = "SELECT end FROM events WHERE type_id=? order by end DESC LIMIT 1";
    private static final String SELECT_ASSOCIATIONS = "SELECT left_events.id, right_events.id FROM event_link " + "LEFT JOIN events AS left_events ON left_events.id=event_link.left_id " + "LEFT JOIN events AS right_events ON right_events.id=event_link.right_id " + "WHERE left_events.start BETWEEN ? AND ? and left_events.type_id=? order by left_events.start, left_events.end ";
    private static final String SELECT_EVENT_BY_ID = "SELECT e.id, e.start, e.end, e.data, event_type.supplier FROM events AS e LEFT JOIN event_type ON e.type_id = event_type.id WHERE e.id=?";

    private static final HashMap<Object, PreparedStatement> statements = new HashMap<>();

    private static final HashMap<SWEKSupplier, RequestCache> downloadedCache = new HashMap<>();

    private static PreparedStatement getPreparedStatement(Connection connection, String statement) throws SQLException {
        statement = statement.intern();
        PreparedStatement pstat = statements.get(statement);
        if (pstat == null) {
            pstat = connection.prepareStatement(statement);
            pstat.setQueryTimeout(30);
            statements.put(statement, pstat);
        }
        return pstat;
    }

    private static int getEventTypeId(Connection connection, SWEKSupplier eventType) throws SQLException {
        int typeId = _getEventTypeId(connection, eventType);
        if (typeId == -1) {
            insertEventTypeIfNotExist(connection, eventType);
            typeId = _getEventTypeId(connection, eventType);
        }
        return typeId;
    }

    private static int _getEventTypeId(Connection connection, SWEKSupplier event) throws SQLException {
        int typeId = -1;
        PreparedStatement pstatement = getPreparedStatement(connection, SELECT_EVENT_TYPE);
        pstatement.setString(1, event.getGroup().getName());
        pstatement.setString(2, event.getKey());

        try (ResultSet rs = pstatement.executeQuery()) {
            if (rs.next()) {
                typeId = rs.getInt(1);
            }
        }
        return typeId;
    }

    private static void insertEventTypeIfNotExist(Connection connection, SWEKSupplier eventType) throws SQLException {
        PreparedStatement pstatement = getPreparedStatement(connection, INSERT_EVENT_TYPE);
        pstatement.setString(1, eventType.getGroup().getName());
        pstatement.setString(2, eventType.getKey());
        pstatement.executeUpdate();

        StringBuilder createtbl = new StringBuilder("CREATE TABLE ").append(eventType.getDatabaseName()).append(" (");
        eventType.getGroup().getAllDatabaseFields().forEach((key, value) -> createtbl.append(key).append(' ').append(value).append(" DEFAULT NULL,"));
        createtbl.append("event_id INTEGER, id INTEGER PRIMARY KEY AUTOINCREMENT, FOREIGN KEY(event_id) REFERENCES events(id), UNIQUE(event_id) ON CONFLICT REPLACE )");

        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.executeUpdate(createtbl.toString());
        }
        connection.commit();
    }

    private static int getIdFromUID(Connection connection, String uid) throws SQLException {
        int id = _getIdFromUID(connection, uid);
        if (id == -1) {
            insertVoidEvent(connection, uid);
            id = _getIdFromUID(connection, uid);
        }
        return id;
    }

    private static int _getIdFromUID(Connection connection, String uid) throws SQLException {
        int id = -1;
        PreparedStatement pstatement = getPreparedStatement(connection, SELECT_EVENT_ID_FROM_UID);
        pstatement.setString(1, uid);

        try (ResultSet rs = pstatement.executeQuery()) {
            if (rs.next()) {
                id = rs.getInt(1);
            }
        }
        return id;
    }

    private static void insertVoidEvent(Connection connection, String uid) throws SQLException {
        PreparedStatement pstatement = getPreparedStatement(connection, INSERT_EVENT);
        pstatement.setString(1, uid);
        pstatement.executeUpdate();
    }

    private static int dump_associationint2db(Connection connection, ArrayList<Pair<Integer, Integer>> assocs) throws SQLException {
        int len = assocs.size();
        int i = 0;
        int errorcode = 0;
        while (i < len && errorcode == 0) {
            Pair<Integer, Integer> assoc = assocs.get(i);
            int id0 = assoc.a;
            int id1 = assoc.b;

            if (id0 != -1 && id1 != -1 && id0 != id1) {
                PreparedStatement pstatement = getPreparedStatement(connection, INSERT_LINK);
                /* Avoid circular insertions by pre-ordering events */
                if (id0 < id1) {
                    pstatement.setInt(1, id0);
                    pstatement.setInt(2, id1);
                } else {
                    pstatement.setInt(1, id1);
                    pstatement.setInt(2, id0);
                }
                pstatement.executeUpdate();
            } else if (id0 != id1) {
                errorcode = -1;
                Log.error("Could not add association to database ");
            }
            i++;
        }
        connection.commit();
        return errorcode;
    }

    public static int dump_association2db(Pair<String, String>[] assocs) {
        try {
            return executor.invokeAndWait(new DumpAssociation2Db(assocs));
        } catch (Exception e) {
            Log.error(e);
        }
        return -1;
    }

    private static class DumpAssociation2Db implements Callable<Integer> {
        private final Pair<String, String>[] assocs;

        DumpAssociation2Db(Pair<String, String>[] _assocs) {
            assocs = _assocs;
        }

        @Override
        public Integer call() throws SQLException {
            Connection connection = EventDatabaseThread.getConnection();
            if (connection == null) {
                return -1;
            }

            int len = assocs.length;
            int i = 0;
            int errorcode = 0;
            while (i < len && errorcode == 0) {
                Pair<String, String> assoc = assocs[i];
                int id0 = getIdFromUID(connection, assoc.a);
                int id1 = getIdFromUID(connection, assoc.b);
                if (id0 != -1 && id1 != -1) {
                    PreparedStatement pstatement = getPreparedStatement(connection, INSERT_LINK);
                    pstatement.setInt(1, id0);
                    pstatement.setInt(2, id1);
                    pstatement.executeUpdate();
                } else {
                    errorcode = -1;
                    Log.error("Could not add association to database ");
                }
                i++;
            }
            connection.commit();
            return errorcode;
        }
    }

    private static int getEventId(Connection connection, String uid) throws SQLException {
        int generatedKey = -1;
        PreparedStatement pstatement = getPreparedStatement(connection, SELECT_EVENT_ID_FROM_UID);
        pstatement.setString(1, uid);

        try (ResultSet rs = pstatement.executeQuery()) {
            if (rs.next()) {
                generatedKey = rs.getInt(1);
            }
        }
        return generatedKey;
    }

    private static int[] get_id_init_list(int sz) {
        int[] inserted_ids = new int[sz];
        Arrays.fill(inserted_ids, -1);
        return inserted_ids;
    }

    public static int[] dump_event2db(ArrayList<Event2Db> event2db_list, SWEKSupplier type) {
        try {
            return executor.invokeAndWait(new DumpEvent2Db(event2db_list, type));
        } catch (Exception e) {
            Log.error(e);
        }
        return get_id_init_list(event2db_list.size());
    }

    private static class DumpEvent2Db implements Callable<int[]> {
        private final SWEKSupplier type;
        private final ArrayList<Event2Db> event2db_list;

        DumpEvent2Db(ArrayList<Event2Db> _event2db_list, SWEKSupplier _type) {
            event2db_list = _event2db_list;
            type = _type;
        }

        @Override
        public int[] call() throws SQLException {
            int[] inserted_ids = get_id_init_list(event2db_list.size());
            Connection connection = EventDatabaseThread.getConnection();
            if (connection == null) {
                return inserted_ids;
            }

            int typeId = getEventTypeId(connection, type);
            int llen = event2db_list.size();
            for (int i = 0; i < llen; i++) {
                Event2Db event2db = event2db_list.get(i);
                int generatedKey = -1;
                if (typeId != -1) {
                    generatedKey = getEventId(connection, event2db.uid);

                    if (generatedKey == -1) {
                        {
                            PreparedStatement pstatement = getPreparedStatement(connection, INSERT_FULL_EVENT);
                            pstatement.setInt(1, typeId);
                            pstatement.setString(2, event2db.uid);
                            pstatement.setLong(3, event2db.start);
                            pstatement.setLong(4, event2db.end);
                            pstatement.setLong(5, event2db.archiv);
                            pstatement.setBinaryStream(6, new ByteArrayInputStream(event2db.compressedJson), event2db.compressedJson.length);
                            pstatement.executeUpdate();
                        }
                        {
                            PreparedStatement pstatement = getPreparedStatement(connection, SELECT_LAST_INSERT);

                            try (ResultSet rs = pstatement.executeQuery()) {
                                if (rs.next()) {
                                    generatedKey = rs.getInt(1);
                                }
                            }
                        }
                    } else {
                        PreparedStatement pstatement = getPreparedStatement(connection, UPDATE_EVENT);
                        pstatement.setInt(1, typeId);
                        pstatement.setString(2, event2db.uid);
                        pstatement.setLong(3, event2db.start);
                        pstatement.setLong(4, event2db.end);
                        pstatement.setBinaryStream(5, new ByteArrayInputStream(event2db.compressedJson), event2db.compressedJson.length);
                        pstatement.setInt(6, generatedKey);
                        pstatement.executeUpdate();
                    }
                    {
                        StringBuilder fieldString = new StringBuilder();
                        StringBuilder varString = new StringBuilder();
                        for (JHVDatabaseParam p : event2db.paramList) {
                            fieldString.append(',').append(p.getParamName());
                            varString.append(",?");
                        }
                        String full_statement = "INSERT INTO " + type.getDatabaseName() + "(event_id" + fieldString + ") VALUES(?" + varString + ')';
                        PreparedStatement pstatement = getPreparedStatement(connection, full_statement);
                        pstatement.setInt(1, generatedKey);

                        int index = 2;
                        for (JHVDatabaseParam p : event2db.paramList) {
                            if (p.isInt()) {
                                pstatement.setInt(index, p.getIntValue());
                            } else if (p.isString()) {
                                pstatement.setString(index, p.getStringValue());
                            } else if (p.isDouble()) {
                                pstatement.setDouble(index, p.getDoubleValue());
                            }
                            index++;
                        }
                        pstatement.executeUpdate();
                    }
                } else {
                    Log.error("Failed to insert event");
                }
                inserted_ids[i] = generatedKey;
            }
            connection.commit();

            ArrayList<Pair<Integer, Integer>> assocs = new ArrayList<>();
            for (int id : inserted_ids) {
                if (id == -1) {
                    Log.error("Failed to dump to database");
                    assocs.add(new Pair<>(1, 1));
                } else {
                    List<JHVEvent> rels = _getOtherRelations(id, type, true, false, true);
                    rels.forEach(rel -> assocs.add(new Pair<>(id, rel.getUniqueID())));
                }
            }
            dump_associationint2db(connection, assocs);

            return inserted_ids;
        }
    }

    private static JHVEvent parseJSON(JsonEvent jsonEvent, boolean full) throws Exception {
        try (InputStream bais = new ByteArrayInputStream(jsonEvent.json); InputStream is = new GZIPInputStream(bais)) {
            return jsonEvent.type.getSource().getHandler().parseEventJSON(JSONUtils.get(is), jsonEvent.type, jsonEvent.id, jsonEvent.start, jsonEvent.end, full);
        }
    }

    private static List<JHVEvent> createUniqueList(List<JHVEvent> events) {
        HashSet<Integer> ids = new HashSet<>();
        ArrayList<JHVEvent> uniqueEvents = new ArrayList<>();
        for (JHVEvent ev : events) {
            int id = ev.getUniqueID();
            if (!ids.contains(id)) {
                ids.add(id);
                uniqueEvents.add(ev);
            }
        }
        return uniqueEvents;
    }

    public static List<JHVEvent> getOtherRelations(int id, SWEKSupplier jhvEventType, boolean similartype, boolean full) throws SQLException {
        return _getOtherRelations(id, jhvEventType, similartype, full, false);
    }

    //Given an event id and its type, return all related events. If similartype is true, return only related events having the same type.
    private static List<JHVEvent> _getOtherRelations(int id, SWEKSupplier jhvEventType, boolean similartype, boolean full, boolean is_dbthread) throws SQLException {
        SWEKGroup group = jhvEventType.getGroup();
        ArrayList<JHVEvent> nEvents = new ArrayList<>();
        ArrayList<JsonEvent> jsonEvents = new ArrayList<>();

        for (SWEKRelatedEvents re : SWEKGroup.getSWEKRelatedEvents()) {
            if (re.getGroup() == group) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();
                for (SWEKRelatedOn swon : relon) {
                    String f = swon.parameterFrom.getParameterName().toLowerCase();
                    String w = swon.parameterWith.getParameterName().toLowerCase();
                    SWEKGroup reType = re.getRelatedWith();
                    for (SWEKSupplier supplier : reType.getSuppliers()) {
                        if (similartype == (supplier == jhvEventType))
                            if (is_dbthread)
                                jsonEvents.addAll(rel2prog(id, jhvEventType, supplier, f, w));
                            else
                                jsonEvents.addAll(relations2Program(id, jhvEventType, supplier, f, w));
                    }
                }
            }

            if (re.getRelatedWith() == group) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();
                for (SWEKRelatedOn swon : relon) {
                    String f = swon.parameterFrom.getParameterName().toLowerCase();
                    String w = swon.parameterWith.getParameterName().toLowerCase();
                    SWEKGroup reType = re.getGroup();
                    for (SWEKSupplier supplier : reType.getSuppliers()) {
                        if (similartype == (supplier == jhvEventType))
                            if (is_dbthread)
                                jsonEvents.addAll(rel2prog(id, supplier, jhvEventType, f, w));
                            else
                                jsonEvents.addAll(relations2Program(id, supplier, jhvEventType, f, w));
                    }
                }
            }

            for (JsonEvent jsonEvent : jsonEvents) {
                try {
                    nEvents.add(parseJSON(jsonEvent, full));
                } catch (Exception e) {
                    Log.error(e);
                }
            }
            jsonEvents.clear();
        }

        JsonEvent ev;
        if (!is_dbthread && (ev = event2Program(id)) != null) {
            jsonEvents.add(ev);
            try {
                nEvents.add(parseJSON(ev, full));
            } catch (Exception e) {
                Log.error(e);
            }
        }

        return createUniqueList(nEvents);
    }

    public static void addDaterange2db(long start, long end, SWEKSupplier type) {
        executor.invokeLater(new AddDateRange2db(start, end, type));
    }

    private static class AddDateRange2db implements Runnable {
        private final SWEKSupplier type;
        private final long start;
        private final long end;

        AddDateRange2db(long _start, long _end, SWEKSupplier _type) {
            start = _start;
            end = _end;
            type = _type;
        }

        @Override
        public void run() {
            Connection connection = EventDatabaseThread.getConnection();
            if (connection == null) {
                return;
            }
            RequestCache typedCache = downloadedCache.get(type);
            if (typedCache == null) {
                return;
            }

            typedCache.adaptRequestCache(start, end);
            try {
                int typeId = getEventTypeId(connection, type);
                PreparedStatement dstatement = getPreparedStatement(connection, DELETE_DATERANGE);
                dstatement.setInt(1, typeId);
                dstatement.executeUpdate();
                for (Interval interval : typedCache.getAllRequestIntervals()) {
                    if (typeId != -1) {
                        PreparedStatement pstatement = getPreparedStatement(connection, INSERT_DATERANGE);
                        pstatement.setInt(1, typeId);
                        pstatement.setLong(2, interval.start);
                        pstatement.setLong(3, interval.end);
                        pstatement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                Log.error("Could not serialize date_range to database " + e.getMessage());
            }
        }

    }

    public static ArrayList<Interval> db2daterange(SWEKSupplier type) {
        try {
            return executor.invokeAndWait(new Db2DateRange(type));
        } catch (Exception e) {
            Log.error(e);
        }
        return new ArrayList<>();
    }

    private static class Db2DateRange implements Callable<ArrayList<Interval>> {

        private final SWEKSupplier type;

        Db2DateRange(SWEKSupplier _type) {
            type = _type;
        }

        @Override
        public ArrayList<Interval> call() throws SQLException {
            Connection connection = EventDatabaseThread.getConnection();
            if (connection == null) {
                return new ArrayList<>();
            }

            RequestCache typedCache = downloadedCache.get(type);
            if (typedCache == null) {
                typedCache = new RequestCache();
                long last_timestamp = getLastEvent(connection, type);
                long lastEvent = last_timestamp == Long.MIN_VALUE ? Long.MAX_VALUE : Math.min(System.currentTimeMillis(), last_timestamp);
                long invalidationDate = lastEvent - ONEWEEK * 2;
                downloadedCache.put(type, typedCache);

                int typeId = getEventTypeId(connection, type);
                if (typeId != -1) {
                    PreparedStatement pstatement = getPreparedStatement(connection, SELECT_DATERANGE);
                    pstatement.setInt(1, typeId);
                    try (ResultSet rs = pstatement.executeQuery()) {
                        while (rs.next()) {
                            long beginDate = Math.min(invalidationDate, rs.getLong(1));
                            long endDate = Math.min(invalidationDate, rs.getLong(2));
                            typedCache.adaptRequestCache(beginDate, endDate);
                        }
                    }
                }
            }

            /* for usage in other thread return full copy! */
            return new ArrayList<>(typedCache.getAllRequestIntervals());
        }
    }

    private static long getLastEvent(Connection connection, SWEKSupplier type) throws SQLException {
        int typeId = getEventTypeId(connection, type);
        long last_timestamp = Long.MIN_VALUE;
        if (typeId != -1) {
            PreparedStatement pstatement = getPreparedStatement(connection, SELECT_LAST_EVENT);
            pstatement.setInt(1, typeId);
            try (ResultSet rs = pstatement.executeQuery()) {
                if (rs.next()) {
                    last_timestamp = rs.getLong(1);
                }
            }
        }
        return last_timestamp;
    }

    private static class JsonEvent {
        final int id;
        final byte[] json;
        final SWEKSupplier type;
        final long start;
        final long end;

        JsonEvent(byte[] _json, SWEKSupplier _type, int _id, long _start, long _end) {
            start = _start;
            end = _end;
            type = _type;
            id = _id;
            json = _json;
        }
    }

    public static List<JHVEvent> events2Program(long start, long end, SWEKSupplier type, List<SWEKParam> params) {
        try {
            return executor.invokeAndWait(new Events2Program(start, end, type, params));
        } catch (Exception e) {
            Log.error(e);
        }
        return new ArrayList<>();
    }

    private static class Events2Program implements Callable<List<JHVEvent>> {
        private final SWEKSupplier type;
        private final long start;
        private final long end;
        private final List<SWEKParam> params;

        Events2Program(long _start, long _end, SWEKSupplier _type, List<SWEKParam> _params) {
            type = _type;
            start = _start;
            end = _end;
            params = _params;
        }

        @Override
        public List<JHVEvent> call() throws SQLException {
            Connection connection = EventDatabaseThread.getConnection();
            List<JHVEvent> eventList = new ArrayList<>();
            if (connection == null) {
                return eventList;
            }

            int typeId = getEventTypeId(connection, type);
            if (typeId != -1) {
                String join = "LEFT JOIN " + type.getDatabaseName() + " AS tp ON tp.event_id=e.id";
                StringBuilder and = new StringBuilder();
                for (SWEKParam p : params) {
                    if (!p.param.equals("provider")) {
                        and.append("AND tp.").append(p.param).append(p.operand.representation).append(p.value).append(' ');
                    }
                }
                String sqlt = "SELECT e.id, e.start, e.end, e.data FROM events AS e " + join + " WHERE e.start BETWEEN ? AND ? and e.type_id=? " + and + " order by e.start, e.end ";
                PreparedStatement pstatement = getPreparedStatement(connection, sqlt);
                pstatement.setLong(1, start);
                pstatement.setLong(2, end);
                pstatement.setInt(3, typeId);

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
            }
            return eventList;
        }
    }

    public static List<JHVAssociation> associations2Program(long start, long end, SWEKSupplier type) {
        try {
            return executor.invokeAndWait(new Associations2Program(start, end, type));
        } catch (Exception e) {
            Log.error(e);
        }
        return new ArrayList<>();
    }

    private static class Associations2Program implements Callable<List<JHVAssociation>> {
        private final SWEKSupplier type;
        private final long start;
        private final long end;

        Associations2Program(long _start, long _end, SWEKSupplier _type) {
            type = _type;
            start = _start;
            end = _end;
        }

        @Override
        public List<JHVAssociation> call() throws SQLException {
            Connection connection = EventDatabaseThread.getConnection();
            List<JHVAssociation> assocList = new ArrayList<>();
            if (connection == null) {
                return assocList;
            }

            int typeId = getEventTypeId(connection, type);
            if (typeId != -1) {
                PreparedStatement pstatement = getPreparedStatement(connection, SELECT_ASSOCIATIONS);
                pstatement.setLong(1, start);
                pstatement.setLong(2, end);
                pstatement.setInt(3, typeId);

                try (ResultSet rs = pstatement.executeQuery()) {
                    while (rs.next()) {
                        assocList.add(new JHVAssociation(rs.getInt(1), rs.getInt(2)));
                    }
                }
            }
            return assocList;
        }
    }

    private static ArrayList<JsonEvent> relations2Program(int event_id, SWEKSupplier type_left, SWEKSupplier type_right, String param_left, String param_right) {
        try {
            return executor.invokeAndWait(new Relations2Program(event_id, type_left, type_right, param_left, param_right));
        } catch (Exception e) {
            Log.error(e);
        }
        return new ArrayList<>();
    }

    private static ArrayList<JsonEvent> rel2prog(int event_id, SWEKSupplier type_left, SWEKSupplier type_right, String param_left, String param_right) throws SQLException {
        Connection connection = EventDatabaseThread.getConnection();
        if (connection == null) {
            return new ArrayList<>();
        }

        int type_left_id = getEventTypeId(connection, type_left);
        int type_right_id = getEventTypeId(connection, type_right);

        if (type_left_id != -1 && type_right_id != -1) {
            String table_left_name = type_left.getDatabaseName();
            String table_right_name = type_right.getDatabaseName();

            String sqlt = "SELECT tl.event_id, tr.event_id FROM " + table_left_name + " AS tl," + table_right_name + " AS tr" + " WHERE tl." + param_left + "=tr." + param_right + " AND tl.event_id!=tr.event_id AND (tl.event_id=? OR tr.event_id=?)";
            PreparedStatement pstatement = getPreparedStatement(connection, sqlt);
            pstatement.setLong(1, event_id);
            pstatement.setLong(2, event_id);

            StringBuilder idList = new StringBuilder();

            try (ResultSet rs = pstatement.executeQuery()) {
                boolean next = rs.next();
                while (next) {
                    idList.append(rs.getInt(1)).append(',').append(rs.getInt(2));
                    next = rs.next();
                    if (next) {
                        idList.append(',');
                    }
                }
            }

            String query = "SELECT distinct events.id, events.start, events.end, events.data, event_type.supplier FROM events LEFT JOIN event_type ON events.type_id = event_type.id WHERE events.id IN ( " + idList + ") AND events.id != " + event_id;
            ArrayList<JsonEvent> ret = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(query)) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    long start = rs.getLong(2);
                    long end = rs.getLong(3);
                    byte[] json = rs.getBytes(4);
                    ret.add(new JsonEvent(json, SWEKSupplier.getSupplier(rs.getString(5)), id, start, end));
                }
            }
            return ret;
        }
        return new ArrayList<>();
    }

    private static class Relations2Program implements Callable<ArrayList<JsonEvent>> {
        private final SWEKSupplier type_left;
        private final SWEKSupplier type_right;
        private final String param_left;
        private final String param_right;

        private final int event_id;

        Relations2Program(int _event_id, SWEKSupplier _type_left, SWEKSupplier _type_right, String _param_left, String _param_right) {
            type_left = _type_left;
            type_right = _type_right;
            param_left = _param_left;
            param_right = _param_right;
            event_id = _event_id;
        }

        @Override
        public ArrayList<JsonEvent> call() throws SQLException {
            return rel2prog(event_id, type_left, type_right, param_left, param_right);
        }
    }

    private static class Event2Program implements Callable<JsonEvent> {
        private final int event_id;

        Event2Program(int _event_id) {
            event_id = _event_id;
        }

        @Nullable
        @Override
        public JsonEvent call() throws SQLException {
            Connection connection = EventDatabaseThread.getConnection();
            if (connection == null) {
                return null;
            }

            PreparedStatement ps = getPreparedStatement(connection, SELECT_EVENT_BY_ID);
            ps.setLong(1, event_id);

            JsonEvent je = null;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    long start = rs.getLong(2);
                    long end = rs.getLong(3);
                    byte[] json = rs.getBytes(4);
                    je = new JsonEvent(json, SWEKSupplier.getSupplier(rs.getString(5)), id, start, end);
                }
            }
            return je;
        }
    }

    @Nullable
    private static JsonEvent event2Program(int event_id) {
        try {
            return executor.invokeAndWait(new Event2Program(event_id));
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

}
