package org.helioviewer.jhv.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.threads.JHVThread.ConnectionThread;

public class JHVDatabase {

    public static class Event2Db {
        byte[] compressedJson;
        long start;
        long end;
        String uid;
        ArrayList<JHVDatabaseParam> paramList;

        public Event2Db(byte[] _compressedJson, long _start, long _end, String _uid, ArrayList<JHVDatabaseParam> _paramList) {
            compressedJson = _compressedJson;
            start = _start;
            end = _end;
            uid = _uid;
            paramList = _paramList;
        }

    }

    private final static ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(10000);
    private final static ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedDbThreadFactory("JHVDatabase"), new ThreadPoolExecutor.DiscardPolicy());

    private static long ONEWEEK = 1000 * 60 * 60 * 24 * 7;
    public static int config_hash;

    private static final String INSERT_EVENT = "INSERT INTO events(uid) VALUES(?)";
    private static final String INSERT_FULL_EVENT = "INSERT INTO events(type_id, uid,  start, end, data) VALUES(?,?,?,?,?)";
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
    private static final String SELECT_ASSOCIATIONS = "SELECT left_events.id, right_events.id FROM event_link "
            + "LEFT JOIN events AS left_events ON left_events.id=event_link.left_id "
            + "LEFT JOIN events AS right_events ON right_events.id=event_link.right_id "
            + "WHERE left_events.start BETWEEN ? AND ? and left_events.type_id=? order by left_events.start, left_events.end ";

    private static HashMap<Object, PreparedStatement> statements = new HashMap<Object, PreparedStatement>();

    private static PreparedStatement getPreparedStatement(Connection connection, String statement) {
        statement = statement.intern();
        PreparedStatement pstat = statements.get(statement);
        if (pstat == null) {
            try {
                pstat = connection.prepareStatement(statement);
                pstat.setQueryTimeout(30);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            statements.put(statement, pstat);
        }
        return pstat;
    }

    public static byte[] compress(final String str) throws IOException {
        if ((str == null) || (str.length() == 0)) {
            return null;
        }
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return obj.toByteArray();
    }

    private static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    public static String decompress(final byte[] compressed) {
        String outStr = "";
        try {
            if ((compressed == null) || (compressed.length == 0)) {
                return "";
            }
            if (isCompressed(compressed)) {
                GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int length;
                while ((length = gis.read(buffer)) != -1) {
                    baos.write(buffer, 0, length);
                }
                outStr = baos.toString("UTF-8");
            } else {
                outStr = new String(compressed, "UTF-8");
            }
        } catch (IOException e) {
            System.out.println("could not decompress");
        }
        return outStr;
    }

    private static int getEventTypeId(Connection connection, JHVEventType eventType) {
        int typeId = _getEventTypeId(connection, eventType);
        if (typeId == -1) {
            insertEventTypeIfNotExist(connection, eventType);
            typeId = _getEventTypeId(connection, eventType);
        }
        return typeId;
    }

    private static int _getEventTypeId(Connection connection, JHVEventType event) {
        int typeId = -1;
        try {
            PreparedStatement pstatement = getPreparedStatement(connection, SELECT_EVENT_TYPE);
            pstatement.setString(1, event.getEventType().getEventName());
            pstatement.setString(2, event.getSupplier().getSupplierName());
            ResultSet rs = pstatement.executeQuery();
            if (rs.next()) {
                typeId = rs.getInt(1);
            }
            rs.close();
        } catch (SQLException e) {
            Log.error("Could not fetch event type " + event.getEventType().getEventName() + event.getSupplier().getSupplierName() + e.getMessage());
        }
        return typeId;
    }

    private static void insertEventTypeIfNotExist(Connection connection, JHVEventType eventType) {
        try {
            PreparedStatement pstatement = getPreparedStatement(connection, INSERT_EVENT_TYPE);
            pstatement.setString(1, eventType.getEventType().getEventName());
            pstatement.setString(2, eventType.getSupplier().getSupplierName());
            pstatement.executeUpdate();

            String dbName = eventType.getSupplier().getDatabaseName();
            String createtbl = "CREATE TABLE " + dbName + " (";
            HashMap<String, String> fields = eventType.getEventType().getAllDatabaseFields();

            for (Map.Entry<String, String> entry : fields.entrySet()) {
                createtbl += entry.getKey() + " " + entry.getValue() + " DEFAULT NULL,";
            }
            createtbl += "event_id INTEGER, id INTEGER PRIMARY KEY AUTOINCREMENT, FOREIGN KEY(event_id) REFERENCES events(id), UNIQUE(event_id) ON CONFLICT REPLACE );";
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
            statement.executeUpdate(createtbl);
            connection.commit();
        } catch (SQLException e) {
            Log.error("Failed to insert event type " + e.getMessage());
        }
    }

    private static int getIdFromUID(Connection connection, String uid) {
        int id = _getIdFromUID(connection, uid);
        if (id == -1) {
            insertVoidEvent(connection, uid);
            id = _getIdFromUID(connection, uid);
        }
        return id;
    }

    private static int _getIdFromUID(Connection connection, String uid) {
        int id = -1;
        try {
            PreparedStatement pstatement = getPreparedStatement(connection, SELECT_EVENT_ID_FROM_UID);
            pstatement.setString(1, uid);
            ResultSet rs = pstatement.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1);
            }
            rs.close();
        } catch (SQLException e) {
            Log.error("Could not fetch id from uid " + e.getMessage());
        }
        return id;
    }

    private static void insertVoidEvent(Connection connection, String uid) {
        try {
            PreparedStatement pstatement = getPreparedStatement(connection, INSERT_EVENT);
            pstatement.setString(1, uid);
            pstatement.executeUpdate();
        } catch (SQLException e) {
            Log.error("Could not insert event" + e.getMessage());
        }
    }

    public static Integer dump_association2db(Pair<String, String>[] assocs) {
        FutureTask<Integer> ft = new FutureTask<Integer>(new DumpAssociation2Db(assocs));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static class DumpAssociation2Db implements Callable<Integer> {
        private final Pair<String, String>[] assocs;

        public DumpAssociation2Db(Pair<String, String>[] _assocs) {
            assocs = _assocs;
        }

        @Override
        public Integer call() {
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return -1;
            int len = assocs.length;
            int i = 0;
            int errorcode = 0;
            while (i < len && errorcode == 0) {
                Pair<String, String> assoc = assocs[i];
                Integer[] ids = new Integer[] { getIdFromUID(connection, assoc.a), getIdFromUID(connection, assoc.b) };

                if (ids[0] != -1 && ids[1] != -1) {
                    try {
                        PreparedStatement pstatement = getPreparedStatement(connection, INSERT_LINK);
                        pstatement.setInt(1, ids[0]);
                        pstatement.setInt(2, ids[1]);
                        pstatement.executeUpdate();
                    } catch (SQLException e) {
                        Log.error("Failed to insert event type " + e.getMessage());
                        errorcode = -1;
                    }
                } else {
                    errorcode = -1;
                    Log.error("Could not add association to database ");
                }
                i++;
            }
            try {
                connection.commit();
            } catch (SQLException e1) {
                Log.error("Could not reset autocommit");
                errorcode = -1;
            }
            return errorcode;
        }
    }

    private static int getEventId(String uid) {
        int generatedKey = -1;
        Connection connection = ConnectionThread.getConnection();
        if (connection == null)
            return generatedKey;

        try {
            PreparedStatement pstatement = getPreparedStatement(connection, SELECT_EVENT_ID_FROM_UID);
            pstatement.setString(1, uid);
            ResultSet generatedKeys = pstatement.executeQuery();
            if (generatedKeys.next()) {
                generatedKey = generatedKeys.getInt(1);
            }
            generatedKeys.close();
        } catch (SQLException e) {
            Log.error("Could not select event with uid " + uid + e.getMessage());
        }
        return generatedKey;
    }

    public static Integer dump_event2db(ArrayList<Event2Db> event2db_list, JHVEventType type) {
        FutureTask<Integer> ft = new FutureTask<Integer>(new DumpEvent2Db(event2db_list, type));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static class DumpEvent2Db implements Callable<Integer> {
        private final JHVEventType type;
        private final ArrayList<Event2Db> event2db_list;

        public DumpEvent2Db(ArrayList<Event2Db> _event2db_list, JHVEventType _type) {
            event2db_list = _event2db_list;
            type = _type;
        }

        @Override
        public Integer call() {
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return -1;
            int errorcode = 0;
            try
            {
                int typeId = getEventTypeId(connection, type);
                for (Event2Db event2db : event2db_list) {
                    if (typeId != -1) {
                        int generatedKey = getEventId(event2db.uid);

                        if (generatedKey == -1) {
                            {
                                PreparedStatement pstatement = getPreparedStatement(connection, INSERT_FULL_EVENT);
                                pstatement.setInt(1, typeId);
                                pstatement.setString(2, event2db.uid);
                                pstatement.setLong(3, event2db.start);
                                pstatement.setLong(4, event2db.end);
                                pstatement.setBinaryStream(5, new ByteArrayInputStream(event2db.compressedJson), event2db.compressedJson.length);
                                pstatement.executeUpdate();
                            }
                            {
                                PreparedStatement pstatement = getPreparedStatement(connection, SELECT_LAST_INSERT);
                                ResultSet generatedKeys = pstatement.executeQuery();
                                if (generatedKeys.next()) {
                                    generatedKey = generatedKeys.getInt(1);
                                }
                                generatedKeys.close();
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
                            String fieldString = "";
                            String varString = "";
                            for (JHVDatabaseParam p : event2db.paramList) {
                                fieldString += "," + p.getParamName();
                                varString += ",?";
                            }
                            String full_statement = "INSERT INTO " + type.getSupplier().getDatabaseName() + "(event_id" + fieldString + ") VALUES(?" + varString + ")";

                            PreparedStatement pstatement = getPreparedStatement(connection, full_statement);
                            pstatement.setInt(1, generatedKey);
                            int index = 2;
                            for (JHVDatabaseParam p : event2db.paramList) {
                                if (p.isInt()) {
                                    pstatement.setInt(index, p.getIntValue());
                                } else if (p.isString()) {
                                    pstatement.setString(index, p.getStringValue());
                                }
                                index++;
                            }
                            pstatement.executeUpdate();
                        }
                    } else {
                        Log.error("Failed to insert event");
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                Log.error("Could not insert event " + e.getMessage());
                errorcode = -1;
            }
            return errorcode;
        }
    }

    public static void addDaterange2db(Date start, Date end, JHVEventType type) {
        executor.execute(new AddDateRange2db(start, end, type));
    }

    private static class AddDateRange2db implements Runnable {
        private final JHVEventType type;
        private final Date start;
        private final Date end;

        public AddDateRange2db(Date _start, Date _end, JHVEventType _type) {
            start = _start;
            end = _end;
            type = _type;
        }

        @Override
        public void run() {
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return;

            HashMap<JHVEventType, RequestCache> dCache = ConnectionThread.downloadedCache;
            RequestCache typedCache = dCache.get(type);
            if (typedCache == null)
                return;
            typedCache.adaptRequestCache(start, end);
            int typeId = getEventTypeId(connection, type);
            try {
                PreparedStatement dstatement = getPreparedStatement(connection, DELETE_DATERANGE);
                dstatement.setInt(1, typeId);
                dstatement.executeUpdate();
                for (Interval<Date> interval : typedCache.getAllRequestIntervals()) {
                    if (typeId != -1) {
                        PreparedStatement pstatement = getPreparedStatement(connection, INSERT_DATERANGE);
                        pstatement.setInt(1, typeId);
                        pstatement.setLong(2, interval.start.getTime());
                        pstatement.setLong(3, interval.end.getTime());
                        pstatement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                Log.error("Could not serialize date_range to database " + e.getMessage());
            }
        }

    }

    public static ArrayList<Interval<Date>> db2daterange(JHVEventType type) {
        FutureTask<ArrayList<Interval<Date>>> ft = new FutureTask<ArrayList<Interval<Date>>>(new Db2DateRange(type));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            return new ArrayList<Interval<Date>>();
        } catch (ExecutionException e) {
            return new ArrayList<Interval<Date>>();
        }
    }

    private static class Db2DateRange implements Callable<ArrayList<Interval<Date>>> {

        private final JHVEventType type;

        public Db2DateRange(JHVEventType _type) {
            type = _type;
        }

        @Override
        public ArrayList<Interval<Date>> call() {
            /* for usage in other thread return full copy! */
            ArrayList<Interval<Date>> copy = new ArrayList<Interval<Date>>();
            Connection connection = ConnectionThread.getConnection();
            if (connection == null) {
                return copy;
            }

            HashMap<JHVEventType, RequestCache> dCache = ConnectionThread.downloadedCache;
            RequestCache typedCache = dCache.get(type);
            if (typedCache == null) {
                typedCache = new RequestCache();
                long last_timestamp = getLastEvent(connection, type);
                long lastEvent;
                if (last_timestamp != Long.MIN_VALUE) {
                    lastEvent = Math.min(System.currentTimeMillis(), last_timestamp);
                } else {
                    lastEvent = Long.MAX_VALUE;
                }
                long invalidationDate = lastEvent - ONEWEEK * 2;
                dCache.put(type, typedCache);
                int typeId = getEventTypeId(connection, type);
                if (typeId != -1) {
                    try {
                        PreparedStatement pstatement = getPreparedStatement(connection, SELECT_DATERANGE);
                        pstatement.setInt(1, typeId);
                        ResultSet rs = pstatement.executeQuery();
                        while (rs.next()) {
                            Date beginDate = new Date(Math.min(invalidationDate, rs.getLong(1)));
                            Date endDate = new Date(Math.min(invalidationDate, rs.getLong(2)));
                            typedCache.adaptRequestCache(beginDate, endDate);
                        }
                        rs.close();
                    } catch (SQLException e) {
                        Log.error("Could db2daterange " + e.getMessage());
                    }
                }
            }

            for (Interval<Date> interval : typedCache.getAllRequestIntervals()) {
                copy.add(new Interval(new Date(interval.start.getTime()), new Date(interval.end.getTime())));
            }
            return copy;
        }
    }

    private static long getLastEvent(Connection connection, JHVEventType type) {
        int typeId = getEventTypeId(connection, type);
        long last_timestamp = Long.MIN_VALUE;
        if (typeId != -1) {
            try {
                PreparedStatement pstatement = getPreparedStatement(connection, SELECT_LAST_EVENT);
                pstatement.setInt(1, typeId);
                ResultSet rs = pstatement.executeQuery();
                if (rs.next()) {
                    last_timestamp = rs.getLong(1);
                }
                rs.close();
            } catch (SQLException e) {
                Log.error("Could not fetch id from uid " + e.getMessage());
            }
        }
        return last_timestamp;
    }

    public static ArrayList<JsonEvent> events2Program(long start, long end, JHVEventType type, List<SWEKParam> params) {
        FutureTask<ArrayList<JsonEvent>> ft = new FutureTask<ArrayList<JsonEvent>>(new Events2Program(start, end, type, params));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            System.out.println(e);
            return new ArrayList<JsonEvent>();
        } catch (ExecutionException e) {
            System.out.println(e);
            return new ArrayList<JsonEvent>();
        }
    }

    public static class JsonEvent {

        public int id;
        public byte[] json;
        public JHVEventType type;
        public long start;
        public long end;

        public JsonEvent(byte[] _json, JHVEventType _type, int _id, long _start, long _end) {
            start = _start;
            end = _end;
            type = _type;
            id = _id;
            json = _json;
        }

    }

    private static class Events2Program implements Callable<ArrayList<JsonEvent>> {
        private final JHVEventType type;
        private final long start;
        private final long end;
        private final List<SWEKParam> params;

        public Events2Program(long _start, long _end, JHVEventType _type, List<SWEKParam> _params) {
            type = _type;
            start = _start;
            end = _end;
            params = _params;
        }

        @Override
        public ArrayList<JsonEvent> call() {
            Connection connection = ConnectionThread.getConnection();
            ArrayList<JsonEvent> eventList = new ArrayList<JsonEvent>();
            if (connection == null)
                return eventList;

            int typeId = getEventTypeId(connection, type);
            if (typeId != -1) {
                try {
                    String join = "LEFT JOIN " + type.getSupplier().getDatabaseName() + " AS tp ON tp.event_id=e.id";
                    String and = "";
                    for (SWEKParam p : params) {
                        if (!p.param.equals("provider")) {
                            and += "AND tp." + p.param + p.operand.getStringRepresentation() + p.value + " ";
                        }
                    }
                    String sqlt = "SELECT e.id, e.start, e.end, e.data FROM events AS e "
                            + join
                            + " WHERE e.start BETWEEN ? AND ? and e.type_id=? "
                            + and + " order by e.start, e.end ";
                    PreparedStatement pstatement = getPreparedStatement(connection, sqlt);
                    pstatement.setLong(1, start);
                    pstatement.setLong(2, end);
                    pstatement.setInt(3, typeId);
                    ResultSet rs = pstatement.executeQuery();
                    boolean next = rs.next();
                    while (next) {
                        int id = rs.getInt(1);
                        long start = rs.getLong(2);
                        long end = rs.getLong(3);
                        byte[] json = rs.getBytes(4);
                        eventList.add(new JsonEvent(json, type, id, start, end));
                        next = rs.next();
                    }
                    rs.close();
                } catch (SQLException e) {
                    Log.error("Could not fetch events " + e.getMessage());
                    return eventList;
                }
            }
            return eventList;
        }
    }

    public static ArrayList<JHVAssociation> associations2Program(long start, long end, JHVEventType type) {
        FutureTask<ArrayList<JHVAssociation>> ft = new FutureTask<ArrayList<JHVAssociation>>(new Associations2Program(start, end, type));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            return new ArrayList<JHVAssociation>();
        } catch (ExecutionException e) {
            return new ArrayList<JHVAssociation>();
        }
    }

    private static class Associations2Program implements Callable<ArrayList<JHVAssociation>> {
        private final JHVEventType type;
        private final long start;
        private final long end;

        public Associations2Program(long _start, long _end, JHVEventType _type) {
            type = _type;
            start = _start;
            end = _end;
        }

        @Override
        public ArrayList<JHVAssociation> call() {
            Connection connection = ConnectionThread.getConnection();
            ArrayList<JHVAssociation> assocList = new ArrayList<JHVAssociation>();
            if (connection == null)
                return assocList;

            int typeId = getEventTypeId(connection, type);
            if (typeId != -1) {
                try {
                    PreparedStatement pstatement = getPreparedStatement(connection, SELECT_ASSOCIATIONS);
                    pstatement.setLong(1, start);
                    pstatement.setLong(2, end);
                    pstatement.setInt(3, typeId);
                    ResultSet rs = pstatement.executeQuery();
                    boolean next = rs.next();
                    while (next) {
                        int left = rs.getInt(1);
                        int right = rs.getInt(2);
                        assocList.add(new JHVAssociation(left, right));
                        next = rs.next();
                    }
                    rs.close();
                } catch (SQLException e) {
                    Log.error("Could not fetch associations " + e.getMessage());
                    return assocList;
                }
            }
            return assocList;
        }
    }

    public static ArrayList<JHVAssociation> relations2Program(int event_id, JHVEventType type_left, JHVEventType type_right, String param_left, String param_right) {
        FutureTask<ArrayList<JHVAssociation>> ft = new FutureTask<ArrayList<JHVAssociation>>(new Relations2Program(event_id, type_left, type_right, param_left, param_right));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            return new ArrayList<JHVAssociation>();
        } catch (ExecutionException e) {
            return new ArrayList<JHVAssociation>();
        }
    }

    private static class Relations2Program implements Callable<ArrayList<JHVAssociation>> {
        private final JHVEventType type_left;
        private final JHVEventType type_right;
        private final String param_left;
        private final String param_right;

        private final int event_id;

        public Relations2Program(int _event_id, JHVEventType _type_left, JHVEventType _type_right, String _param_left, String _param_right) {
            type_left = _type_left;
            type_right = _type_right;
            param_left = _param_left;
            param_right = _param_right;
            event_id = _event_id;
        }

        @Override
        public ArrayList<JHVAssociation> call() {
            Connection connection = ConnectionThread.getConnection();
            ArrayList<JHVAssociation> assocList = new ArrayList<JHVAssociation>();
            if (connection == null)
                return assocList;

            int type_left_id = getEventTypeId(connection, type_left);
            int type_right_id = getEventTypeId(connection, type_right);

            if (type_left_id != -1 && type_right_id != -1) {
                try {
                    String table_left_name = type_left.getSupplier().getDatabaseName();
                    String table_right_name = type_right.getSupplier().getDatabaseName();

                    String sqlt = "SELECT tl.event_id, tr.event_id FROM "
                            + table_left_name + " AS tl," + table_right_name + " AS tr"
                            + " WHERE tl." + param_left + "=tr." + param_right
                            + " AND tl.event_id!=tr.event_id"
                            + " tl.event_id=? OR tl.right_id=?";
                    PreparedStatement pstatement = getPreparedStatement(connection, sqlt);
                    pstatement.setLong(1, event_id);
                    pstatement.setLong(2, event_id);
                    ResultSet rs = pstatement.executeQuery();
                    boolean next = rs.next();
                    while (next) {
                        int left = rs.getInt(1);
                        int right = rs.getInt(2);
                        assocList.add(new JHVAssociation(left, right));
                        next = rs.next();
                    }
                    rs.close();
                } catch (SQLException e) {
                    Log.error("Could not fetch associations " + e.getMessage());
                    return assocList;
                }
            }
            return assocList;
        }
    }
}
