package org.helioviewer.swhv.time;

public interface GlobalTimeListener {

    void beginTimeChanged(long beginTime);

    void endTimeChanged(long endTime);

    void currentTimeChanged(long currentTime);

}
