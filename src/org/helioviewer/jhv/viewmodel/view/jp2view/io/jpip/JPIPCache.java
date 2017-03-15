package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import java.io.IOException;

public interface JPIPCache {

    void addJPIPDataSegment(JPIPDataSegment data) throws IOException;

}
