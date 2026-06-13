package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.helioviewer.jhv.app.Log;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.votable.VOTableBuilder;

class SoarTable {

    static {
        Log.setLoggerLevel("uk.ac.starlink", Level.WARNING); // shut-up info logs
    }

    public static List<SoarClient.DataItem> get(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri);
             StarTable table = new StarTableFactory().makeStarTable(nc.getStream(), new VOTableBuilder())) {
            int col_id = -1, col_format = -1, col_size = -1;
            int num = table.getColumnCount();
            for (int i = 0; i < num; i++) {
                ColumnInfo col_info = table.getColumnInfo(i);
                String col_name = col_info.getName();
                Class<?> col_class = col_info.getContentClass();
                if (String.class == col_class && "Item Id".equals(col_name))
                    col_id = i;
                if (String.class == col_class && "File Format".equals(col_name))
                    col_format = i;
                if (Long.class == col_class && "File Size".equals(col_name))
                    col_size = i;
            }
            if (col_id == -1)
                throw new Exception("Column \"Item Id\" not found: " + uri);
            if (col_format == -1)
                throw new Exception("Column \"File Format\" not found: " + uri);
            if (col_size == -1)
                throw new Exception("Column \"File Size\" not found: " + uri);

            List<SoarClient.DataItem> items = new ArrayList<>();
            try (RowSequence rseq = table.getRowSequence()) {
                while (rseq.next()) {
                    Object[] row = rseq.getRow();
                    String format = (String) row[col_format];
                    try {
                        items.add(new SoarClient.DataItem(
                                (String) row[col_id],
                                SoarClient.SoarFileFormat.valueOf(format),
                                ((Number) row[col_size]).longValue()));
                    } catch (IllegalArgumentException e) {
                        Log.warn("Ignoring unknown SOAR data format: " + format, e);
                    }
                }
            }
            return items;
        }
    }

}
