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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.Log2;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.interval.RequestCache;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.SWEK;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.threads.SingleExecutor;

public class EventDatabase {

    private static final SingleExecutor executor = new SingleExecutor(new JHVThread.NamedClassThreadFactory(EventDatabaseThread.class, "EventDatabase"));

    public record Event2Db(byte[] compressedJson, long start, long end, long archiv, String uid,
                           List<JHVDatabaseParam> paramList) {
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

    private static final HashMap<String, PreparedStatement> statements = new HashMap<>();
    private static final HashMap<SWEKSupplier, RequestCache> downloadedCache = new HashMap<>();

    private static PreparedStatement getPreparedStatement(String statement) throws SQLException {
        PreparedStatement pstat = statements.get(statement);
        if (pstat == null) {
            pstat = EventDatabaseThread.getConnection().prepareStatement(statement);
            pstat.setQueryTimeout(30);
            statements.put(statement, pstat);
        }
        return pstat;
    }

    private static int getEventTypeId(SWEKSupplier eventType) throws SQLException {
        int typeId = _getEventTypeId(eventType);
        if (typeId == -1) {
            insertEventTypeIfNotExist(eventType);
            typeId = _getEventTypeId(eventType);
        }
        return typeId;
    }

    private static int _getEventTypeId(SWEKSupplier event) throws SQLException {
        int typeId = -1;
        PreparedStatement pstatement = getPreparedStatement(SELECT_EVENT_TYPE);
        pstatement.setString(1, event.getGroup().getName());
        pstatement.setString(2, event.getKey());

        try (ResultSet rs = pstatement.executeQuery()) {
            if (rs.next()) {
                typeId = rs.getInt(1);
            }
        }
        return typeId;
    }

    private static void insertEventTypeIfNotExist(SWEKSupplier eventType) throws SQLException {
        PreparedStatement pstatement = getPreparedStatement(INSERT_EVENT_TYPE);
        pstatement.setString(1, eventType.getGroup().getName());
        pstatement.setString(2, eventType.getKey());
        pstatement.executeUpdate();

        StringBuilder createtbl = new StringBuilder("CREATE TABLE ").append(eventType.getDatabaseName()).append(" (");
        eventType.getGroup().getAllDatabaseFields().forEach((key, value) -> createtbl.append(key).append(' ').append(value).append(" DEFAULT NULL,"));
        createtbl.append("event_id INTEGER, id INTEGER PRIMARY KEY AUTOINCREMENT, FOREIGN KEY(event_id) REFERENCES events(id), UNIQUE(event_id) ON CONFLICT REPLACE )");

        Connection connection = pstatement.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.executeUpdate(createtbl.toString());
        }
        connection.commit();
    }

    private static int getIdFromUID(String uid) throws SQLException {
        int id = _getIdFromUID(uid);
        if (id == -1) {
            insertVoidEvent(uid);
            id = _getIdFromUID(uid);
        }
        return id;
    }

    private static int _getIdFromUID(String uid) throws SQLException {
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

    private static void insertVoidEvent(String uid) throws SQLException {
        PreparedStatement pstatement = getPreparedStatement(INSERT_EVENT);
        pstatement.setString(1, uid);
        pstatement.executeUpdate();
    }

    private static void dump_associationint2db(List<Pair<Integer, Integer>> assocs) throws SQLException {
        int len = assocs.size();
        int i = 0;
        int errorcode = 0;
        PreparedStatement pstatement = getPreparedStatement(INSERT_LINK);
        while (i < len && errorcode == 0) {
            Pair<Integer, Integer> assoc = assocs.get(i);
            int id0 = assoc.left();
            int id1 = assoc.right();

            if (id0 != -1 && id1 != -1 && id0 != id1) {
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
                Log2.error("Could not add association to database");
            }
            i++;
        }
        pstatement.getConnection().commit();
    }

    public static int dump_association2db(Pair<String, String>[] assocs) {
        try {
            return executor.invokeAndWait(new DumpAssociation2Db(assocs));
        } catch (Exception e) {
            Log2.error(e);
        }
        return -1;
    }

    private record DumpAssociation2Db(Pair<String, String>[] assocs) implements Callable<Integer> {
        @Override
        public Integer call() throws SQLException {
            int len = assocs.length;
            int i = 0;
            int errorcode = 0;

            PreparedStatement pstatement = getPreparedStatement(INSERT_LINK);

            while (i < len && errorcode == 0) {
                Pair<String, String> assoc = assocs[i];
                int id0 = getIdFromUID(assoc.left());
                int id1 = getIdFromUID(assoc.right());
                if (id0 != -1 && id1 != -1) {
                    pstatement.setInt(1, id0);
                    pstatement.setInt(2, id1);
                    pstatement.executeUpdate();
                } else {
                    errorcode = -1;
                    Log2.error("Could not add association to database");
                }
                i++;
            }
            pstatement.getConnection().commit();
            return errorcode;
        }
    }

    private static int getEventId(String uid) throws SQLException {
        int generatedKey = -1;
        PreparedStatement pstatement = getPreparedStatement(SELECT_EVENT_ID_FROM_UID);
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

    public static int[] dump_event2db(List<Event2Db> event2db_list, SWEKSupplier type) {
        try {
            return executor.invokeAndWait(new DumpEvent2Db(event2db_list, type));
        } catch (Exception e) {
            Log2.error(e);
        }
        return get_id_init_list(event2db_list.size());
    }

    private record DumpEvent2Db(List<Event2Db> event2db_list, SWEKSupplier type) implements Callable<int[]> {
        @Override
        public int[] call() throws SQLException {
            int[] inserted_ids = get_id_init_list(event2db_list.size());
            int typeId = getEventTypeId(type);
            int llen = event2db_list.size();

            PreparedStatement insertFullEvent = getPreparedStatement(INSERT_FULL_EVENT);
            PreparedStatement selectLastInsert = getPreparedStatement(SELECT_LAST_INSERT);
            PreparedStatement updateEvent = getPreparedStatement(UPDATE_EVENT);

            for (int i = 0; i < llen; i++) {
                Event2Db event2db = event2db_list.get(i);
                int generatedKey = -1;
                if (typeId != -1) {
                    generatedKey = getEventId(event2db.uid);

                    if (generatedKey == -1) {
                        insertFullEvent.setInt(1, typeId);
                        insertFullEvent.setString(2, event2db.uid);
                        insertFullEvent.setLong(3, event2db.start);
                        insertFullEvent.setLong(4, event2db.end);
                        insertFullEvent.setLong(5, event2db.archiv);
                        insertFullEvent.setBinaryStream(6, new ByteArrayInputStream(event2db.compressedJson), event2db.compressedJson.length);
                        insertFullEvent.executeUpdate();

                        try (ResultSet rs = selectLastInsert.executeQuery()) {
                            if (rs.next()) {
                                generatedKey = rs.getInt(1);
                            }
                        }
                    } else {
                        updateEvent.setInt(1, typeId);
                        updateEvent.setString(2, event2db.uid);
                        updateEvent.setLong(3, event2db.start);
                        updateEvent.setLong(4, event2db.end);
                        updateEvent.setBinaryStream(5, new ByteArrayInputStream(event2db.compressedJson), event2db.compressedJson.length);
                        updateEvent.setInt(6, generatedKey);
                        updateEvent.executeUpdate();
                    }
                    {
                        StringBuilder fieldString = new StringBuilder();
                        StringBuilder varString = new StringBuilder();
                        for (JHVDatabaseParam p : event2db.paramList) {
                            fieldString.append(',').append(p.getParamName());
                            varString.append(",?");
                        }
                        String full_statement = "INSERT INTO " + type.getDatabaseName() + "(event_id" + fieldString + ") VALUES(?" + varString + ')';
                        PreparedStatement pstatement = getPreparedStatement(full_statement);
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
                    Log2.error("Failed to insert event");
                }
                inserted_ids[i] = generatedKey;
            }
            insertFullEvent.getConnection().commit();

            ArrayList<Pair<Integer, Integer>> assocs = new ArrayList<>();
            for (int id : inserted_ids) {
                if (id == -1) {
                    Log2.error("Failed to dump to database");
                    assocs.add(new Pair<>(1, 1));
                } else {
                    List<JHVEvent> rels = _getOtherRelations(id, type, true, false, true);
                    rels.forEach(rel -> assocs.add(new Pair<>(id, rel.getUniqueID())));
                }
            }
            dump_associationint2db(assocs);

            return inserted_ids;
        }
    }

    private static JHVEvent parseJSON(JsonEvent jsonEvent, boolean full) throws Exception {
        try (InputStream bais = new ByteArrayInputStream(jsonEvent.json); InputStream is = new GZIPInputStream(bais)) {
            return jsonEvent.type.getSource().handler().parseEventJSON(JSONUtils.get(is), jsonEvent.type, jsonEvent.id, jsonEvent.start, jsonEvent.end, full);
        }
    }

    private static List<JHVEvent> createUniqueList(List<JHVEvent> events) {
        HashSet<Integer> ids = new HashSet<>();
        List<JHVEvent> uniqueEvents = new ArrayList<>();
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

    // Given an event id and its type, return all related events. If similartype is true, return only related events having the same type.
    private static List<JHVEvent> _getOtherRelations(int id, SWEKSupplier jhvEventType, boolean similartype, boolean full, boolean is_dbthread) throws SQLException {
        SWEKGroup group = jhvEventType.getGroup();
        List<JHVEvent> nEvents = new ArrayList<>();
        List<JsonEvent> jsonEvents = new ArrayList<>();

        for (SWEK.RelatedEvents re : SWEKGroup.getSWEKRelatedEvents()) {
            if (re.group() == group) {
                for (SWEK.RelatedOn swon : re.relatedOnList()) {
                    String f = swon.parameterFrom().name().toLowerCase();
                    String w = swon.parameterWith().name().toLowerCase();

                    SWEKGroup reType = re.relatedWith();
                    for (Enumeration<TreeNode> e = reType.children(); e.hasMoreElements(); ) {
                        SWEKSupplier supplier = (SWEKSupplier) e.nextElement();
                        if (similartype == (supplier == jhvEventType)) {
                            jsonEvents.addAll(is_dbthread
                                    ? rel2prog(id, jhvEventType, supplier, f, w)
                                    : relations2Program(id, jhvEventType, supplier, f, w));
                        }
                    }
                }
            }

            if (re.relatedWith() == group) {
                for (SWEK.RelatedOn swon : re.relatedOnList()) {
                    String f = swon.parameterFrom().name().toLowerCase();
                    String w = swon.parameterWith().name().toLowerCase();

                    SWEKGroup reType = re.group();
                    for (Enumeration<TreeNode> e = reType.children(); e.hasMoreElements(); ) {
                        SWEKSupplier supplier = (SWEKSupplier) e.nextElement();
                        if (similartype == (supplier == jhvEventType)) {
                            jsonEvents.addAll(is_dbthread
                                    ? rel2prog(id, supplier, jhvEventType, f, w)
                                    : relations2Program(id, supplier, jhvEventType, f, w));
                        }
                    }
                }
            }

            for (JsonEvent jsonEvent : jsonEvents) {
                try {
                    nEvents.add(parseJSON(jsonEvent, full));
                } catch (Exception e) {
                    Log2.error(e);
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
                Log2.error(e);
            }
        }

        return createUniqueList(nEvents);
    }

    public static void addDaterange2db(long start, long end, SWEKSupplier type) {
        executor.invokeLater(new AddDateRange2db(start, end, type));
    }

    private record AddDateRange2db(long start, long end, SWEKSupplier type) implements Runnable {
        @Override
        public void run() {
            RequestCache typedCache = downloadedCache.get(type);
            if (typedCache == null) {
                return;
            }

            typedCache.adaptRequestCache(start, end);
            try {
                int typeId = getEventTypeId(type);
                PreparedStatement dstatement = getPreparedStatement(DELETE_DATERANGE);
                dstatement.setInt(1, typeId);
                dstatement.executeUpdate();
                for (Interval interval : typedCache.getAllRequestIntervals()) {
                    if (typeId != -1) {
                        PreparedStatement pstatement = getPreparedStatement(INSERT_DATERANGE);
                        pstatement.setInt(1, typeId);
                        pstatement.setLong(2, interval.start);
                        pstatement.setLong(3, interval.end);
                        pstatement.executeUpdate();
                    }
                }
                dstatement.getConnection().commit();
            } catch (SQLException e) {
                Log2.error("Could not serialize date_range to database", e);
            }
        }
    }

    public static List<Interval> db2daterange(SWEKSupplier type) {
        try {
            return executor.invokeAndWait(new Db2DateRange(type));
        } catch (Exception e) {
            Log2.error(e);
        }
        return new ArrayList<>();
    }

    private record Db2DateRange(SWEKSupplier type) implements Callable<List<Interval>> {
        @Override
        public List<Interval> call() throws SQLException {
            RequestCache typedCache = downloadedCache.get(type);
            if (typedCache == null) {
                typedCache = new RequestCache();
                long last_timestamp = getLastEvent(type);
                long lastEvent = last_timestamp == Long.MIN_VALUE ? Long.MAX_VALUE : Math.min(System.currentTimeMillis(), last_timestamp);
                long invalidationDate = lastEvent - ONEWEEK * 2;
                downloadedCache.put(type, typedCache);

                int typeId = getEventTypeId(type);
                if (typeId != -1) {
                    PreparedStatement pstatement = getPreparedStatement(SELECT_DATERANGE);
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

    private static long getLastEvent(SWEKSupplier type) throws SQLException {
        int typeId = getEventTypeId(type);
        long last_timestamp = Long.MIN_VALUE;
        if (typeId != -1) {
            PreparedStatement pstatement = getPreparedStatement(SELECT_LAST_EVENT);
            pstatement.setInt(1, typeId);
            try (ResultSet rs = pstatement.executeQuery()) {
                if (rs.next()) {
                    last_timestamp = rs.getLong(1);
                }
            }
        }
        return last_timestamp;
    }

    private record JsonEvent(byte[] json, SWEKSupplier type, int id, long start, long end) {
    }

    public static List<JHVEvent> events2Program(long start, long end, SWEKSupplier type, List<SWEK.Param> params) {
        try {
            return executor.invokeAndWait(new Events2Program(start, end, type, params));
        } catch (Exception e) {
            Log2.error(e);
        }
        return new ArrayList<>();
    }

    private record Events2Program(long start, long end, SWEKSupplier type, List<SWEK.Param> params)
            implements Callable<List<JHVEvent>> {
        @Override
        public List<JHVEvent> call() throws SQLException {
            List<JHVEvent> eventList = new ArrayList<>();
            int typeId = getEventTypeId(type);
            if (typeId != -1) {
                String join = "LEFT JOIN " + type.getDatabaseName() + " AS tp ON tp.event_id=e.id";
                StringBuilder and = new StringBuilder();
                for (SWEK.Param p : params) {
                    if (!p.name().equals("provider")) {
                        and.append("AND tp.").append(p.name()).append(p.operand().representation).append(p.value()).append(' ');
                    }
                }
                String sqlt = "SELECT e.id, e.start, e.end, e.data FROM events AS e " + join + " WHERE e.start BETWEEN ? AND ? and e.type_id=? " + and + " order by e.start, e.end ";
                PreparedStatement pstatement = getPreparedStatement(sqlt);
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
                            Log2.error(e);
                        }
                    }
                }
            }
            return eventList;
        }
    }

    public static List<Pair<Integer, Integer>> associations2Program(long start, long end, SWEKSupplier type) {
        try {
            return executor.invokeAndWait(new Associations2Program(start, end, type));
        } catch (Exception e) {
            Log2.error(e);
        }
        return new ArrayList<>();
    }

    private record Associations2Program(long start, long end, SWEKSupplier type)
            implements Callable<List<Pair<Integer, Integer>>> {
        @Override
        public List<Pair<Integer, Integer>> call() throws SQLException {
            List<Pair<Integer, Integer>> assocList = new ArrayList<>();
            int typeId = getEventTypeId(type);
            if (typeId != -1) {
                PreparedStatement pstatement = getPreparedStatement(SELECT_ASSOCIATIONS);
                pstatement.setLong(1, start);
                pstatement.setLong(2, end);
                pstatement.setInt(3, typeId);

                try (ResultSet rs = pstatement.executeQuery()) {
                    while (rs.next()) {
                        assocList.add(new Pair<>(rs.getInt(1), rs.getInt(2)));
                    }
                }
            }
            return assocList;
        }
    }

    private static List<JsonEvent> relations2Program(int event_id, SWEKSupplier type_left, SWEKSupplier type_right, String param_left, String param_right) {
        try {
            return executor.invokeAndWait(new Relations2Program(event_id, type_left, type_right, param_left, param_right));
        } catch (Exception e) {
            Log2.error(e);
        }
        return new ArrayList<>();
    }

    private static List<JsonEvent> rel2prog(int event_id, SWEKSupplier type_left, SWEKSupplier type_right, String param_left, String param_right) throws SQLException {
        int type_left_id = getEventTypeId(type_left);
        int type_right_id = getEventTypeId(type_right);

        if (type_left_id != -1 && type_right_id != -1) {
            String table_left_name = type_left.getDatabaseName();
            String table_right_name = type_right.getDatabaseName();

            String sqlt = "SELECT tl.event_id, tr.event_id FROM " + table_left_name + " AS tl," + table_right_name + " AS tr" + " WHERE tl." + param_left + "=tr." + param_right + " AND tl.event_id!=tr.event_id AND (tl.event_id=? OR tr.event_id=?)";
            PreparedStatement pstatement = getPreparedStatement(sqlt);
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
            List<JsonEvent> ret = new ArrayList<>();
            try (Statement statement = pstatement.getConnection().createStatement();
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

    private record Relations2Program(int event_id, SWEKSupplier type_left, SWEKSupplier type_right, String param_left,
                                     String param_right) implements Callable<List<JsonEvent>> {
        @Override
        public List<JsonEvent> call() throws SQLException {
            return rel2prog(event_id, type_left, type_right, param_left, param_right);
        }
    }

    private record Event2Program(int event_id) implements Callable<JsonEvent> {
        @Nullable
        @Override
        public JsonEvent call() throws SQLException {
            PreparedStatement ps = getPreparedStatement(SELECT_EVENT_BY_ID);
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
            Log2.error(e);
        }
        return null;
    }

}
