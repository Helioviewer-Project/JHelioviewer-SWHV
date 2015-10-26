package org.helioviewer.jhv.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.AlphanumComparator;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.logging.Log;
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
 * <p>
 * For further improvements in the result also the URL for the data query could
 * be encoded to distribute the load to different server this way.
 *
 * @author Helge Dietert
 */
public class DataSources {

    public static final HashSet<String> SupportedObservatories = new HashSet<String>();

    /**
     * Item to select. Has a nice toString() so that a list can be put into a
     * JComboBox
     * <p>
     * Also has some advanced sorting and overloaded equal.
     *
     * @author Helge Dietert
     */
    public class Item implements Comparable<Item> {
        /**
         * Flag if this should take as default item
         */
        private final boolean defaultItem;
        /**
         * Tooltip description
         */
        private final String description;
        /**
         * Key as needed to send to the API for this item
         */
        private final String key;
        /**
         * Display name for a dropdown list
         */
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

        /**
         * Compare it to some other item with the advanced key
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(Item other) {
            return keyComparator.compare(key, other.key);
        }

        /**
         * Sorting and equal from sortKey,key
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */

        @Override
        public boolean equals(Object obj) {
            try {
                Item other = (Item) obj;
                return key == other.key;
            } catch (ClassCastException e) {
                return false;
            } catch (NullPointerException e) {
                return false;
            }
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * True if it was created as default item
         *
         * @return the defaultItem
         */
        public boolean isDefaultItem() {
            return defaultItem;
        }

        /**
         * Shows a nice string (name)
         */

        @Override
        public String toString() {
            return name;
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
        }
        return instance;
    }

    /**
     * Result with the available data sources
     */
    private JSONObject jsonResult;

    /**
     * Used comparator to sort the items after the key
     */
    private final Comparator<String> keyComparator = new AlphanumComparator();

    private void reload() {
        jsonResult = null;

        while (true) {
            try {
                String queryString = Settings.getSingletonInstance().getProperty("API.dataSources.path");
                URL query = new URL(queryString);
                DownloadStream ds = new DownloadStream(query, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());
                Reader reader = new BufferedReader(new InputStreamReader(ds.getInput(), "UTF-8"));
                jsonResult = new JSONObject(new JSONTokener(reader));
                break;
            } catch (MalformedURLException e) {
                // Should not occur
                Log.error("Invalid url to retrieve data source", e);
                break;
            } catch (JSONException e) {
                // Should not occur
                Log.error("While retrieving the available data sources got invalid response", e);
                break;
            } catch (IOException e) {
                // Log.error("Error while reading the available data sources",
                // e);
                // Message.err("Cannot Download Source Information",
                // "When trying to read the available data sources from the internet got:\n"
                // + e.getMessage(), false);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    // Should not occur
                    Log.error(e1);
                }
            }
        }
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
        return null;
    }

    /**
     * Gives the JSON Object for an detector
     *
     * @param observatory
     *            Key of the observatory
     * @param instrument
     *            Key of the instrument
     * @param detector
     *            Key of the detector
     * @return JSON object for the given observatory
     * @throws JSONException
     */
    private JSONObject getDetector(String observatory, String instrument, String detector) throws JSONException {
        return getInstrument(observatory, instrument).getJSONObject("children").getJSONObject(detector);
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
            return getChildrenList(getInstrument(observatory, instrument).getJSONObject("children"));
        } catch (JSONException e) {
            Log.error("Cannot find instruments for " + observatory, e);
            return null;
        }
    }

    /**
     * Gives the JSON Object for an instrument
     *
     * @param observatory
     *            Key of the observatory
     * @param instrument
     *            Key of the instrument
     * @return JSON object for the given observatory
     * @throws JSONException
     */
    private JSONObject getInstrument(String observatory, String instrument) throws JSONException {
        return getObservatory(observatory).getJSONObject("children").getJSONObject(instrument);
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
            return getChildrenList(getObservatory(observatory).getJSONObject("children"));
        } catch (JSONException e) {
            Log.error("Cannot find instruments for " + observatory, e);
            return null;
        }
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
            return getChildrenList(getDetector(observatory, instrument, detector).getJSONObject("children"));
        } catch (JSONException e) {
            Log.error("Cannot find instruments for " + observatory, e);
            return null;
        }
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

    private JSONObject getObservatory(String observatory) throws JSONException {
        return jsonResult.getJSONObject(observatory);
    }

    private String selectedServer = "";
    private final String[] serverList = new String[] { "ROB", "GSFC", "IAS" };

    public void changeServer(String server, final boolean donotloadStartup) {
        selectedServer = server;
        if (server.contains("ROB")) {
            Settings.getSingletonInstance().setProperty("API.dataSources.path", "http://swhv.oma.be/hv/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
            Settings.getSingletonInstance().setProperty("API.jp2images.path", "http://swhv.oma.be/hv/api/index.php");
            Settings.getSingletonInstance().setProperty("API.jp2series.path", "http://swhv.oma.be/hv/api/index.php");
            Settings.getSingletonInstance().setProperty("default.remote.path", "jpip://swhv.oma.be:8090");
            Settings.getSingletonInstance().setProperty("API.event.path", "http://swhv.oma.be/hv/api/");
            Settings.getSingletonInstance().setProperty("default.httpRemote.path", "http://swhv.oma.be/hv/jp2/");
        } else if (server.contains("GSFC")) {
            Settings.getSingletonInstance().setProperty("API.dataSources.path", "http://helioviewer.org/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
            Settings.getSingletonInstance().setProperty("API.jp2images.path", "http://helioviewer.org/api/index.php");
            Settings.getSingletonInstance().setProperty("API.jp2series.path", "http://helioviewer.org/api/index.php");
            Settings.getSingletonInstance().setProperty("default.remote.path", "jpip://helioviewer.org:8090");
            Settings.getSingletonInstance().setProperty("API.event.path", "http://helioviewer.org/api/");
            Settings.getSingletonInstance().setProperty("default.httpRemote.path", "http://helioviewer.org/jp2/");
        } else if (server.contains("IAS")) {
            Settings.getSingletonInstance().setProperty("API.dataSources.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
            Settings.getSingletonInstance().setProperty("API.jp2images.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php");
            Settings.getSingletonInstance().setProperty("API.jp2series.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php");
            Settings.getSingletonInstance().setProperty("default.remote.path", "jpip://helioviewer.ias.u-psud.fr:8080");
            Settings.getSingletonInstance().setProperty("API.event.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/");
            Settings.getSingletonInstance().setProperty("default.httpRemote.path", "http://helioviewer.ias.u-psud.fr/helioviewer/jp2/");
        }

        JHVWorker<Void, Void> reloadSources = new JHVWorker<Void, Void>() {

            @Override
            protected Void backgroundWork() {
                reload();
                return null;
            }

            @Override
            protected void done() {
                for (DataSourcesListener l : listeners) {
                    l.serverChanged(donotloadStartup);
                }
            }

        };
        reloadSources.setThreadName("ReloadServer");
        reloadSources.execute();
    }

    public String[] getServerList() {
        return serverList;
    }

    public String getSelectedServer() {
        return selectedServer;
    }

    private static final HashSet<DataSourcesListener> listeners = new HashSet<DataSourcesListener>();

    public void addListener(DataSourcesListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DataSourcesListener listener) {
        listeners.remove(listener);
    }

}
