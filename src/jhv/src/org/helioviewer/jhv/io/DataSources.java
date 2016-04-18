package org.helioviewer.jhv.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.gui.dialogs.observation.ImageDataPanel;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Data objects storing the information of available data as a central singleton
 * object.
 * <p>
 * On the first startup it will query
 * <code>http://helioviewer.org/api/?action=getDataSources</code> (or whatever
 * is set in the setting to "API.dataSources.path") and offer then convenient
 * methods to obtain the choices in drop down list as
 * <ul>
 * <li>Observatory (SDO, SOHO)
 * <li>Instrument (EIT,..)
 * <li>Detector
 * <li>Measurement (171..)
 * </ul>
 */
public class DataSources {

    public static final Map<String, String> ROBsettings;
    private static final Map<String, String> IASsettings;
    private static final Map<String, String> GSFCsettings;

    static {
        ROBsettings = new HashMap<String, String>();
        IASsettings = new HashMap<String, String>();
        GSFCsettings = new HashMap<String, String>();

        String[][] ROBpairs = {
            { "API.dataSources.path", "http://swhv.oma.be/hv/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]" },
            { "API.jp2images.path", "http://swhv.oma.be/hv/api/index.php" },
            { "API.jp2series.path", "http://swhv.oma.be/hv/api/index.php" },
            { "default.remote.path", "jpip://swhv.oma.be:8090" },
            { "API.event.path", "http://swhv.oma.be/hv/api/" },
            { "default.httpRemote.path", "http://swhv.oma.be/hv/jp2/" }
        };

        String[][] IASpairs = {
                { "API.dataSources.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/?action=getDataSources&verbose=true&enable=[TRACE,Yohkoh,STEREO_A,STEREO_B,PROBA2]" },
                { "API.jp2images.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php" },
                { "API.jp2series.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php" },
                { "default.remote.path", "jpip://helioviewer.ias.u-psud.fr:8080" },
                { "API.event.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/" },
                { "default.httpRemote.path", "http://helioviewer.ias.u-psud.fr/helioviewer/jp2/" }
        };

        String[][] GSFCpairs = {
                { "API.dataSources.path", "http://helioviewer.org/api/?action=getDataSources&verbose=true&enable=[TRACE,Yohkoh,STEREO_A,STEREO_B,PROBA2]" },
                { "API.jp2images.path", "http://helioviewer.org/api/index.php" },
                { "API.jp2series.path", "http://helioviewer.org/api/index.php" },
                { "default.remote.path", "jpip://helioviewer.org:8090" },
                { "API.event.path", "http://helioviewer.org/api/" },
                { "default.httpRemote.path", "http://helioviewer.org/jp2/" }
        };

        for (String[] pair : ROBpairs) {
            ROBsettings.put(pair[0], pair[1]);
        }
        for (String[] pair : IASpairs) {
            IASsettings.put(pair[0], pair[1]);
        }
        for (String[] pair : GSFCpairs) {
            GSFCsettings.put(pair[0], pair[1]);
        }
    }

    public static final HashSet<String> SupportedObservatories = new HashSet<String>();

    public static class Item implements Comparable<Item> {

        // Flag if this should take as default item
        private final boolean defaultItem;

        // Tooltip description
        private final String description;

        // Key as needed to send to the API for this item
        private final String key;

        // Display name for a dropdown list
        private final String name;

        /**
         * Creates a new item
         *
         * @param key
         *            Key to reference the API
         * @param defaultItem
         *            Flag to indicate usage as default
         * @param name
         *            Nice name to show, will be shown as toString()
         * @param description
         *            Longer description
         */
        public Item(String key, boolean defaultItem, String name, String description) {
            this.key = key;
            this.defaultItem = defaultItem;
            this.name = name;
            this.description = description;
        }

        @Override
        public int compareTo(Item other) {
            return JHVGlobals.alphanumComparator.compare(key, other.key);
        }

       @Override
        public boolean equals(Object o) {
            if (o instanceof Item)
                return key == ((Item) o).key;
            return false;
        }

        public String getDescription() {
            return description;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public boolean isDefaultItem() {
            return defaultItem;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            assert false : "hashCode not designed";
            return 42;
        }

    }

    private static DataSources instance;

    private DataSources() {}

    public static DataSources getSingletonInstance() {
        if (instance == null) {
            instance = new DataSources();

            String prop = Settings.getSingletonInstance().getProperty("supported.data.sources");
            if (prop != null && SupportedObservatories.isEmpty()) {
                String supportedObservatories[] = prop.split(" ");
                for (String s : supportedObservatories) {
                    if (!s.isEmpty()) {
                        SupportedObservatories.add(s);
                    }
                }
            }

            String datasourcesPath = Settings.getSingletonInstance().getProperty("API.dataSources.path");
            if (datasourcesPath.contains("ias.u-psud.fr")) {
                selectedServer = "IAS";
            } else if (datasourcesPath.contains("helioviewer.org")) {
                selectedServer = "GSFC";
            } else {
                selectedServer = "ROB";
            }
            changeServer(selectedServer);
        }
        return instance;
    }

    /**
     * For the given root this will create a sorted list or items
     *
     * @param root
     *            Element for which the children will be created
     * @return List of items to select or null if some error occurs
     */
    private Item[] getChildrenList(JSONObject root) {
        try {
            SortedSet<Item> children = new TreeSet<Item>();
            Iterator<?> iter = root.keys();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                JSONObject child = root.getJSONObject(key);
                Item newItem = new Item(key, child.optBoolean("default", false),
                                        child.getString("name").replace((char) 8287, ' '), // e.g. 304\u205f\u212b
                                        child.getString("description"));
                children.add(newItem);
            }
            return children.toArray(new Item[children.size()]);
        } catch (JSONException e) {
            Log.error("Error finding children of " + root, e);
        }
        return new Item[0];
    }

    private static JSONObject jsonResult;

    private JSONObject getJSONItemChildren(String... spec) throws JSONException {
        JSONObject o = jsonResult.getJSONObject(spec[0]).getJSONObject("children");
        for (int i = 1; i < spec.length; i++)
            o = o.getJSONObject(spec[i]).getJSONObject("children");
        return o;
    }

    /**
     * Resolves the detectors for a observatory and instrument
     *
     * @param observatory
     *            Name of observatory to query
     * @param instrument
     *            Name of instrument to query
     * @return List of available measurements, null if not valid
     */
    public Item[] getDetectors(String observatory, String instrument) {
        try {
            return getChildrenList(getJSONItemChildren(observatory, instrument));
        } catch (JSONException e) {
            Log.error("Cannot find detectors for " + observatory, e);
        }
        return new Item[0];
    }

    /**
     * Resolves the instruments for a observatory
     *
     * @param observatory
     *            Name of observatory for which the instruments are returned
     * @return List of available instruments, null if this observatory is not
     *         supported
     */
    public Item[] getInstruments(String observatory) {
        try {
            return getChildrenList(getJSONItemChildren(observatory));
        } catch (JSONException e) {
            Log.error("Cannot find instruments for " + observatory, e);
        }
        return new Item[0];
    }

    /**
     * Resolves the detectors for a observatory and instrument
     *
     * @param observatory
     *            Name of observatory to query
     * @param instrument
     *            Name of instrument to query
     * @param detector
     *            Name of detector to query
     * @return List of available measurements, null if not valid
     */
    public Item[] getMeasurements(String observatory, String instrument, String detector) {
        try {
            return getChildrenList(getJSONItemChildren(observatory, instrument, detector));
        } catch (JSONException e) {
            Log.error("Cannot find measurements for " + observatory, e);
        }
        return new Item[0];
    }

    /**
     * Resolve the available observatories
     *
     * @return List of available observatories
     */
    public Item[] getObservatories() {
        ArrayList<Item> result = new ArrayList<Item>();
        for (Item item : getChildrenList(jsonResult)) {
            if (SupportedObservatories.contains(item.getName())) {
                result.add(item);
            }
        }
        return result.toArray(new Item[result.size()]);
    }

    private static String selectedServer = "";
    private static final String[] serverList = new String[] { "ROB", "IAS", "GSFC" };

    public static void changeServer(String server) {
        selectedServer = server;

        Map<String, String> map;
        if (server.contains("ROB")) {
            map = ROBsettings;
        } else if (server.contains("IAS")) {
            map = IASsettings;
        } else /* if (server.contains("GSFC")) */ {
            map = GSFCsettings;
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            Settings.getSingletonInstance().setProperty(entry.getKey(), entry.getValue());
        }

        JHVWorker<Void, Void> reloadTask = new JHVWorker<Void, Void>() {

            private JSONObject newJsonResult = null;

            @Override
            protected Void backgroundWork() {
                while (true) {
                    try {
                        String queryString = Settings.getSingletonInstance().getProperty("API.dataSources.path");
                        URL query = new URL(queryString);
                        DownloadStream ds = new DownloadStream(query, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());
                        Reader reader = new BufferedReader(new InputStreamReader(ds.getInput(), "UTF-8"));
                        newJsonResult = new JSONObject(new JSONTokener(reader));
                        break;
                    } catch (MalformedURLException e) {
                        Log.error("Invalid url to retrieve data source", e);
                        break;
                    } catch (JSONException e) {
                        Log.error("While retrieving the available data sources got invalid response", e);
                        break;
                    } catch (IOException e) {
                        // Log.error("Error while reading the available data sources", e);
                        // Message.err("Cannot Download Source Information", "When trying to read the available data sources from the internet got:" + System.getProperty("line.separator") + e.getMessage(), false);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            Log.error(e1);
                            break;
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                if (newJsonResult == null)
                    return;

                jsonResult = newJsonResult;

                ImageDataPanel idp = ObservationDialog.getInstance().getObservationImagePane();

                idp.setupSources(DataSources.getSingletonInstance());
                if (idp.validSelection()) {
                    if (first) {
                        first = false;
                        SetupTimeTask setupTimeTask = new SetupTimeTask(idp.getObservatory(), idp.getInstrument(), idp.getDetector(), idp.getMeasurement());
                        JHVGlobals.getExecutorService().execute(setupTimeTask);
                    }
                } else {
                    Message.err("Could not retrieve data sources", "The list of available data could not be fetched, so you cannot use the GUI to add data." +
                                System.getProperty("line.separator") +
                                "This may happen if you do not have an internet connection or there are server problems. You can still open local files.", false);
                }
            }

        };
        reloadTask.setThreadName("MAIN--ReloadServer");
        JHVGlobals.getExecutorService().execute(reloadTask);
    }

    private static boolean first = true;

    public static String[] getServerList() {
        return serverList;
    }

    public static String getSelectedServer() {
        return selectedServer;
    }

}
