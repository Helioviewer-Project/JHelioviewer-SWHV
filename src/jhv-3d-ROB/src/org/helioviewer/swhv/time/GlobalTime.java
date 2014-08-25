package org.helioviewer.swhv.time;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Timer;

public class GlobalTime {

    long beginTime = 1404641735000l;
    long endTime = 1404717083000l;
    long currentTime = 1404641735000l;
    long cadence = 30 * 60 * 60 * 1000;
    int targetFPS;

    ArrayList<GlobalTimeListener> globalTimeListeners = new ArrayList<GlobalTimeListener>();

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public void setTargetFPS(int targetFPS) {
        this.targetFPS = targetFPS;
    }

    public void setTimeStep(long cadence) {
        this.cadence = cadence;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getCurrentTime() {
        return endTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public void setTimeInterval(long beginTime, long endTime) {
        this.setBeginTime(beginTime);
        this.setEndTime(endTime);
    }

    public void addTimeListener(GlobalTimeListener listener) {
        this.globalTimeListeners.add(listener);
    }

    public void fireBeginTimeChanged() {
        for (GlobalTimeListener timeListener : this.globalTimeListeners) {
            timeListener.beginTimeChanged(this.beginTime);
        }
    }

    public void fireEndTimeChanged() {
        for (GlobalTimeListener timeListener : this.globalTimeListeners) {
            timeListener.endTimeChanged(this.endTime);
        }
    }

    public void fireCurrentTimeChanged() {
        for (GlobalTimeListener timeListener : this.globalTimeListeners) {
            timeListener.currentTimeChanged(this.currentTime);
        }
    }

    public void runTime() {
        ActionListener taskPerformer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                currentTime = currentTime + (endTime - beginTime) / 100;
                if (currentTime > endTime) {
                    currentTime = beginTime;
                }
                fireCurrentTimeChanged();
            }
        };
        new Timer(100, taskPerformer).start();
    }

}
