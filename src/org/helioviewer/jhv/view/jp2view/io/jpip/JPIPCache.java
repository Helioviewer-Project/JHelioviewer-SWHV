package org.helioviewer.jhv.view.jp2view.io.jpip;

import java.io.IOException;

public interface JPIPCache {

    void addJPIPDataSegment(JPIPDataSegment data) throws IOException;

}
