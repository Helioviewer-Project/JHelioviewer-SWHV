package org.helioviewer.gl3d.spaceobjects;

import java.util.ArrayList;
import java.util.Date;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DFollowObjectCamera;
import org.helioviewer.gl3d.camera.GL3DPositionLoadingListener;
import org.helioviewer.gl3d.camera.GL3DSpaceObject;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DSphere;
import org.helioviewer.jhv.display.TimeListener;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class Planet extends GL3DSphere implements LayersListener, TimeListener, GL3DPositionLoadingListener {

    private final GL3DPositionLoadingPlanet positionLoading;
    private final GL3DPositionLoadingPlanet positionLoadingalt;

    private Date beginDate;
    private Date endDate;
    private boolean loaded;
    private GL3DVec3d position;
    private final GL3DSpaceObject spaceObject;
    private final GL3DSpaceObject viewPoint;

    public Planet(GL3DSpaceObject spaceObject, GL3DSpaceObject viewPoint) {
        super(0.9 * spaceObject.getSize() / Constants.SunRadiusInMeter, 10, 10, new GL3DVec4f(1.f, 0.f, 0.f, 1.f));
        this.spaceObject = spaceObject;
        this.viewPoint = viewPoint;
        positionLoading = new GL3DPositionLoadingPlanet();
        positionLoading.setObserver(viewPoint.getUrlName());
        positionLoading.setTarget("SUN");
        positionLoadingalt = new GL3DPositionLoadingPlanet();
        positionLoadingalt.setObserver(viewPoint.getUrlName());
        positionLoadingalt.setTarget(spaceObject.getUrlName());
        positionLoading.requestData();
        this.layerAdded(0);
        this.drawBits.set(Bit.Wireframe, true);
    }

    @Override
    public void layerAdded(int idx) {
        ArrayList<Date> requestDates = new ArrayList<Date>();

        View activeView = LayersModel.getSingletonInstance().getActiveView();
        JHVJPXView jpxView = activeView.getAdapter(JHVJPXView.class);
        if (jpxView != null) {
            for (int frame = 0; frame <= jpxView.getMaximumFrameNumber(); frame++) {
                requestDates.add(jpxView.getFrameDateTime(frame).getTime());
            }
        }
        if (requestDates.size() > 0) {
            if (this.beginDate == null || requestDates.get(0).getTime() < this.beginDate.getTime()) {
                this.beginDate = requestDates.get(0);
            }
            if (this.endDate == null || requestDates.get(0).getTime() > this.endDate.getTime()) {
                this.endDate = requestDates.get(requestDates.size() - 1);
            }
            //this.positionLoading.setBeginDate(beginDate);
            //this.positionLoading.setEndDate(endDate);
            //this.positionLoadingalt.setBeginDate(beginDate);
            //this.positionLoadingalt.setEndDate(endDate);
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
    public void timeChanged(Date date) {
        updatePosition(date);
    }

    private void updatePosition(Date date) {
        if (this.positionLoading.isLoaded() && this.positionLoadingalt.isLoaded()) {
            long time = date.getTime();
            GL3DVec3d position = this.positionLoading.getInterpolatedPosition(time);
            GL3DVec3d positionalt = this.positionLoadingalt.getInterpolatedPosition(time);

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
            GL3DFollowObjectCamera foc = (GL3DFollowObjectCamera) (GL3DState.get().getActiveCamera());
            double currentRotation = Astronomy.getL0Radians(new Date(foc.getTime()));

            this.m.rotate(-currentRotation, GL3DVec3d.YAxis);
            this.m.translate(position);

            this.wm = (this.m);
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

    @Override
    public String toString() {
        return spaceObject.toString() + " as seen from " + viewPoint.toString();
    }

}
