package org.helioviewer.jhv.io;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONException;
import org.json.JSONObject;

public class DataSources {

    private static final HashMap<String, HashMap<String, String>> serverSettings = new HashMap<String, HashMap<String, String>>() {
        {
            put("ROB",
                new HashMap<String, String>() {
                {
                    put("API.dataSources.path", "http://swhv.oma.be/hv/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                    put("API.jp2images.path", "http://swhv.oma.be/hv/api/index.php?action=getJP2Image&");
                    put("API.jp2series.path", "http://swhv.oma.be/hv/api/index.php?action=getJPX&");
                    put("default.remote.path", "jpip://swhv.oma.be:8090");
                    put("default.httpRemote.path", "http://swhv.oma.be/hv/jp2/");
                }
            });
            put("IAS",
                new HashMap<String, String>() {
                {
                    put("API.dataSources.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/?action=getDataSources&verbose=true&enable=[Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.jp2images.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php?action=getJP2Image&");
                    put("API.jp2series.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php?action=getJPX&");
                    put("default.remote.path", "jpip://helioviewer.ias.u-psud.fr:8080");
                    put("default.httpRemote.path", "http://helioviewer.ias.u-psud.fr/helioviewer/jp2/");
                }
            });
            put("GSFC",
                new HashMap<String, String>() {
                {
                    put("API.dataSources.path", "http://api.helioviewer.org/v2/getDataSources/?verbose=true&enable=[Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.jp2images.path", "http://api.helioviewer.org/v2/getJP2Image/?");
                    put("API.jp2series.path", "http://api.helioviewer.org/v2/getJPX/?");
                    put("default.remote.path", "jpip://helioviewer.org:8090");
                    put("default.httpRemote.path", "http://helioviewer.org/jp2/");
                }
            });
        }
    };

    private static final String[] serverList = new String[] { "ROB", "IAS", "GSFC" };

    public static String getServerSetting(String server, String setting) {
        Map<String, String> settings = serverSettings.get(server);
        if (settings != null)
            return settings.get(setting);
        else
            return null;
    }

    private static DataSources instance;
    private static DefaultComboBoxModel comboModel;

    public static final HashSet<String> SupportedObservatories = new HashSet<String>();

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

            String selectedServer;
            String datasourcesPath = Settings.getSingletonInstance().getProperty("API.dataSources.path");
            if (datasourcesPath.contains("ias.u-psud.fr")) {
                selectedServer = "IAS";
            } else if (datasourcesPath.contains("helioviewer.org")) {
                selectedServer = "GSFC";
            } else {
                selectedServer = "ROB";
            }
            changeServer(selectedServer);

            comboModel = new DefaultComboBoxModel(serverList);
            comboModel.setSelectedItem(selectedServer);
        }
        return instance;
    }

    private static void changeServer(final String server) {
        Map<String, String> map = serverSettings.get(server);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Settings.getSingletonInstance().setProperty(entry.getKey(), entry.getValue());
        }

        JHVWorker<Void, Void> reloadTask = new JHVWorker<Void, Void>() {

            private final DataSourcesParser parser = new DataSourcesParser(server);

            @Override
            protected Void backgroundWork() {
                while (true) {
                    try {
                        URL url = new URL(Settings.getSingletonInstance().getProperty("API.dataSources.path"));
                        JSONObject json = JSONUtils.getJSONStream(new DownloadStream(url).getInput());

                        parser.parse(json);
                        return null;
                    } catch (MalformedURLException e) {
                        Log.error("Invalid data sources URL", e);
                        break;
                    } catch (JSONException e) {
                        Log.error("Invalid response while retrieving the available data sources", e);
                        break;
                    } catch (ParseException e) {
                        Log.error("Invalid response while retrieving the available data sources", e);
                        break;
                    } catch (IOException e) {
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
                ObservationDialog.getInstance().getObservationImagePane().setupSources(parser);
            }

        };
        reloadTask.setThreadName("MAIN--ReloadServer");
        JHVGlobals.getExecutorService().execute(reloadTask);
    }

    private static final ActionListener serverChange = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String server = (String) comboModel.getSelectedItem();
                changeServer(server);
                ObservationDialog.getInstance().setAvailabilityStatus(server);
            }
        };

    private static boolean firstCombo = true;

    public static JComboBox getServerComboBox() {
        JComboBox combo = new JComboBox(comboModel);
        if (firstCombo) {
            firstCombo = false;
            combo.addActionListener(serverChange);
        }
        return combo;
    }

    public static String getSelectedServer() {
        return (String) comboModel.getSelectedItem();
    }

}
