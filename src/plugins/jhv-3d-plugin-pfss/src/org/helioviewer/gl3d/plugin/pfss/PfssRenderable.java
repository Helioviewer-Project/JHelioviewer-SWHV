package org.helioviewer.gl3d.plugin.pfss;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.media.opengl.GL2;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.plugin.pfss.data.PfssCache;
import org.helioviewer.gl3d.plugin.pfss.data.PfssData;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;
import org.helioviewer.viewmodel.view.View;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssRenderable implements Renderable, LayersListener {

    private PfssCache pfssCache = null;
    private boolean isVisible = false;
    private final RenderableType type;
    private final PfssPluginPanel optionsPanel;

    /**
     * Default constructor.
     */
    public PfssRenderable(PfssCache pfssCache) {
        type = new RenderableType("PFSS plugin");
        this.pfssCache = pfssCache;
        Displayer.getRenderablecontainer().addRenderable(this);
        this.optionsPanel = new PfssPluginPanel();
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    @Override
    public void init(GL3DState state) {
    }

    @Override
    public void render(GL3DState state) {
        if (isVisible) {
            GL2 gl = GL3DState.get().gl;

            PfssData pfssData = pfssCache.getData(0);//state.getCurrentObservationDate().getTime());
            if (pfssData != null) {
                pfssData.setInit(false);
                pfssData.init(gl);
                if (pfssData.isInit()) {
                    pfssData.display(gl);
                    datetime = pfssData.getDateString();
                    Displayer.getRenderablecontainer().fireTimeUpdated(this);
                }
            }
        }
    }

    @Override
    public void remove(GL3DState state) {

    }

    @Override
    public RenderableType getType() {
        return type;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "PFSS data";
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;

    }

    String datetime = "";

    @Override
    public String getTimeString() {

        return datetime;

    }

    @Override
    public void layerAdded(int idx) {
        Date start = LayersModel.getSingletonInstance().getFirstDate();
        Date end = LayersModel.getSingletonInstance().getLastDate();
        if (start != null && end != null) {

            Calendar startCal = GregorianCalendar.getInstance();
            startCal.setTime(start);

            Calendar endCal = GregorianCalendar.getInstance();
            endCal.setTime(end);

            int startYear = startCal.get(Calendar.YEAR);
            int startMonth = startCal.get(Calendar.MONTH);

            int endYear = endCal.get(Calendar.YEAR);
            int endMonth = endCal.get(Calendar.MONTH);
            boolean run = true;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            while (run) {
                URL data;
                try {
                    String m = (startMonth) < 9 ? "0" + (startMonth + 1) : (startMonth + 1) + "";
                    data = new URL(PfssSettings.baseUrl + startYear + "/" + m + "/list.txt");
                    System.out.println(PfssSettings.baseUrl + startYear + "/" + m + "/list.txt");
                    BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));

                    String inputLine;
                    String[] splitted = null;
                    String url;
                    String[] date;
                    String[] time;
                    while ((inputLine = in.readLine()) != null) {
                        splitted = inputLine.split(" ");
                        url = splitted[1];

                        try {
                            Date dd = dateFormat.parse(url);
                            System.out.println(url);
                            pfssCache.preloadData(dd.getTime(), url);
                        } catch (ParseException e) {
                            Log.debug("Date could not be parsed from url " + url + "Exception was thrown : " + e);
                        }
                    }
                    in.close();

                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {

                }

                if (startYear == endYear && startMonth == endMonth)
                    run = false;
                else if (startYear == endYear && startMonth < endMonth) {
                    startMonth++;
                } else if (startYear < endYear) {
                    if (startMonth == 11) {
                        startMonth = 1;
                        startYear++;
                    }
                }

            }
        }
    }

    @Override
    public void layerRemoved(int oldIdx) {
    }

    @Override
    public void activeLayerChanged(View view) {
    }
}
