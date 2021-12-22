package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;

public class SoarClient {

    private static final String SEARCH_URL = "http://soar.esac.esa.int/soar-sl-tap/tap";
    private static final String LOAD_URL = "http://soar.esac.esa.int/soar-sl-tap/data?retrieval_type=LAST_PRODUCT&product_type=SCIENCE&data_item_id=";

    private enum FileFormat {CDF, FITS, JP2}

    public record DataItem(String id, FileFormat format, long size) {
        @Override
        public String toString() {
            return id;
        }
    }

    public static void submitSearch(@Nonnull TAPClient.Receiver receiver, @Nonnull List<String> descriptors, @Nonnull String level, long start, long end) {
        TAPClient.submitQuery(receiver, SEARCH_URL, buildADQL(descriptors, level, start, end), SoarClient::data2DataItems);
    }

    public static void submitLoad(@Nonnull List<DataItem> items) {
        List<URI> fitsUris = new ArrayList<>();
        List<URI> jp2Uris = new ArrayList<>();
        List<URI> cdfUris = new ArrayList<>();

        for (DataItem item : items) {
            try {
                URI uri = new URI(LOAD_URL + item.id);
                switch (item.format) {
                    case CDF -> cdfUris.add(uri);
                    case FITS -> fitsUris.add(uri);
                    case JP2 -> jp2Uris.add(uri);
                }
            } catch (Exception e) {
                Log.warn(e);
            }
        }
        Load.CDF.getAll(cdfUris);
        Load.FITS.getAll(fitsUris);
        Load.Image.getAll(jp2Uris);
    }

    private static String buildADQL(@Nonnull List<String> descriptors, @Nonnull String level, long start, long end) {
        String desc = String.join("' OR descriptor='", descriptors);
        return "SELECT data_item_id,file_format,filesize FROM v_sc_data_item WHERE " +
                "(descriptor='" + desc + "') AND " +
                "begin_time >= '" + TimeUtils.format(start) + "' AND end_time <= '" + TimeUtils.format(end) + "' AND " +
                "level='" + level + "' ORDER BY begin_time";
    }

    private static List<DataItem> data2DataItems(JSONArray data) {
        int length = data.length();
        List<DataItem> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            JSONArray item = data.getJSONArray(i);
            try {
                result.add(new DataItem(item.getString(0), FileFormat.valueOf(item.getString(1)), item.getLong(2)));
            } catch (Exception ignore) { // ignore unknown formats
            }
        }
        return result;
    }

}
