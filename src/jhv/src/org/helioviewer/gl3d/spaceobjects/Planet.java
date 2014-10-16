package org.helioviewer.gl3d.spaceobjects;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DPositionLoadingListener;
import org.helioviewer.gl3d.camera.GL3DPositionLoadingPlanet;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DSphere;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;

public class Planet extends GL3DSphere implements LayersListener, ViewListener, GL3DPositionLoadingListener {
    private final GL3DPositionLoadingPlanet positionLoading;
    private final GL3DPositionLoadingPlanet positionLoadingalt;

    private Date beginDate;
    private Date endDate;
    private Date currentDate;
    private final GL3DSceneGraphView sceneGraphView;
    private boolean loaded;
    private GL3DVec3d position;

    public Planet(GL3DSceneGraphView sceneGraphView) {
        //super(6052000 / Constants.SunRadiusInMeter, 10, 10, new GL3DVec4f(1.f, 0.f, 0.f, 1.f));
        super(2439700 / Constants.SunRadiusInMeter, 10, 10, new GL3DVec4f(1.f, 0.f, 0.f, 1.f));
        //super(173710000 / Constants.SunRadiusInMeter, 10, 10, new GL3DVec4f(1.f, 0.f, 0.f, 1.f));
        this.sceneGraphView = sceneGraphView;
        positionLoading = new GL3DPositionLoadingPlanet();
        //positionLoading.setObserver("PROBA2");
        positionLoading.setObserver("STEREO%20Ahead");
        //positionLoading.setObserver("SDO");
        positionLoading.setTarget("SUN");
        positionLoadingalt = new GL3DPositionLoadingPlanet();
        //positionLoadingalt.setObserver("PROBA2");
        positionLoadingalt.setObserver("STEREO%20Ahead");
        //positionLoadingalt.setObserver("SDO");
        //positionLoadingalt.setTarget("Venus");
        positionLoadingalt.setTarget("Mercury");
        //positionLoading.setTarget("Moon");

        positionLoading.requestData();
        this.sceneGraphView.addViewListener(this);
        LayersModel.getSingletonInstance().addLayersListener(this);

    }

    private Date parseDate(String dateOBS) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        try {
            return sdf.parse(dateOBS);
        } catch (ParseException e) {
            Log.warn("Could not parse date:" + dateOBS + ". Returned null.");
            return null;
        }
    }

    @Override
    public void layerAdded(int idx) {
        try {
            List<Date> requestDates = new ArrayList<Date>();

            View activeView = LayersModel.getSingletonInstance().getActiveView();
            JHVJPXView jpxView = activeView.getAdapter(JHVJPXView.class);
            if (jpxView != null) {
                for (int frame = 1; frame <= jpxView.getMaximumFrameNumber(); frame++) {
                    String dateOBS = jpxView.getJP2Image().getValueFromXML("DATE-OBS", "fits", frame);
                    if (dateOBS == null) {
                        dateOBS = jpxView.getJP2Image().getValueFromXML("DATE_OBS", "fits", frame);
                    }
                    if (dateOBS != null) {
                        Date parsedDate = parseDate(dateOBS);
                        if (parsedDate != null) {
                            requestDates.add(parsedDate);
                        }
                    } else {
                        Log.error("Destroy myself with handgrenade. No date-obs in whatever dialect could be found");
                    }
                }
            }
            if (requestDates.size() > 0) {
                if (this.beginDate == null || requestDates.get(0).getTime() < this.beginDate.getTime()) {
                    this.beginDate = requestDates.get(0);
                }
                if (this.endDate == null || requestDates.get(0).getTime() > this.endDate.getTime()) {
                    this.endDate = requestDates.get(requestDates.size() - 1);
                }
                this.positionLoading.setBeginDate(beginDate);
                this.positionLoading.setEndDate(endDate);
                this.positionLoadingalt.setBeginDate(beginDate);
                this.positionLoadingalt.setEndDate(endDate);
            }
        } catch (JHV_KduException ex) {
            Log.error("Received an kakadu exception. " + ex);
        }
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
    }

    @Override
    public void layerChanged(int idx) {
    }

    @Override
    public void activeLayerChanged(int idx) {
    }

    @Override
    public void viewportGeometryChanged() {
    }

    @Override
    public void timestampChanged(int idx) {
    }

    @Override
    public void subImageDataChanged() {
    }

    @Override
    public void layerDownloaded(int idx) {
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
        if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
            currentDate = timestampReason.getNewDateTime().getTime();
            updatePosition();
        }
    }

    private void updatePosition() {
        if (this.positionLoading.isLoaded() && this.positionLoadingalt.isLoaded()) {
            GL3DVec3d position = this.positionLoading.getInterpolatedPosition(currentDate.getTime());
            GL3DVec3d positionalt = this.positionLoadingalt.getInterpolatedPosition(currentDate.getTime());

            this.position = new GL3DVec3d(position.x - positionalt.x, position.y - positionalt.y, position.z - positionalt.z);
        }
    }

    @Override
    public void shapeDraw(GL3DState state) {
        this.markAsChanged();
        super.shapeDraw(state);
    }

    @Override
    public void update(GL3DState state) {
        if (!this.isInitialised) {
            this.init(state);
        }
        if (position != null) {
            state.pushMV();

            this.m.setIdentity();
            double currentRotation = Astronomy.getL0Radians(currentDate);

            this.m.rotate(-currentRotation, new GL3DVec3d(0, 1, 0));
            this.m.translate(position);

            this.wm = (this.m);
            state.buildInverseAndNormalMatrix();
            this.wmI = new GL3DMat4d(state.getMVInverse());
            //this.shapeUpdate(state);
            state.popMV();
        }
    }

    @Override
    public void fireNewLoaded(String string) {
        this.loaded = true;
    }

    @Override
    public void fireNewDate() {
    }
}
