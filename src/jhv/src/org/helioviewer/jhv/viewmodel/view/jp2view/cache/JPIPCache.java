package org.helioviewer.jhv.viewmodel.view.jp2view.cache;

import java.io.IOException;

import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDataSegment;

public interface JPIPCache {

    void addJPIPDataSegment(JPIPDataSegment data) throws IOException;

}
