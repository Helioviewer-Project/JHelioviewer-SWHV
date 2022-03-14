package org.helioviewer.jhv.io;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.votable.VOTableBuilder;

class SoarTable {

    static {
        Logger.getLogger("uk.ac.starlink").setLevel(Level.WARNING); // shut-up info logs
    }

    public static List<SoarClient.DataItem> get(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri);
             InputStream is = nc.getStream();
             StarTable table = new StarTableFactory().makeStarTable(is, new VOTableBuilder())) {
            int col_id = -1, col_format = -1, col_size = -1;
            int num = table.getColumnCount();
            for (int i = 0; i < num; i++) {
                ColumnInfo col_info = table.getColumnInfo(i);
                String col_name = col_info.getName();
                Class<?> col_class = col_info.getContentClass();
                if ("Item Id".equals(col_name) && String.class == col_class)
                    col_id = i;
                if ("File Format".equals(col_name) && String.class == col_class)
                    col_format = i;
                if ("File Size".equals(col_name) && Long.class == col_class)
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
                    items.add(new SoarClient.DataItem((String) rseq.getCell(col_id), SoarClient.FileFormat.valueOf((String) rseq.getCell(col_format)), (Long) rseq.getCell(col_size)));
                }
            }
            return items;
        }
    }

}
