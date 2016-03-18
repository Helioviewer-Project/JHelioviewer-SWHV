package org.helioviewer.jhv.data.datatype.event;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.threads.JHVThread.ConnectionThread;

public class JHVDatabase {
    private final static ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(10000);
    private final static ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedDbThreadFactory("JHVDatabase"), new ThreadPoolExecutor.DiscardPolicy());
    private static long ONEWEEK = 1000 * 60 * 60 * 24 * 7;
    private static long ENDOFTIMES = Long.MAX_VALUE;

    private static byte[] compress(final String str) throws IOException {
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

    private static String decompress(final byte[] compressed) {
        String outStr = "";
        try {
            if ((compressed == null) || (compressed.length == 0)) {
                return "";
            }
            if (isCompressed(compressed)) {
                GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    outStr += line;
                }
            } else {
                outStr = new String(compressed);
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
        String sqlt = "SELECT id FROM event_type WHERE name=? AND supplier=?";
        try {
            PreparedStatement pstatement = connection.prepareStatement(sqlt);
            pstatement.setQueryTimeout(30);
            pstatement.setString(1, event.getEventType().getEventName());
            pstatement.setString(2, event.getSupplier().getSupplierName());
            ResultSet rs = pstatement.executeQuery();
            if (!rs.isClosed() && rs.next()) {
                typeId = rs.getInt(1);
                rs.close();
            }
            pstatement.close();
        } catch (SQLException e)
        {
            Log.error("Could not fetch event type " + event.getEventType().getEventName() + event.getSupplier().getSupplierName() + e.getMessage());
        }
        return typeId;
    }

    private static void insertEventTypeIfNotExist(Connection connection, JHVEventType eventType) {
        try {
            String sqlt = "INSERT INTO event_type(name, supplier) VALUES(?,?)";

            PreparedStatement pstatement = connection.prepareStatement(sqlt);
            pstatement.setQueryTimeout(30);
            pstatement.setString(1, eventType.getEventType().getEventName());
            pstatement.setString(2, eventType.getSupplier().getSupplierName());
            pstatement.executeUpdate();
            pstatement.close();
        } catch (SQLException e)
        {
            Log.error("Failed to insert event type " + e.getMessage());
        }

    }

    private static void insertLinkIfNotExist(Connection connection, int left_id, int right_id) {
        try {
            String sqlt = "INSERT INTO event_link(left_id, right_id) VALUES(?,?)";
            PreparedStatement pstatement = connection.prepareStatement(sqlt);
            pstatement.setQueryTimeout(30);
            pstatement.setInt(1, left_id);
            pstatement.setInt(2, right_id);
            pstatement.executeUpdate();
            pstatement.close();
        } catch (SQLException e)
        {
            Log.error("Failed to insert event type " + e.getMessage());
        }
    }

    private static Integer[] insertLinkIfNotExist(Connection connection, String left_uid, String right_uid) {
        Integer[] ids = new Integer[] { getIdFromUID(connection, left_uid), getIdFromUID(connection, right_uid) };

        if (ids[0] != -1 && ids[1] != -1) {
            insertLinkIfNotExist(connection, ids[0], ids[1]);
        }
        else {
            Log.error("Could not add association to database " + ids[0] + " " + ids[1]);
        }
        return ids;
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
        String sqlt = "SELECT id FROM events WHERE uid=?";
        try {
            PreparedStatement pstatement = connection.prepareStatement(sqlt);
            pstatement.setQueryTimeout(30);
            pstatement.setString(1, uid);
            ResultSet rs = pstatement.executeQuery();
            if (!rs.isClosed() && rs.next()) {
                id = rs.getInt(1);
                rs.close();
            }
            pstatement.close();
        } catch (SQLException e)
        {
            Log.error("Could not fetch id from uid " + e.getMessage());
        }
        return id;
    }

    private static void insertVoidEvent(Connection connection, String uid) {
        try
        {
            String sql = "INSERT INTO events(uid) VALUES(?)";
            PreparedStatement pstatement = connection.prepareStatement(sql);
            pstatement.setQueryTimeout(30);
            pstatement.setString(1, uid);
            pstatement.executeUpdate();
            pstatement.close();
        } catch (SQLException e)
        {
            Log.error("Could not insert event" + e.getMessage());
        }
    }

    public static Integer[] dump_association2db(String left, String right) {
        FutureTask<Integer[]> ft = new FutureTask<Integer[]>(new DumpAssociation2Db(left, right));
        executor.execute(ft);

        try {
            return ft.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new Integer[] { -1, -1 };
        } catch (ExecutionException e) {
            e.printStackTrace();
            return new Integer[] { -1, -1 };
        }
    }

    private static class DumpAssociation2Db implements Callable<Integer[]> {
        private final String left;
        private final String right;

        public DumpAssociation2Db(String _left, String _right) {
            left = _left;
            right = _right;
        }

        @Override
        public Integer[] call() {
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return new Integer[] { -1, -1 };
            return insertLinkIfNotExist(connection, left, right);
        }
    }

    public static Integer getEventId(String uid) {
        FutureTask<Integer> ft = new FutureTask<Integer>(new GetEventId(uid));
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

    private static class GetEventId implements Callable<Integer> {

        private final String uid;

        public GetEventId(String _uid) {
            uid = _uid;
        }

        @Override
        public Integer call() {
            int generatedKey = -1;
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return generatedKey;
            try
            {
                String sql = "SELECT id from events WHERE uid=?";
                PreparedStatement pstatement = connection.prepareStatement(sql);
                pstatement.setQueryTimeout(30);
                pstatement.setString(1, uid);
                ResultSet generatedKeys = pstatement.executeQuery();
                if (generatedKeys.next()) {
                    generatedKey = generatedKeys.getInt(1);
                }
                generatedKeys.close();
                pstatement.close();
            } catch (SQLException e)
            {
                Log.error("Could not select event with uid " + uid + e.getMessage());
            }
            return generatedKey;
        }
    }

    public static Integer dump_event2db(String eventStr, JHVEvent event, String uid) {
        FutureTask<Integer> ft = new FutureTask<Integer>(new DumpEvent2Db(eventStr, event, uid));
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
        private final JHVEvent event;
        private final String eventStr;
        private final String uid;

        public DumpEvent2Db(String _eventStr, JHVEvent _event, String _uid) {
            eventStr = _eventStr;
            event = _event;
            uid = _uid;
        }

        @Override
        public Integer call() {
            int generatedKey = -1;
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return generatedKey;
            byte[] compressed_data;
            try {
                compressed_data = compress(eventStr.toString());
            } catch (IOException e1) {
                compressed_data = new byte[0];
            }
            try
            {
                int typeId = getEventTypeId(connection, event.getJHVEventType());
                if (typeId != -1) {
                    String sql = "INSERT INTO events(type_id, uid,  start, end, data) VALUES(?,?,?,?,?)";
                    PreparedStatement pstatement = connection.prepareStatement(sql);
                    pstatement.setQueryTimeout(30);
                    pstatement.setInt(1, typeId);
                    pstatement.setString(2, uid);
                    pstatement.setLong(3, event.getStartDate().getTime());
                    pstatement.setLong(4, event.getEndDate().getTime());
                    pstatement.setBinaryStream(5, new ByteArrayInputStream(compressed_data), compressed_data.length);
                    pstatement.executeUpdate();
                    pstatement.close();
                    Statement statement = connection.createStatement();
                    ResultSet generatedKeys = statement.executeQuery("SELECT last_insert_rowid()");
                    if (generatedKeys.next()) {
                        generatedKey = generatedKeys.getInt(1);
                    }
                    generatedKeys.close();
                }
                else {
                    Log.error("Failed to insert event");
                }
            } catch (SQLException e)
            {
                Log.error("Could not insert event " + e.getMessage());
            }
            return generatedKey;
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
                String sqld = "DELETE FROM date_range where type_id=?";
                PreparedStatement dstatement = connection.prepareStatement(sqld);
                dstatement.setQueryTimeout(30);
                dstatement.setInt(1, typeId);
                dstatement.executeUpdate();
                for (Interval<Date> interval : typedCache.getAllRequestIntervals()) {
                    if (typeId != -1) {
                        String sql = "INSERT INTO date_range(type_id,  start, end) VALUES(?,?,?)";
                        PreparedStatement pstatement = connection.prepareStatement(sql);
                        pstatement.setQueryTimeout(30);
                        pstatement.setQueryTimeout(30);
                        pstatement.setInt(1, typeId);
                        pstatement.setLong(2, interval.getStart().getTime());
                        pstatement.setLong(3, interval.getEnd().getTime());
                        pstatement.executeUpdate();
                        pstatement.close();
                    }
                }
            } catch (SQLException e)
            {
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
            Connection connection = ConnectionThread.getConnection();
            HashMap<JHVEventType, RequestCache> dCache = ConnectionThread.downloadedCache;
            RequestCache typedCache = dCache.get(type);
            if (typedCache == null) {
                typedCache = new RequestCache();
                dCache.put(type, typedCache);
                if (connection != null) {
                    int typeId = getEventTypeId(connection, type);
                    if (typeId != -1) {
                        try {
                            String sqlt = "SELECT start, end FROM date_range where type_id=? order by start, end ";
                            PreparedStatement pstatement = connection.prepareStatement(sqlt);
                            pstatement.setQueryTimeout(30);
                            pstatement.setInt(1, typeId);
                            ResultSet rs = pstatement.executeQuery();
                            while (!rs.isClosed() && rs.next()) {
                                typedCache.adaptRequestCache(new Date(rs.getLong(1)), new Date(rs.getLong(2)));
                            }
                            rs.close();
                            pstatement.close();
                        } catch (SQLException e)
                        {
                            Log.error("Could db2daterange " + e.getMessage());
                        }
                    }
                }
                long lastEvent = getLastEvent(connection, type);
                if (lastEvent != -1)
                    typedCache.removeRequestedIntervals(new Interval<Date>(new Date(lastEvent - ONEWEEK), new Date(ENDOFTIMES)));

            }
            /* for usage in other thread return full copy! */
            ArrayList<Interval<Date>> copy = new ArrayList<Interval<Date>>();
            for (Interval<Date> interval : typedCache.getAllRequestIntervals()) {
                copy.add(new Interval(new Date(interval.getStart().getTime()), new Date(interval.getEnd().getTime())));
            }
            return copy;
        }
    }

    private static long getLastEvent(Connection connection, JHVEventType type) {
        int typeId = getEventTypeId(connection, type);
        long last_timestamp = -1;
        if (typeId != -1) {
            try {
                String sqlt = "SELECT end FROM events WHERE type_id=? order by end DESC LIMIT 1";
                PreparedStatement pstatement = connection.prepareStatement(sqlt);
                pstatement.setQueryTimeout(30);
                pstatement.setInt(1, typeId);
                ResultSet rs = pstatement.executeQuery();
                if (!rs.isClosed() && rs.next()) {
                    last_timestamp = rs.getLong(1);
                }
                rs.close();
                pstatement.close();
            } catch (SQLException e)
            {
                Log.error("Could not fetch id from uid " + e.getMessage());
            }
        }
        return last_timestamp;
    }

    public static InputStream getEvents(long start, long end, JHVEventType type) {
        FutureTask<InputStream> ft = new FutureTask<InputStream>(new GetEvents(start, end, type));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    private static class GetEvents implements Callable<InputStream> {
        private final JHVEventType type;
        private final long start;
        private final long end;

        public GetEvents(long _start, long _end, JHVEventType _type) {
            type = _type;
            start = _start;
            end = _end;
        }

        @Override
        public InputStream call() {
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return null;
            int typeId = getEventTypeId(connection, type);
            StringBuilder events = new StringBuilder();
            events.append("{\"result\":[");
            if (typeId != -1) {
                try {
                    String sqlt = "SELECT data FROM events WHERE start>=? and end <=? and type_id=? order by start, end ";
                    PreparedStatement pstatement = connection.prepareStatement(sqlt);
                    pstatement.setQueryTimeout(30);
                    pstatement.setLong(1, start);
                    pstatement.setLong(2, end);
                    pstatement.setInt(3, typeId);
                    ResultSet rs = pstatement.executeQuery();
                    boolean next = rs.next();

                    while (!rs.isClosed() && next) {
                        events.append(decompress(rs.getBytes(1)));
                        next = rs.next();
                        if (next) {
                            events.append(",");
                        }
                    }
                    rs.close();
                    pstatement.close();
                } catch (SQLException e)
                {
                    Log.error("Could not fetch id from uid " + e.getMessage());
                }
            }
            events.append("]");
            events.append(",\"overmax\":false");
            events.append(",\"association\":[");
            try {
                String sqlt = "SELECT left_events.uid, right_events.uid FROM event_link "
                        + "LEFT JOIN events AS left_events ON left_events.id=event_link.left_id "
                        + "LEFT JOIN events AS right_events ON right_events.id=event_link.left_id "
                        + "WHERE left_events.start>=? and left_events.end <=? and left_events.type_id=? order by left_events.start, left_events.end ";
                PreparedStatement pstatement = connection.prepareStatement(sqlt);
                pstatement.setQueryTimeout(30);
                pstatement.setLong(1, start);
                pstatement.setLong(2, end);
                pstatement.setInt(3, typeId);
                ResultSet rs = pstatement.executeQuery();
                boolean next = rs.next();

                while (!rs.isClosed() && next) {
                    events.append("{\"first_ivorn\":\"");
                    events.append(rs.getString(1));
                    events.append("\",");
                    events.append("\"second_ivorn\":\"");
                    events.append(rs.getString(1));
                    events.append("\"}");
                    next = rs.next();
                    if (next) {
                        events.append(",");
                    }
                }
                rs.close();
                pstatement.close();
            } catch (SQLException e)
            {
                Log.error("Could not fetch id from uid " + e.getMessage());
            }
            events.append("]}");
            return new ByteArrayInputStream(events.toString().getBytes());
        }
    }

}
